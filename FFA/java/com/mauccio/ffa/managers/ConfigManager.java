package com.mauccio.ffa.managers;

import com.mauccio.ffa.Core;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private final Core core;
    private FileConfiguration config;

    public ConfigManager(Core core) {
        this.core = core;
        this.config = core.getConfig();
        core.saveDefaultConfig();
        this.load(false);
    }

    public void load() {
        this.load(true);
    }

    public void persists() {
        this.core.saveConfig();
    }

    private void load(boolean reload) {
        if (reload) {
            this.core.reloadConfig();
        }
    }

    public boolean isVoidInstaKill() {
        return this.core.getConfig().getBoolean("settings.game.void-instakill", false);
    }

    public boolean isFallDamage() {
        return this.core.getConfig().getBoolean("settings.game.fall-damage", false);
    }

    public int getBlockTime() {
        return core.getConfig().getInt("settings.game.block-time", 10);
    }

    public boolean canJoinInGame() {
        return this.core.getConfig().getBoolean("settings.game.join-in-game", true);
    }

    public boolean useLobbyGuard() {
        return this.core.getConfig().getBoolean("settings.lobby.guard", false);
    }

    public boolean giveLobbyItems() {
        return this.core.getConfig().getBoolean("settings.lobby.items", false);
    }

    public String getString(String path) {
        String text = this.core.getConfig().getString(path);
        if (text == null) {
            text = path;
        } else {
            text = ChatColor.translateAlternateColorCodes('&', text);
        }
        return text;
    }

    public List<String> getStringList(String path) {
        List<String> list = config.getStringList(path);
        List<String> colored = new ArrayList<>();
        for (String line : list) {
            colored.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return colored;
    }

    public Sound getSound(String path) {
        String sound = config.getString(path);
        return Sound.valueOf(sound);
    }
}