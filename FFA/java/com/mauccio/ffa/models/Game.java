package com.mauccio.ffa.models;

import com.mauccio.ffa.Core;
import com.mauccio.ffa.util.Fireworks;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.stream.Collectors;

public class Game {

    private final Arena arena;
    private final List<Player> players;
    private final Scoreboard board;
    private GameState state;
    private int countdown;
    private int time;
    private int finishCountdown;
    private final Map<UUID, Integer> kills = new HashMap<>();
    private int taskId = -1;
    private final Core core;

    public enum GameState {
        WAITING,
        STARTING,
        PLAYING,
        FINISHING,
        RESTARTING
    }

    public Game(Core core, Arena arena) {
        this.core = core;
        this.arena = arena;
        this.players = new ArrayList<>();
        this.state = GameState.WAITING;
        this.countdown = 0;
        this.time = 0;
        this.finishCountdown = 0;
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();
    }

    public void startTicking() {
        if (taskId != -1) return;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(core, this::tick, 20L, 20L);
    }

    public void stopTicking() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void tick() {
        switch (state) {
            case WAITING:
                if (players.size() >= arena.getMinPlayers()) {
                    start();
                }
                break;
            case STARTING:
                tickCountdown();
                break;
            case PLAYING:
                tickGameTime();
                for(Player player : core.getGameManager().getGamePlayers(this)) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                break;
            case FINISHING:
                tickFinishing();
                break;
            case RESTARTING:
                restart();
                state = GameState.WAITING;
                break;
        }
        core.getGameManager().updateScoreboard(this);
        core.getGameManager().updateGameSign(this);
    }

    public void tickCountdown() {
        if (state == GameState.STARTING) {
            if (players.size() < arena.getMinPlayers()) {
                for (Player p : players) {
                    p.sendMessage(ChatColor.RED + "Se canceló el inicio: faltan jugadores.");
                }
                restart();
                return;
            }
            if (countdown > 0) {
                countdown--;

                if (countdown == 10 || countdown == 5 || countdown <= 4) {
                    for (Player p : players) {
                        String msg = core.getLangManager().getText("arena.countdown")
                                .replace("{count}", String.valueOf(countdown));
                        p.sendMessage(msg);
                    }
                }
                for (Player p : players) {
                    switch (countdown) {
                        case 10:
                            core.getTitleManager().sendCountdown10(p);
                            p.playSound(p.getLocation(), core.getConfigManager().getSound("settings.sound.countdown"), 1.0F, 1.0F);
                            break;
                        case 5:
                            core.getTitleManager().sendCountdown5(p);
                            p.playSound(p.getLocation(), core.getConfigManager().getSound("settings.sound.countdown"), 1.0F, 1.0F);
                            break;
                        case 4:
                            core.getTitleManager().sendCountdown4(p);
                            p.playSound(p.getLocation(), core.getConfigManager().getSound("settings.sound.countdown"), 1.0F, 1.0F);
                            break;
                        case 3:
                            core.getTitleManager().sendCountdown3(p);
                            p.playSound(p.getLocation(), core.getConfigManager().getSound("settings.sound.countdown"), 1.0F, 1.0F);
                            break;
                        case 2:
                            core.getTitleManager().sendCountdown2(p);
                            p.playSound(p.getLocation(), core.getConfigManager().getSound("settings.sound.countdown"), 1.0F, 1.0F);
                            break;
                        case 1:
                            core.getTitleManager().sendCountdown1(p);
                            p.playSound(p.getLocation(), core.getConfigManager().getSound("settings.sound.countdown"), 1.0F, 1.0F);
                            break;
                        case 0:
                            core.getTitleManager().sendGameStarted(p);
                            p.playSound(p.getLocation(), core.getConfigManager().getSound("settings.sound.start"), 1.0F, 1.0F);
                            break;
                    }
                }

                if (countdown == 0) {
                    this.state = GameState.PLAYING;
                    this.time = JavaPlugin.getPlugin(Core.class).getConfig().getInt("settings.game.time", 30);
                    for (Player p : players) {
                        Location spawn = arena.getRandomSpawn();
                        if (spawn != null) p.teleport(spawn);
                        core.getGameManager().giveArenaKit(p, arena);
                        String msg = core.getLangManager().getText("arena.started")
                                .replace("{arena}", arena.getName());
                        p.sendMessage(msg);
                    }
                }
            }
        }
    }

    public void cancelStart() {
        this.countdown = 0;
        this.state = GameState.WAITING;
    }

    public void clearKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setHelmet(new ItemStack(Material.AIR));
        player.getInventory().setChestplate(new ItemStack(Material.AIR));
        player.getInventory().setLeggings(new ItemStack(Material.AIR));
        player.getInventory().setBoots(new ItemStack(Material.AIR));
    }

    public void tickGameTime() {
        if (state == GameState.PLAYING && time > 0) {
            time--;
            if (time == 0) {
                finish();
            }
        }
    }

    public void tickFinishing() {
        if (state == GameState.FINISHING && finishCountdown > 0) {
            finishCountdown--;
            Fireworks.launch(core, getTop1().getLocation(), Color.YELLOW, Color.ORANGE, Color.AQUA, FireworkEffect.Type.BALL_LARGE);
            if (finishCountdown == 0) {
                restart();
            }
        }
    }

    public int getCountdown() {
        return countdown;
    }

    public void start() {
        if (players.size() >= arena.getMinPlayers()) {
            this.state = GameState.STARTING;
            this.countdown = 10;
        } else {
            for (Player p : players) {
                p.sendMessage(ChatColor.RED + "Esperando más jugadores para empezar...");
            }
        }
    }

    public void finish() {
        this.state = GameState.FINISHING;
        this.finishCountdown = 10;

        for (Player p : players) {
            clearKit(p);
            String msg = core.getLangManager().getText("arena.finished")
                    .replace("{arena}", arena.getName());
            p.sendMessage(msg);
        }
    }

    public void restart() {
        core.getGameManager().resetPlayersToLobby(this);
        countdown = 0;
        time = 0;
        finishCountdown = 0;
        kills.clear();
        state = GameState.RESTARTING;

    }

    public int getTime() {
        return time;
    }

    public GameState getState() {
        return state;
    }

    public Scoreboard getBoard() {
        return board;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Arena getArena() {
        return arena;
    }

    public Player getTop1() {
        List<Player> leaderboard = getLeaderboard();
        return leaderboard.isEmpty() ? null : leaderboard.get(0);
    }

    public void addKill(Player killer) {
        kills.put(killer.getUniqueId(), kills.getOrDefault(killer.getUniqueId(), 0) + 1);
    }

    public int getKills(Player player) {
        return kills.getOrDefault(player.getUniqueId(), 0);
    }

    public List<Player> getLeaderboard() {
        return players.stream()
                .sorted((p1, p2) -> Integer.compare(getKills(p2), getKills(p1)))
                .collect(Collectors.toList());
    }

    public Map<UUID, Integer> getKillsMap() {
        return kills;
    }

    public void setState(GameState state) {
        this.state = state;
    }
}