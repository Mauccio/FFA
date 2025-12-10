package com.mauccio.ffa.models;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.inventory.ItemStack;

public class Arena {
    
    private String name;
    private int min;
    private int max;
    private Location spectSpawn;
    private List<Player> arenaPlayers;
    private List<Selection> protectedAreas;
    private List<Location> spawns;
    private World world;
    private int time;
    private ItemStack[] armorContents;
    private ItemStack[] invContents;
    private boolean buildable;

    public Arena(String name) {
        this.name = name;
        this.arenaPlayers = new ArrayList<>();
        this.protectedAreas = new ArrayList<>();
        this.spawns = new ArrayList<>();
    }
    
    public String getName() {
        return name;
    }

    public World getWorld() {
        return world;
    }

    public int getMinPlayers() {
        return min;
    }

    public int getMaxPlayers() {
        return max;
    }

    public Location getSpectatorSpawn() {
        return spectSpawn;
    }

    public List<Player> getPlayers() {
        return arenaPlayers;
    }

    public List<Selection> getProtectedAreas() {
        return protectedAreas;
    }

    public List<Location> getSpawns() {
        return spawns;
    }

    public void setName(String arenaName) {
        this.name = arenaName;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void setMinPlayers(int min) {
        this.min = min;
    }

    public void setMaxPlayers(int max) {
        this.max = max;
    }

    public void setSpectatorSpawn(Location loc) {
        this.spectSpawn = loc;
    }

    public void addToArena(Player player) {
        this.arenaPlayers.add(player);
    }

    public void removeFromArena(Player player) {
        this.arenaPlayers.remove(player);
    }

    public void addProtectedArea(Selection sel) {
        this.protectedAreas.add(sel);
    }

    public void removeProtectedArea(Selection sel) {
        this.protectedAreas.remove(sel);
    }

    public void addSpawn(Location loc) {
        this.spawns.add(loc);
    }

    public void removeSpawn(Location loc) {
        this.spawns.remove(loc);
    }

    public Location getRandomSpawn() {
        if (spawns == null || spawns.isEmpty()) {
            return null;
        }
        return spawns.get(new java.util.Random().nextInt(spawns.size()));
    }

    public void setArmorContents(ItemStack[] armorContents) {
        this.armorContents = armorContents;
    }

    public ItemStack[] getArmorContents() {
        return armorContents;
    }

    public void setInvContents(ItemStack[] invContents) {
        this.invContents = invContents;
    }

    public ItemStack[] getInvContents() {
        return invContents;
    }

    public boolean isBuildable() {
        return buildable;
    }

    public void setBuildable(boolean value) {
        this.buildable = value;
    }
}
