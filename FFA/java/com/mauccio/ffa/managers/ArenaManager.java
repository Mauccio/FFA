package com.mauccio.ffa.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.mauccio.ffa.models.Game;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.mauccio.ffa.Core;
import com.mauccio.ffa.models.Arena;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.inventory.ItemStack;

public class ArenaManager {

    private Core core;
    private TreeMap<String,Arena> arenas;
    private File arenasFile;
    private YamlConfiguration arenasYaml;

    public ArenaManager(Core core) {
        this.core = core;
        this.arenas = new TreeMap<>();
        this.arenasFile = new File(core.getDataFolder(), "arenas.yml");
        this.arenasYaml = new YamlConfiguration();
    }

    public void loadArenas() {
        if (!arenasFile.exists()) return;
        arenasYaml = YamlConfiguration.loadConfiguration(arenasFile);

        for (String key : arenasYaml.getKeys(false)) {
            Arena arena = new Arena(key);
            arena.setMinPlayers(arenasYaml.getInt(key + ".min"));
            arena.setMaxPlayers(arenasYaml.getInt(key + ".max"));

            String worldName = arenasYaml.getString(key + ".spectator-spawn.world");
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = arenasYaml.getDouble(key + ".spectator-spawn.x");
                    double y = arenasYaml.getDouble(key + ".spectator-spawn.y");
                    double z = arenasYaml.getDouble(key + ".spectator-spawn.z");
                    float yaw = (float) arenasYaml.getDouble(key + ".spectator-spawn.yaw");
                    float pitch = (float) arenasYaml.getDouble(key + ".spectator-spawn.pitch");
                    arena.setSpectatorSpawn(new Location(world, x, y, z, yaw, pitch));
                    arena.setWorld(world);
                }
            }

            List<Map<?, ?>> spawnList = arenasYaml.getMapList(key + ".spawns");
            for (Map<?, ?> map : spawnList) {
                String spawnWorldName = (String) map.get("world");
                World world = Bukkit.getWorld(spawnWorldName);
                if (world == null) continue;
                double x = ((Number) map.get("x")).doubleValue();
                double y = ((Number) map.get("y")).doubleValue();
                double z = ((Number) map.get("z")).doubleValue();
                float yaw = ((Number) map.get("yaw")).floatValue();
                float pitch = ((Number) map.get("pitch")).floatValue();
                arena.addSpawn(new Location(world, x, y, z, yaw, pitch));
            }

            List<Map<?,?>> areaList = arenasYaml.getMapList(key + ".protected-areas");
            for (Map<?,?> map : areaList) {
                String areaWorldName = (String) map.get("world");
                World world = Bukkit.getWorld(areaWorldName);
                if (world == null) continue;

                int minX = ((Number) map.get("minX")).intValue();
                int minY = ((Number) map.get("minY")).intValue();
                int minZ = ((Number) map.get("minZ")).intValue();
                int maxX = ((Number) map.get("maxX")).intValue();
                int maxY = ((Number) map.get("maxY")).intValue();
                int maxZ = ((Number) map.get("maxZ")).intValue();

                Selection sel = new CuboidSelection(world,
                    new Location(world, minX, minY, minZ),
                    new Location(world, maxX, maxY, maxZ)
                );
                arena.addProtectedArea(sel);
            }

            List<ItemStack> armor = (List<ItemStack>) arenasYaml.getList(key + ".kit.armor");
            if (armor != null) {
                arena.setArmorContents(armor.toArray(new ItemStack[0]));
            }
            List<ItemStack> inv = (List<ItemStack>) arenasYaml.getList(key + ".kit.inventory");
            if (inv != null) {
                arena.setInvContents(inv.toArray(new ItemStack[0]));
            }
            arena.setBuildable(arenasYaml.getBoolean(key + ".buildable", false));
            arenas.put(key, arena);
        }
    }

    public void saveArenas() {
        if (arenas.isEmpty()) {
            core.getLogger().warning("No arenas detected in arenas.yml, ignoring it...");
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();

        for (Arena arena : arenas.values()) {
            String key = arena.getName();
            yaml.set(key + ".min", arena.getMinPlayers());
            yaml.set(key + ".max", arena.getMaxPlayers());

            Location loc = arena.getSpectatorSpawn();
            if (loc != null) {
                yaml.set(key + ".spectator-spawn.world", loc.getWorld().getName());
                yaml.set(key + ".spectator-spawn.x", loc.getX());
                yaml.set(key + ".spectator-spawn.y", loc.getY());
                yaml.set(key + ".spectator-spawn.z", loc.getZ());
                yaml.set(key + ".spectator-spawn.yaw", loc.getYaw());
                yaml.set(key + ".spectator-spawn.pitch", loc.getPitch());
            }

            List<Map<String,Object>> spawns = new ArrayList<>();
            for (Location spawn : arena.getSpawns()) {
                Map<String,Object> map = new HashMap<>();
                map.put("world", spawn.getWorld().getName());
                map.put("x", spawn.getX());
                map.put("y", spawn.getY());
                map.put("z", spawn.getZ());
                map.put("yaw", spawn.getYaw());
                map.put("pitch", spawn.getPitch());
                spawns.add(map);
            }
            yaml.set(key + ".spawns", spawns);

            List<Map<String,Object>> areas = new ArrayList<>();
            for (Selection sel : arena.getProtectedAreas()) {
                Map<String,Object> map = new HashMap<>();
                map.put("world", sel.getWorld().getName());
                map.put("minX", sel.getMinimumPoint().getBlockX());
                map.put("minY", sel.getMinimumPoint().getBlockY());
                map.put("minZ", sel.getMinimumPoint().getBlockZ());
                map.put("maxX", sel.getMaximumPoint().getBlockX());
                map.put("maxY", sel.getMaximumPoint().getBlockY());
                map.put("maxZ", sel.getMaximumPoint().getBlockZ());
                areas.add(map);
            }
            yaml.set(key + ".protected-areas", areas);

            if (arena.getArmorContents() != null) {
                yaml.set(key + ".kit.armor", arena.getArmorContents());
            }
            if (arena.getInvContents() != null) {
                yaml.set(key + ".kit.inventory", arena.getInvContents());
            }

            yaml.set(key + ".buildable", arena.isBuildable());
        }

        try {
            yaml.save(arenasFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveArena(String arenaName, Player player) {
        Arena arena = arenas.get(arenaName);
        if(arena == null) {
            core.getLangManager().sendCmdMessage("arena-setup.missing-arena", player);
            return;
        }
        arenasYaml = YamlConfiguration.loadConfiguration(arenasFile);
        arenasYaml.set(arenaName + ".min", arena.getMinPlayers());
        arenasYaml.set(arenaName + ".max", arena.getMaxPlayers());

        Location loc = arena.getSpectatorSpawn();
        if (loc != null) {
            arenasYaml.set(arenaName + ".spectator-spawn.world", loc.getWorld().getName());
            arenasYaml.set(arenaName + ".spectator-spawn.x", loc.getX());
            arenasYaml.set(arenaName + ".spectator-spawn.y", loc.getY());
            arenasYaml.set(arenaName + ".spectator-spawn.z", loc.getZ());
            arenasYaml.set(arenaName + ".spectator-spawn.yaw", loc.getYaw());
            arenasYaml.set(arenaName + ".spectator-spawn.pitch", loc.getPitch());
        }

        List<Map<String,Object>> spawns = new ArrayList<>();
        for (Location spawn : arena.getSpawns()) {
            Map<String,Object> map = new HashMap<>();
            map.put("world", spawn.getWorld().getName());
            map.put("x", spawn.getX());
            map.put("y", spawn.getY());
            map.put("z", spawn.getZ());
            map.put("yaw", spawn.getYaw());
            map.put("pitch", spawn.getPitch());
            spawns.add(map);
        }
        arenasYaml.set(arenaName + ".spawns", spawns);

        List<Map<String,Object>> areas = new ArrayList<>();
        for (Selection sel : arena.getProtectedAreas()) {
            Map<String,Object> map = new HashMap<>();
            map.put("world", sel.getWorld().getName());
            map.put("minX", sel.getMinimumPoint().getBlockX());
            map.put("minY", sel.getMinimumPoint().getBlockY());
            map.put("minZ", sel.getMinimumPoint().getBlockZ());
            map.put("maxX", sel.getMaximumPoint().getBlockX());
            map.put("maxY", sel.getMaximumPoint().getBlockY());
            map.put("maxZ", sel.getMaximumPoint().getBlockZ());
            areas.add(map);
        }
        arenasYaml.set(arenaName + ".protected-areas", areas);

        if (arena.getArmorContents() != null) {
            arenasYaml.set(arenaName + ".kit.armor", arena.getArmorContents());
        }
        if (arena.getInvContents() != null) {
            arenasYaml.set(arenaName + ".kit.inventory", arena.getInvContents());
        }

        arenasYaml.set(arenaName + ".buildable", arena.isBuildable());

        try {
            arenasYaml.save(arenasFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void create(String arenaName, Player player) {
        if (exists(arenaName)) return;
        Arena arena = new Arena(arenaName);
        arena.setWorld(player.getWorld());
        arena.setSpectatorSpawn(player.getWorld().getSpawnLocation());
        arenas.put(arenaName, arena);
        saveArenas();
    }

    public Arena getArena(String arenaName) {
        return arenas.get(arenaName);
    }

    public boolean exists(String arenaName) {
        return arenas.containsKey(arenaName);
    }

    public void removeArena(String arenaName) {
        arenas.remove(arenaName);
        saveArenas();
    }

    public TreeMap<String, Arena> getArenas() {
        return arenas;
    }

    public void setupTip(String arenaName, Player player) {
        Arena arena = getArena(arenaName);
        if(arena == null) return;
        if(arena.getMaxPlayers() == 0) {
            String msg = core.getLangManager().getText("setup-tips.max-players").replace("{arena}", arenaName);
            player.sendMessage(msg);
            return;
        }

        if(arena.getMinPlayers() == 0) {
            String msg = core.getLangManager().getText("setup-tips.min-players").replace("{arena}", arenaName);
            player.sendMessage(msg);
            return;
        }

        if(arena.getSpectatorSpawn() == null) {
            String msg = core.getLangManager().getText("setup-tips.spectator-spawn").replace("{arena}", arenaName);
            player.sendMessage(msg);
            return;
        }

        if(arena.getSpawns().isEmpty()) {
            String msg = core.getLangManager().getText("setup-tips.add-spawn").replace("{arena}", arenaName);
            player.sendMessage(msg);
            return;
        }

        if(arena.getProtectedAreas().isEmpty()) {
            String msg = core.getLangManager().getText("setup-tips.add-protected-area").replace("{arena}", arenaName);
            player.sendMessage(msg);
            return;
        }

        if(arena.getInvContents() == null) {
            String msg = core.getLangManager().getText("setup-tips.add-start-kit").replace("{arena}", arenaName);
            player.sendMessage(msg);
        }
    }

    public void setMaxPlayers(String arenaName, int max) {
        Arena arena = arenas.get(arenaName);
        if(arena == null) {
            return;
        }
        arena.setMaxPlayers(max);

    }

    public void setMinPlayers(String arenaName, int min) {
        Arena arena = arenas.get(arenaName);
        if(arena == null) {
            return;
        }
        arena.setMinPlayers(min);
    }

    public void setSpectatorSpawn(String arenaName, Location loc) {
        Arena arena = arenas.get(arenaName);
        if(arena == null) {
            return;
        }
        arena.setSpectatorSpawn(loc);
    }

    public void addSpawn(String arenaName, Location loc) {
        Arena arena = arenas.get(arenaName);
        if(arena == null) {
            return;
        }
        arena.addSpawn(loc);
    }

    public void addProtectedArea(String arenaName, Selection sel) {
        Arena arena = arenas.get(arenaName);
        if(arena == null) {
            return;
        }
        arena.addProtectedArea(sel);
    }

    public void setArenaInv(String arenaName, ItemStack[] contents) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) return;
        arena.setInvContents(contents);
    }

    public void setArenaArmor(String arenaName, ItemStack[] armor) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) return;
        arena.setArmorContents(armor);
    }

    public ItemStack[] getArenaInv(String arenaName) {
        Arena arena = arenas.get(arenaName);
        return arena != null ? arena.getInvContents() : null;
    }

    public ItemStack[] getArenaArmor(String arenaName) {
        Arena arena = arenas.get(arenaName);
        return arena != null ? arena.getArmorContents() : null;
    }

    public Arena getArenaFromPlayer(Player player) {
        Game game = core.getGameManager().getGameFromPlayer(player);
        return game != null ? game.getArena() : null;
    }

    public void setBuildable(String arenaName, boolean value) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) return;
        arena.setBuildable(value);
    }

    public void saveArenaKit(String arenaName, Player player) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) return;

        ItemStack[] contents = player.getInventory().getContents();
        List<ItemStack> validContents = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                validContents.add(item.clone());
            }
        }
        arena.setInvContents(validContents.toArray(new ItemStack[0]));

        ItemStack[] armor = player.getInventory().getArmorContents();
        List<ItemStack> validArmor = new ArrayList<>();
        for (ItemStack piece : armor) {
            if (piece != null && piece.getType() != Material.AIR && piece.getAmount() > 0) {
                validArmor.add(piece.clone());
            }
        }
        arena.setArmorContents(validArmor.toArray(new ItemStack[0]));
    }
}