package com.mauccio.ffa;

import com.mauccio.ffa.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.mauccio.ffa.cmds.*;

public class Core extends JavaPlugin {

    LangManager lm;
    ArenaManager am;
    GameManager gm;
    SignManager sm;
    EventManager em;
    LobbyManager lb;
    ConfigManager cm;
    TitleManager tm;
    SQLiteManager sql;
    public static Core instance;

    @Override
    public void onEnable() {
        getLogger().info("Enabling FFA...");
        if (getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            getLogger().severe("WorldEdit not found, disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.cm = new ConfigManager(this);
        this.sql = new SQLiteManager();
        this.sql.connect();
        this.lm = new LangManager(this);
        this.am = new ArenaManager(this);
        this.gm = new GameManager(this);
        this.sm = new SignManager(this);

        this.em = new EventManager(this);
        this.lb = new LobbyManager(this);
        this.tm = new TitleManager(this);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            am.loadArenas();
            sm.loadSigns();
        }, 20L);


        registerCommands();
        registerListeners();
    }

    @Override
    public void onDisable() {
        if(sql != null) {
            for(Player player : Bukkit.getServer().getOnlinePlayers()) {
                if(sql != null) sql.saveStats(sql.loadStats(player.getUniqueId()));
            }
            sql.close();
        }
        getLogger().info("Disabling FFA...");
        if (gm != null) gm.shutdownAllGames();
        if (am != null) am.saveArenas();
        if (sm != null) sm.saveAll();
        if (lb != null) lb.saveLobby();

    }

    public void reload() {
        this.cm = new ConfigManager(this);
        this.lm = new LangManager(this);
        this.am = new ArenaManager(this);
        this.gm = new GameManager(this);
        this.sm = new SignManager(this);
        this.em = new EventManager(this);
        this.lb = new LobbyManager(this);
        this.tm = new TitleManager(this);
        this.cm.load();
        this.am.loadArenas();
        this.sm.loadSigns();
    }

    public ArenaManager getArenaManager() {
        return am;
    }

    public LangManager getLangManager() {
        return lm;
    }

    public EventManager getEventManager() {
        return em;
    }

    public SignManager getSignManager() {
        return sm;
    }

    public GameManager getGameManager() {
        return gm;
    }

    public LobbyManager getLobbyManager() {
        return lb;
    }

    public ConfigManager getConfigManager() {
        return cm;
    }

    public TitleManager getTitleManager() {
        return tm;
    }

    public SQLiteManager getDB() {
        return sql;
    }

    public Core getInstance() {
        return instance;
    }

    public void registerCommands() {
        getCommand("freeforall").setExecutor(new MainCommand(this));
    }
    public void registerListeners() {
        Bukkit.getPluginManager().registerEvents(em, this);
    }
}
