package com.mauccio.ffa.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mauccio.ffa.Core;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class LangManager {

    private final Core core;
    private final YamlConfiguration lang;
    private final String messagePrefix;

    public LangManager(Core core) {
        this.core = core;
        lang = new YamlConfiguration();
        File langFile = new File(core.getDataFolder(), "lang.yml");

        if (!langFile.exists()) {
            core.saveResource("lang.yml", false);
        }

        try {
            lang.load(langFile);
        } catch (IOException | InvalidConfigurationException ex) {
            core.getLogger().severe("Error loading lang.yml: " + ex.getMessage());
        }

        messagePrefix = ChatColor.translateAlternateColorCodes('&',
                lang.getString("prefix"));
    }

    private String translateUnicode(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length();) {
            char c = input.charAt(i);
            if (c == '\\' && i + 5 < input.length() && input.charAt(i + 1) == 'u') {
                String hex = input.substring(i + 2, i + 6);
                try {
                    int code = Integer.parseInt(hex, 16);
                    sb.append((char) code);
                    i += 6;
                    continue;
                } catch (NumberFormatException ignored) {}
            }
            sb.append(c);
            i++;
        }
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public String getChar(String path) {
        String raw = lang.getString(path);
        return translateUnicode(raw);
    }

    public List<String> getStringList(String path) {
        List<String> list = lang.getStringList(path);
        List<String> colored = new ArrayList<>();
        for (String line : list) {
            colored.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return colored;
    }

    public String getText(String label) {
        String text = lang.getString(label);
        if (text == null) {
            text = label;
        } else {
            text = ChatColor.translateAlternateColorCodes('&', text);
            text = text.replace("{prefix}", messagePrefix);
        }
        return text;
    }

    public String getCmdText(String label) {
        String text = lang.getString("commands."+label);
        if (text == null) {
            text = label;
        } else {
            text = ChatColor.translateAlternateColorCodes('&', text);
            text = text.replace("{prefix}", messagePrefix);
        }
        return text;
    }

    public void sendMessage(String label, Player player) {
        player.sendMessage(getText(label));
    }

    public void sendMessage(String label, CommandSender cs) {
        cs.sendMessage(getText(label));
    }

    public void sendCmdMessage(String label, Player player) {
        player.sendMessage(getCmdText(label));
    }

    public void sendCmdMessage(String label, CommandSender cs) {
        cs.sendMessage(getCmdText(label));
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }

    public void sendText(String baseLabel, Player player) {
        if (lang.getString(baseLabel) == null) {
            sendMessage(baseLabel, player);
            return;
        }
        for (String label : lang.getConfigurationSection(baseLabel).getKeys(false)) {
            sendMessage(baseLabel + "." + label, player);
        }
    }

    public void sendRawMessage(String path, CommandSender sender) {
        for (String line : getStringList(path)) {
            sender.sendMessage(line);
        }
    }

    public String getTitleMessage(String label) {
        return this.getText(label);
    }

    public void sendVerbatimTextToWorld(String text, World world, Player filter) {
        for (Player receiver : world.getPlayers()) {
            if (filter != null && receiver.getName().equals(filter.getName())) {
                continue;
            }
            receiver.sendMessage(messagePrefix + " " + text);
        }
    }

    public void sendMessageToWorld(String label, World world, Player filter) {
        String text = getText(label);
        for (Player receiver : world.getPlayers()) {
            if (filter != null && receiver.getName().equals(filter.getName())) {
                continue;
            }
            receiver.sendMessage(messagePrefix + " " + text);
        }
    }

    public String getMurderText(Player player, Player killer, ItemStack is) {
        String ret = ChatColor.translateAlternateColorCodes('&',
                lang.getString("death-events.by-player.message"));
        ret = ret.replace("{killer}", killer.getName());
        ret = ret.replace("{killed}", player.getName());
        String how;
        if (is != null) {
            how = lang.getString("death-events.by-player.melee.".concat(is.getType().name()));
            if (how == null) {
                how = lang.getString("death-events.by-player.melee._OTHER_");
            }
        } else {
            how = lang.getString("death-events.by-player.melee.PULL");
        }
        ret = ret.replace("{how}", how);
        return ret;
    }

    public String getRangeMurderText(Player player, Player killer, int distance, boolean headshoot) {
        String ret = ChatColor.translateAlternateColorCodes('&',
                lang.getString("death-events.by-player.message"));
        ret = ret.replace("{killer}", killer.getName());
        ret = ret.replace("{killed}", player.getName());
        if (headshoot) {
            ret = ret.replace("{how}", lang.getString("death-events.by-player.range.HEADSHOT"));
        } else {
            ret = ret.replace("{how}", lang.getString("death-events.by-player.range.BODYSHOT"));
        }
        ret = ret.replace("{distance}", distance + "");
        return ret;
    }

    public String getNaturalDeathText(Player player, EntityDamageEvent.DamageCause cause) {
        String ret = ChatColor.translateAlternateColorCodes('&',
                lang.getString("death-events.natural.message"));
        ret = ret.replace("{killed}", player.getName());
        String how = lang.getString("death-events.natural.cause.".concat(cause.name()));
        if (how == null) {
            how = lang.getString("death-events.natural.cause._OTHER_");
        }
        ret = ret.replace("{how}", how);
        return ret;
    }
}
