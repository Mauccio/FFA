package com.mauccio.ffa.managers;

import com.mauccio.ffa.Core;
import com.mauccio.ffa.models.Arena;
import com.mauccio.ffa.models.Game;
import com.mauccio.ffa.models.GameSign;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SignManager {

    private File file;
    private FileConfiguration config;
    private final Map<Location, GameSign> signs = new HashMap<>();
    private GameSign gameSign;
    private Core core;

    public SignManager(Core core) {
        this.core = core;
        file = new File(core.getDataFolder(), "signs.yml");
        if (!file.exists()) {
            try {
                core.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadSigns();
    }

    public void registerSign(Arena arena, Location location) {
        GameSign sign = new GameSign(arena, location);
        signs.put(location, sign);

        String path = "signs." + arena.getName() + "."
                + location.getWorld().getName() + "_"
                + location.getBlockX() + "_"
                + location.getBlockY() + "_"
                + location.getBlockZ();

        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getBlockX());
        config.set(path + ".y", location.getBlockY());
        config.set(path + ".z", location.getBlockZ());
        config.set(path + ".arena", arena.getName());

        save();
        Game game = core.getGameManager().getGame(arena);
        if (game == null) {
            game = new Game(core, arena);
            game.restart();
            core.getGameManager().registerGame(arena, game);
        }
        updateSign(sign, game);
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAll() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSigns() {
        if (config.getConfigurationSection("signs") == null) {
            return;
        }

        for (String arenaKey : config.getConfigurationSection("signs").getKeys(false)) {
            for (String locKey : config.getConfigurationSection("signs." + arenaKey).getKeys(false)) {
                String path = "signs." + arenaKey + "." + locKey;
                String worldName = config.getString(path + ".world");
                int x = config.getInt(path + ".x");
                int y = config.getInt(path + ".y");
                int z = config.getInt(path + ".z");
                String arenaName = config.getString(path + ".arena");

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location loc = new Location(world, x, y, z);
                    Arena arena = core.getArenaManager().getArena(arenaName);
                    if (arena != null) {
                        GameSign sign = new GameSign(arena, loc);
                        signs.put(loc, sign);

                        if (core.getGameManager().getGame(arena) == null) {
                            Game game = new Game(core, arena);
                            game.restart();
                            core.getGameManager().registerGame(arena, game);

                            updateSign(sign, game);
                        }
                    }
                }
            }
        }
    }

    public GameSign getSign(Location location) {
        return signs.get(location);
    }

    public boolean isGameSign(Location location) {
        return signs.containsKey(location);
    }

    public void removeSign(Location location) {
        GameSign sign = signs.remove(location);
        if (sign == null) return;

        String path = "signs." + sign.getArena().getName() + "."
                + location.getWorld().getName() + "_"
                + location.getBlockX() + "_"
                + location.getBlockY() + "_"
                + location.getBlockZ();

        config.set(path, null);
        save();
    }

    public void updateSign(GameSign sign, Game game) {
        Game.GameState state = game.getState();
        String stateText = core.getLangManager().getText("signs.state." + state.name().toLowerCase());
        String[] lines = new String[] {
                core.getLangManager().getText("signs.arena.line-1")
                        .replace("{current}", String.valueOf(game.getPlayers().size()))
                        .replace("{state}", stateText)
                        .replace("{arena}", game.getArena().getName())
                        .replace("{max}", String.valueOf(game.getArena().getMaxPlayers())),
                core.getLangManager().getText("signs.arena.line-2")
                        .replace("{current}", String.valueOf(game.getPlayers().size()))
                        .replace("{state}", stateText)
                        .replace("{arena}", game.getArena().getName())
                        .replace("{max}", String.valueOf(game.getArena().getMaxPlayers())),
                core.getLangManager().getText("signs.arena.line-3")
                        .replace("{current}", String.valueOf(game.getPlayers().size()))
                        .replace("{state}", stateText)
                        .replace("{arena}", game.getArena().getName())
                        .replace("{max}", String.valueOf(game.getArena().getMaxPlayers())),
                core.getLangManager().getText("signs.arena.line-4")
                        .replace("{current}", String.valueOf(game.getPlayers().size()))
                        .replace("{state}", stateText)
                        .replace("{arena}", game.getArena().getName())
                        .replace("{max}", String.valueOf(game.getArena().getMaxPlayers()))
        };
        sign.updateSign(lines);
    }

    public GameSign getSignByArena(Arena arena) {
        for (GameSign sign : signs.values()) {
            if (sign.getArena().equals(arena)) {
                return sign;
            }
        }
        return null;
    }
}