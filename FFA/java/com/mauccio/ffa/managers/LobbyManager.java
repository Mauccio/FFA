package com.mauccio.ffa.managers;

import com.mauccio.ffa.Core;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LobbyManager {

    private Core core;
    private Location loc;
    private final Map<UUID, ItemStack[]> inventoryBackup = new HashMap<>();

    public LobbyManager(Core core) {
        this.core = core;
        loadLobby();
    }

    public void saveLobby() {
        if (loc != null) {
            FileConfiguration config = core.getConfig();
            config.set("lobby.world", loc.getWorld().getName());
            config.set("lobby.x", loc.getX());
            config.set("lobby.y", loc.getY());
            config.set("lobby.z", loc.getZ());
            config.set("lobby.yaw", loc.getYaw());
            config.set("lobby.pitch", loc.getPitch());
            core.saveConfig();
        }
    }

    public void setLobbyLocation(Location loc) {
        this.loc = loc;

        FileConfiguration config = core.getConfig();
        config.set("lobby.world", loc.getWorld().getName());
        config.set("lobby.x", loc.getX());
        config.set("lobby.y", loc.getY());
        config.set("lobby.z", loc.getZ());
        config.set("lobby.yaw", loc.getYaw());
        config.set("lobby.pitch", loc.getPitch());

        core.saveConfig();
    }

    public Location getLobbyLocation() {
        if (loc == null) {
            World world = Bukkit.getWorlds().get(0);
            return world.getSpawnLocation();
        }
        return loc;
    }

    private void loadLobby() {
        FileConfiguration config = core.getConfig();
        if (config.contains("lobby.world")) {
            String worldName = config.getString("lobby.world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = config.getDouble("lobby.x");
                double y = config.getDouble("lobby.y");
                double z = config.getDouble("lobby.z");
                float yaw = (float) config.getDouble("lobby.yaw");
                float pitch = (float) config.getDouble("lobby.pitch");

                this.loc = new Location(world, x, y, z, yaw, pitch);
            }
        }
    }

    public void invSaver(Player player, UUID uuid) {
        inventoryBackup.put(uuid, player.getInventory().getContents());
    }

    public void invRecover(Player player, UUID uuid) {
        if (inventoryBackup.containsKey(uuid)) {
            player.getInventory().setContents(inventoryBackup.get(uuid));
            inventoryBackup.remove(uuid);
        }
    }
}