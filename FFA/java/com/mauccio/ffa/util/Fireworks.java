package com.mauccio.ffa.util;

import com.mauccio.ffa.Core;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;

public class Fireworks {

    public static void launch(Core core, Location loc,
                                Color color1, Color color2, Color color3,
                                FireworkEffect.Type type) {
        World world = loc.getWorld();
        new org.bukkit.scheduler.BukkitRunnable() {

            @Override
            public void run() {

                for (int i = -2; i < 3; i++) {
                    org.bukkit.entity.Firework firework = world.spawn(new org.bukkit.Location(loc.getWorld(), loc.getX() + (i * 5), loc.getY(), loc.getZ()), org.bukkit.entity.Firework.class);
                    org.bukkit.inventory.meta.FireworkMeta data = firework.getFireworkMeta();
                    data.addEffects(org.bukkit.FireworkEffect.builder()
                            .withColor(color1).withColor(color2).withColor(color3).with(type)
                            .trail(new java.util.Random().nextBoolean()).flicker(new java.util.Random().nextBoolean()).build());
                    data.setPower(new java.util.Random().nextInt(2) + 2);
                    firework.setFireworkMeta(data);
                }
            }
        }.runTaskLater(core, 10);
    }
}
