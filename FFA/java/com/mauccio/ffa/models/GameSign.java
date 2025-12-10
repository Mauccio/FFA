package com.mauccio.ffa.models;

import org.bukkit.block.Sign;
import org.bukkit.Location;

public class GameSign {
    private final Arena arena;
    private final Location location;

    public GameSign(Arena arena, Location location) {
        this.arena = arena;
        this.location = location;
    }

    public Arena getArena() {
        return arena;
    }

    public Location getLocation() {
        return location;
    }

    public void updateSign(String[] lines) {
        if (location.getBlock().getState() instanceof Sign) {
            Sign sign = (Sign) location.getBlock().getState();
            for (int i = 0; i < lines.length && i < 4; i++) {
                sign.setLine(i, lines[i]);
            }
            sign.update();
        }
    }
}