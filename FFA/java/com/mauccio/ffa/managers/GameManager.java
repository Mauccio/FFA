package com.mauccio.ffa.managers;

import com.mauccio.ffa.Core;
import com.mauccio.ffa.models.Arena;
import com.mauccio.ffa.models.Game;
import com.mauccio.ffa.models.GameSign;
import com.mauccio.ffa.util.Fireworks;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.util.*;

public class GameManager {
    private final Map<Arena, Game> games;
    private final Core core;
    private final TreeMap<String, Map.Entry<Long, String>> lastDamager;

    public GameManager(Core core) {
        this.core = core;
        this.games = new TreeMap<>((a1, a2) -> a1.getName().compareToIgnoreCase(a2.getName()));
        this.lastDamager = new TreeMap<>();
    }



    public void registerGame(Arena arena, Game game) {
        games.put(arena, game);
        game.startTicking();
        GameSign sign = core.getSignManager().getSignByArena(arena);
        if (sign != null) {
            updateGameSign(game);
        }
    }

    public Game getGame(Arena arena) {
        return games.get(arena);
    }

    public boolean hasGame(Arena arena) {
        return games.containsKey(arena);
    }

    public void removeGame(Arena arena) {
        Game game = games.remove(arena);
        if (game != null) {
            game.stopTicking();
        }
    }

    public Map<Arena, Game> getActiveGames() {
        return games;
    }

    public void tickAllGames() {
        for (Game game : games.values()) {
            switch (game.getState()) {
                case WAITING:
                    if (game.getPlayers().size() >= game.getArena().getMinPlayers()) {
                        game.start();
                    }
                    break;
                case STARTING:
                    game.tickCountdown();
                    break;
                case PLAYING:
                    game.tickGameTime();
                    break;

                case FINISHING:
                    game.tickFinishing();
                    Fireworks.launch(core, game.getTop1().getLocation(), Color.YELLOW, Color.ORANGE, Color.AQUA, FireworkEffect.Type.BALL_LARGE);
                    break;

                case RESTARTING:
                    resetPlayersToLobby(game);
                    game.restart();
                    game.setState(Game.GameState.WAITING);
                    break;
            }
            updateGameSign(game);
            updateScoreboard(game);
        }
    }

    public Game getGameFromPlayer(Player player) {
        for (Game game : games.values()) {
            if (game.getPlayers().contains(player)) {
                return game;
            }
        }
        return null;
    }

    public List<Player> getGamePlayers(Game game) {
        return game.getPlayers();
    }

    public boolean isInGame(Player player) {
        return getGameFromPlayer(player) != null;
    }

    public Player getGlobalTop1() {
        Player best = null;
        int maxKills = -1;

        for (Game game : games.values()) {
            Player top = game.getTop1();
            if (top != null && game.getKills(top) > maxKills) {
                best = top;
                maxKills = game.getKills(top);
            }
        }
        return best;
    }

    public Player getTop1(Game game) {
        return game.getTop1();
    }

    public void removePlayer(Player player) {
        Game game = getGameFromPlayer(player);
        if (game == null) return;
        removePlayerFromGame(game, player);
    }

    public void shutdownAllGames() {
        for (Game game : games.values()) {
            try {
                game.stopTicking();
                for (Player p : new ArrayList<>(game.getPlayers())) {
                    removePlayerFromGame(game, p);
                }
                game.finish();
                game.restart();
            } catch (Exception ex) {
                core.getLogger().warning("Error al apagar juego de arena "
                        + game.getArena().getName() + ": " + ex.getMessage());
            }
        }
        games.clear();
    }

    public void moveToRandomSpawn(Game game, Player player) {
        player.teleport(game.getArena().getRandomSpawn());
    }

    public void addPlayerToGame(Game game, Player player) {

        if (game.getPlayers().contains(player)) return;
        core.getLobbyManager().invSaver(player, player.getUniqueId());
        Location dest = game.getArena().getSpectatorSpawn();
        if (dest == null) {
            dest = core.getLobbyManager().getLobbyLocation();
            core.getLangManager().sendMessage("arena.spawn-not-found", player);
        }
        game.getPlayers().add(player);
        player.setScoreboard(game.getBoard());
        player.getInventory().clear();
        updateScoreboard(game);
        player.teleport(dest);
        if(game.getState() == Game.GameState.PLAYING) {
            giveJoinItemInGame(player);
        }

    }

    public void giveJoinItemInGame(Player player) {
        Inventory inv = player.getInventory();
        ItemStack is = new ItemStack(Material.valueOf(core.getConfigManager().getString("items.join-game.item")));
        is.setAmount(core.getConfig().getInt("items.join-game.amount"));
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(core.getConfigManager().getString("items.join-game.name"));
        im.setLore(core.getConfigManager().getStringList("items.join-game.lore"));
        is.setItemMeta(im);
        if(core.getConfig().getBoolean("items.join-game.glide")) {
            is.addEnchantment(org.bukkit.enchantments.Enchantment.LUCK, 1);
        }
        inv.addItem(is);
    }

    public void removePlayerFromGame(Game game, Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        player.teleport(core.getLobbyManager().getLobbyLocation());
        game.clearKit(player);
        game.getPlayers().remove(player);
        game.getKillsMap().remove(player.getUniqueId());
        if (game.getState() == Game.GameState.STARTING &&
                game.getPlayers().size() < game.getArena().getMinPlayers()) {
            game.cancelStart();
            for (Player p : game.getPlayers()) {
                core.getLangManager().sendMessage("arena.aborting-start", p);
            }
        }

        if (game.getState() == Game.GameState.PLAYING &&
                game.getPlayers().isEmpty()) {
            game.finish();
        }

        if(game.getState() == Game.GameState.PLAYING &&
            game.getPlayers().size() < game.getArena().getMinPlayers()) {
            game.finish();
        }

        updateScoreboard(game);
        updateGameSign(game);
        core.getLobbyManager().invRecover(player, player.getUniqueId());
    }

    public void updateScoreboard(Game game) {
        if (game.getState() == Game.GameState.RESTARTING) {
            return;
        }
        LangManager lang = core.getLangManager();
        String path = "scoreboards.game." + game.getState().name().toLowerCase();
        String title = lang.getText(path + ".title");

        List<String> lines = lang.getStringList(path + ".body");

        Objective obj = game.getBoard().getObjective("ffa");
        if (obj == null) {
            obj = game.getBoard().registerNewObjective("ffa", "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        obj.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));

        for (String entry : new ArrayList<>(game.getBoard().getEntries())) {
            game.getBoard().resetScores(entry);
        }

        List<Player> leaderboard = game.getLeaderboard();

        int score = lines.size();
        int blankCounter = 0;
        ChatColor[] blankColors = {
                ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN,
                ChatColor.DARK_AQUA, ChatColor.DARK_RED, ChatColor.DARK_PURPLE,
                ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA
        };
        int totalSeconds = game.getTime();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        for (String raw : lines) {
            String line = ChatColor.translateAlternateColorCodes('&', raw)
                    .replace("{arena}", game.getArena().getName())
                    .replace("{current}", String.valueOf(game.getPlayers().size()))
                    .replace("{countdown}", String.valueOf(game.getCountdown()))
                    .replace("{time}", formattedTime)
                    .replace("{state}", core.getLangManager()
                            .getText("scoreboards.game.status." + game.getState().name().toLowerCase()))
                    .replace("{server_ip}", core.getLangManager().getText("server-ip"));

            if (raw.equalsIgnoreCase("{blank}") || raw.trim().isEmpty()) {
                line = blankColors[blankCounter % blankColors.length] + "" + ChatColor.RESET;
                blankCounter++;
            }

            if (game.getState() == Game.GameState.PLAYING || game.getState() == Game.GameState.FINISHING) {
                int max = game.getState() == Game.GameState.PLAYING ? 10 : 5;
                for (int i = 1; i <= max; i++) {
                    String replacement = "";
                    if (i <= leaderboard.size()) {
                        Player top = leaderboard.get(i - 1);
                        replacement = core.getLangManager().getText("scoreboards.game.playing.top-format")
                                .replace("{position}", String.valueOf(i))
                                .replace("{name}", top.getName())
                                .replace("{kills}", String.valueOf(game.getKills(top)));
                    }
                    line = line.replace("{top-" + i + "}", replacement);
                }
            }

            obj.getScore(line).setScore(score--);
        }

        for (Player p : game.getPlayers()) {
            p.setScoreboard(game.getBoard());
        }
    }

    public void resetPlayersToLobby(Game game) {
        Location lobby = core.getLobbyManager().getLobbyLocation();
        for (Player p : new ArrayList<>(game.getPlayers())) {
            game.clearKit(p);
            p.teleport(lobby);
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        game.getPlayers().clear();
    }

    public void updateGameSign(final Game game) {
        final GameSign sign = this.core.getSignManager().getSignByArena(game.getArena());
        this.core.getSignManager().updateSign(sign, game);
    }

    public void manageDeath(PlayerDeathEvent e) {
        if (!core.getGameManager().isInGame(e.getEntity().getPlayer())) {
            return;
        }
        Player player = e.getEntity();
        Game game = getGameFromPlayer(player);
        if(game.getState() != Game.GameState.PLAYING) {
            return;
        }
        e.setDeathMessage(null);
        Player killer = null;
        int blockDistance = 0;
        boolean headshot = false;
        if (e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent entityDamageByEntityEvent = (EntityDamageByEntityEvent) e.getEntity().getLastDamageCause();
            if (entityDamageByEntityEvent.getDamager() instanceof Player) {
                killer = (Player) entityDamageByEntityEvent.getDamager();
            } else if (entityDamageByEntityEvent.getDamager() instanceof Arrow) {
                Arrow arrow = (Arrow) entityDamageByEntityEvent.getDamager();
                if (arrow.getShooter() instanceof Player) {
                    killer = (Player) arrow.getShooter();
                    blockDistance = (int) player.getLocation().distance(killer.getLocation());
                    double y = arrow.getLocation().getY();
                    double shotY = player.getLocation().getY();
                    headshot = y - shotY > 1.35d;
                }
            }
        }

        String murderText;
        if (killer != null) {
            if (blockDistance == 0) {
                ItemStack is = killer.getItemInHand();
                murderText = core.getLangManager().getMurderText(player, killer, is);
            } else {
                murderText = core.getLangManager().getRangeMurderText(player, killer, blockDistance, headshot);
                if(headshot) {
                    core.getLangManager().sendMessage("player-messages.headshot", killer);
                }
            }
        } else {
            EntityDamageEvent ede = e.getEntity().getLastDamageCause();
            if (ede != null) {
                if (e.getEntity().getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.VOID) {
                    String killerName = getLastDamager(player);
                    if (killerName != null) {
                        killer = core.getServer().getPlayer(killerName);
                        if (killer != null) {
                            murderText = core.getLangManager().getMurderText(player, killer, null);

                        } else {
                            murderText = core.getLangManager().getNaturalDeathText(player, ede.getCause());
                        }
                    } else {
                        murderText = core.getLangManager().getNaturalDeathText(player, ede.getCause());
                    }
                } else {
                    murderText = core.getLangManager().getNaturalDeathText(player, ede.getCause());
                }
            } else {
                murderText = core.getLangManager().getNaturalDeathText(player, EntityDamageEvent.DamageCause.SUICIDE);
            }
        }

        for (Player receiver : player.getWorld().getPlayers()) {
            String personalizedMessage = murderText;

            if (personalizedMessage.contains(receiver.getName())) {
                personalizedMessage = personalizedMessage.replace(receiver.getName(),
                        ChatColor.BOLD + receiver.getName() + ChatColor.RESET);
            }
            receiver.sendMessage(personalizedMessage);
        }

        if (core.getDB() != null) {
            UUID victimUUID = player.getUniqueId();

            if (killer != null && !killer.getUniqueId().equals(victimUUID)) {
                UUID killerUUID = killer.getUniqueId();

                String msgKiller = core.getLangManager().getText("player-messages.add-kill");
                killer.sendMessage(msgKiller);

                Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
                    core.getDB().addKill(killerUUID, 1);
                });
                Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
                    core.getDB().addDeath(victimUUID, 1);
                });
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
                    core.getDB().addDeath(victimUUID, 1);
                });
            }
        }
    }

    public void setLastDamager(Player player, Player damager) {
        Map.Entry<Long, String> entry = new AbstractMap.SimpleEntry<>((new Date()).getTime(), damager.getName());
        this.lastDamager.put(player.getName(), entry);
    }

    public String getLastDamager(Player player) {
        String ret = null;
        Map.Entry<Long, String> entry = lastDamager.remove(player.getName());
        if (entry != null) {
            long upTo = (new Date()).getTime() - 10000L;
            if ((Long)entry.getKey() > upTo) {
                ret = (String)entry.getValue();
            }
        }

        return ret;
    }

    public boolean isInCombat(Player player) {
        Map.Entry<Long, String> entry = lastDamager.get(player.getName());
        if (entry != null) {
            long cutoff = System.currentTimeMillis() - 10000L;
            return entry.getKey() > cutoff;
        }
        return false;
    }

    public void giveArenaKit(Player player, Arena arena) {
        if (arena.getInvContents() != null) {
            player.getInventory().setContents(arena.getInvContents());
        }
        if (arena.getArmorContents() != null) {
            player.getInventory().setArmorContents(arena.getArmorContents());
        }
    }

}


