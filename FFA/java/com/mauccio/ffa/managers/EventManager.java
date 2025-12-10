package com.mauccio.ffa.managers;

import com.mauccio.ffa.Core;
import com.mauccio.ffa.models.Arena;
import com.mauccio.ffa.models.Game;
import com.mauccio.ffa.models.GameSign;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EventManager implements Listener {

    private Core core;

    public EventManager(Core core) {
        this.core = core;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String firstLine = event.getLine(0);
        if (firstLine != null && firstLine.equalsIgnoreCase("ffa")) {
            String arenaName = event.getLine(1);
            if (arenaName == null || arenaName.trim().isEmpty()) {
                core.getLangManager().sendCmdMessage("arena-setup.sign-arena-need", event.getPlayer());
                return;
            }
            Arena arena = core.getArenaManager().getArena(arenaName);
            if (arena != null) {
                core.getSignManager().registerSign(arena, event.getBlock().getLocation());

                Game game = core.getGameManager().getGame(arena);
                if (game == null) {
                    game = new Game(core, arena);
                    game.restart();
                    core.getGameManager().registerGame(arena, game);
                }
                Location loc = event.getBlock().getLocation();
                GameSign sign = core.getSignManager().getSign(loc);
                core.getSignManager().updateSign(sign, game);
                String msg = core.getLangManager().getText("commands.arena-setup.arena-available")
                        .replace("{arena}", arenaName);
                event.getPlayer().sendMessage(msg);
            } else {
                String msg = core.getLangManager().getText("commands.arena-setup.missing-arena")
                        .replace("{arena}", arenaName);
                event.getPlayer().sendMessage(msg);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Location loc = event.getClickedBlock().getLocation();
            if (core.getSignManager().isGameSign(loc)) {
                GameSign sign = core.getSignManager().getSign(loc);
                Game game = core.getGameManager().getGame(sign.getArena());
                if (game != null) {
                    if(game.getState() == Game.GameState.FINISHING ||
                            game.getState() == Game.GameState.RESTARTING) {
                        core.getLangManager().sendMessage("arena.finishing", event.getPlayer());
                        return;
                    }
                    if(game.getState() == Game.GameState.PLAYING) {
                        if(!core.getConfigManager().canJoinInGame()) {
                            core.getLangManager().sendMessage("arena.in-game", event.getPlayer());
                        }
                    }
                    core.getGameManager().addPlayerToGame(game, event.getPlayer());
                    String msg = core.getLangManager().getText("arena.joining")
                            .replace("{arena}", sign.getArena().getName());
                    event.getPlayer().sendMessage(msg);
                }
            }
        }
    }

    @EventHandler
    public void onSignBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        if (core.getSignManager().isGameSign(loc)) {
            if (event.getPlayer().isOp() || event.getPlayer().hasPermission("ffa.admin")) {
                GameSign sign = core.getSignManager().getSign(loc);
                core.getSignManager().removeSign(loc);
                core.getGameManager().removeGame(sign.getArena());
                String msg = core.getLangManager().getText("commands.arena-setup.sign-removed")
                        .replace("{arena}", sign.getArena().getName());
                event.getPlayer().sendMessage(msg);
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onItemJoinUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Game game = core.getGameManager().getGameFromPlayer(player);

        if (game == null) return;

        ItemStack item = player.getItemInHand();
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String name = meta.getDisplayName();
        if (name == null) return;

        if (name.equalsIgnoreCase(core.getConfigManager().getString("items.join-game.name"))) {
            event.setCancelled(true);
            core.getGameManager().moveToRandomSpawn(game, player);
            player.getInventory().clear();
            core.getGameManager().giveArenaKit(player, game.getArena());
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = core.getGameManager().getGameFromPlayer(player);
        if (game == null) return;

        Arena arena = game.getArena();
        if (arena == null) return;

        Location loc = event.getBlockPlaced().getLocation();
        for (Selection sel : arena.getProtectedAreas()) {
            if (sel.contains(loc)) {
                event.setCancelled(true);
                core.getLangManager().sendMessage("arena.protected-area", player);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Game game = core.getGameManager().getGameFromPlayer(player);
        if (game == null) return;

        Arena arena = game.getArena();
        if (arena == null) return;

        Location loc = event.getBlock().getLocation();
        for (Selection sel : arena.getProtectedAreas()) {
            if (sel.contains(loc)) {
                event.setCancelled(true);
                core.getLangManager().sendMessage("arena.protected-area", player);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Game game = core.getGameManager().getGameFromPlayer(player);
        if (game == null) return;

        switch (game.getState()) {
            case WAITING:
            case STARTING:
            case FINISHING:
                event.setCancelled(true);
                break;
            default:
                // Do nothing.
                break;
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        Game game = core.getGameManager().getGameFromPlayer(victim);
        if (game == null) return;

        if (killer != null && game.getPlayers().contains(killer)) {
            game.addKill(killer);
            killer.playSound(killer.getLocation(), Sound.NOTE_PLING, 1.0F, 2.0F);
        }

        event.getDrops().clear();
        event.setDroppedExp(0);
        Bukkit.getScheduler().runTaskLater(core, () -> {
            victim.spigot().respawn();
            if (game.getArena().getSpawns() == null || game.getArena().getSpawns().isEmpty()) {
                victim.teleport(core.getLobbyManager().getLobbyLocation());
                game.finish();
                return;
            }
            victim.teleport(game.getArena().getRandomSpawn());

            Bukkit.getScheduler().runTaskLater(core, () -> {
                core.getGameManager().giveArenaKit(victim, game.getArena());
                victim.updateInventory();
            }, 2L);
        }, 1L);


        core.getGameManager().updateScoreboard(game);
        core.getGameManager().updateGameSign(game);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Game game = core.getGameManager().getGameFromPlayer(player);
        if (game == null) return;
        core.getGameManager().removePlayer(player);
        Bukkit.getScheduler().runTaskLater(core, () -> {
            core.getGameManager().updateScoreboard(game);
            core.getGameManager().updateGameSign(game);
        }, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(core.getLobbyManager().getLobbyLocation() != null) {
            Location lobby = core.getLobbyManager().getLobbyLocation();
            player.teleport(lobby);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        core.getGameManager().manageDeath(e);
    }


    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        Game gameVictim = core.getGameManager().getGameFromPlayer(victim);
        Game gameAttacker = core.getGameManager().getGameFromPlayer(attacker);

        if (gameAttacker == null || gameVictim != gameAttacker) {
            return;
        }

        if (gameVictim.getState() != Game.GameState.PLAYING) {
            event.setCancelled(true);
            core.getLangManager().sendMessage("arena.no-pvp", attacker);
            return;
        }
        core.getGameManager().setLastDamager(victim, attacker);
    }

    @EventHandler
    public void onArenaBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = core.getGameManager().getGameFromPlayer(player);
        if (game == null) return;

        Arena arena = game.getArena();
        if (arena == null) return;

        boolean allowPlacement = arena.isBuildable();
        int blockTime = core.getConfigManager().getBlockTime();
        if (!allowPlacement) {
            event.setCancelled(true);
            core.getLangManager().sendMessage("arena.not-buildable", player);
            return;
        }

        Block block = event.getBlockPlaced();
        Bukkit.getScheduler().runTaskLater(core, () -> {
            if (block.getType() != Material.AIR) {
                block.setType(Material.AIR);
            }
        }, blockTime * 20L);
    }

    @EventHandler
    public void onArenaBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Game game = core.getGameManager().getGameFromPlayer(player);
        if(game != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void atArenaTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Game game = core.getGameManager().getGameFromPlayer(player);
        if(game != null) {
            if(game.getState() == Game.GameState.WAITING || game.getState() == Game.GameState.STARTING) {
                if(event.getTo().getWorld() == game.getArena().getWorld()) {
                    player.setGameMode(GameMode.ADVENTURE);
                    player.setFlying(true);
                }
            }
        }
    }
}
