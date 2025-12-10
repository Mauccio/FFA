package com.mauccio.ffa.cmds;

import com.mauccio.ffa.models.Arena;
import com.mauccio.ffa.models.Game;
import com.mauccio.ffa.models.Stats;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mauccio.ffa.Core;

import java.util.List;
import java.util.UUID;

public class MainCommand implements CommandExecutor {

    Core core;

    public MainCommand(Core core) {
        this.core = core;
    }

    private void sendHelpList(Player player) {
        core.getLangManager().sendRawMessage("commands.lists.help", player);
    }

    private void sendSetupList(Player player) {
        core.getLangManager().sendRawMessage("commands.lists.setup", player);
    }

    private void sendArenaList(Player player) {
        core.getLangManager().sendRawMessage("commands.lists.arena", player);
    }

    private Selection getSelection(Player player) {
        WorldEditPlugin we = (WorldEditPlugin) core.getServer().getPluginManager().getPlugin("WorldEdit");
        return (we != null) ? we.getSelection(player) : null;
    }

    private boolean requireSelection(Player player) {
        Selection sel = getSelection(player);
        if (sel == null) {
            core.getLangManager().sendCmdMessage("arena-setup.missing-selection", player);
            return false;
        }
        return true;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if(cs instanceof Player) {
            Player p = (Player) cs;
            if (args.length == 0) {
                sendHelpList(p);
                return true;
            }
            String subCmds = args[0].toLowerCase();
            switch(subCmds) {
                case "stats":
                    UUID uuid = p.getUniqueId();
                    Stats stats = core.getDB().loadStats(uuid);
                    List<String> lines = core.getLangManager().getStringList("commands.lists.stats");

                    for (String raw : lines) {
                        String line = ChatColor.translateAlternateColorCodes('&', raw)
                                .replace("{kills}", String.valueOf(stats.getKills()))
                                .replace("{deaths}", String.valueOf(stats.getDeaths()));
                        p.sendMessage(line);
                    }
                    break;
                case "reload":
                    core.reload();
                    core.getLangManager().sendCmdMessage("reload", p);
                    break;
                case "version":
                    String version = core.getDescription().getVersion();
                    String msg = core.getLangManager().getCmdText("version");
                    msg = msg.replace("{version}", version);
                    p.sendMessage(msg);
                    break;
                case "leave":
                    if(core.getGameManager().isInCombat(p)) {
                        core.getLangManager().sendMessage("arena.in-combat", p);
                        return true;
                    }
                    if (core.getGameManager().isInGame(p)) {
                        core.getGameManager().removePlayer(p);
                    } else {
                        core.getLangManager().sendMessage("arena.not-in", p);
                    }
                    break;
                case "setup":
                if (args.length == 1) {
                    sendSetupList(p);
                    return true;
                }

                String setupSub = args[1].toLowerCase();
                switch (setupSub) {
                    case "setlobby":
                        Location loc = p.getLocation();
                        core.getLobbyManager().setLobbyLocation(loc);
                        p.sendMessage("Success!");
                        break;
                    case "arena":
                        if (args.length == 2) {
                            sendArenaList(p);
                            return true;
                        }
                        String arenaAction = args[2].toLowerCase();
                        switch (arenaAction) {
                            case "add":
                                if (args.length >= 4) {
                                    String arenaName = args[3];
                                    Arena arena = core.getArenaManager().getArena(arenaName);
                                    if(arena != null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.already-exists")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }
                                    core.getArenaManager().create(arenaName, p);
                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.added")
                                            .replace("{arena}", arenaName));
                                    core.getArenaManager().setupTip(arenaName, p);
                                } else {
                                    p.sendMessage(core.getLangManager()
                                        .getCmdText("arena-setup.missing-name"));
                                }
                                break;
                            case "remove":
                                if (args.length >= 4) {
                                    String arenaName = args[3];
                                    Arena arena = core.getArenaManager().getArena(arenaName);
                                    if(arena == null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-arena")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }
                                    core.getArenaManager().removeArena(arenaName);
                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.removed")
                                            .replace("{arena}", arenaName));
                                } else {
                                    p.sendMessage(core.getLangManager()
                                        .getCmdText("arena-setup.missing-name"));
                                }
                                break;
                            case "list":
                                if(core.getArenaManager().getArenas() == null || core.getArenaManager().getArenas().isEmpty()) {
                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.list-empty"));
                                } else {
                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.list-title"));
                                    for (String arenaName : core.getArenaManager().getArenas().keySet()) {
                                        p.sendMessage("-" + arenaName);
                                    }
                                }
                                break;
                            case "max":
                                if (args.length >= 5) {
                                    String arenaName = args[3];
                                    Arena arena = core.getArenaManager().getArena(arenaName);
                                    if(arena == null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-arena")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }
                                    try {
                                        int maxPlayers = Integer.parseInt(args[4]);
                                        core.getArenaManager().setMaxPlayers(arenaName, maxPlayers);
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.max-set")
                                                .replace("{arena}", arenaName)
                                                .replace("{max}", String.valueOf(maxPlayers)));
                                        core.getArenaManager().saveArena(arenaName, p);
                                        core.getArenaManager().setupTip(arenaName, p);
                                    } catch (NumberFormatException e) {
                                        p.sendMessage(core.getLangManager().getCmdText("arena-setup.invalid-number"));
                                    }
                                } else {
                                    p.sendMessage(core.getLangManager().getCmdText("arena-setup.missing-number"));
                                }
                                break;

                            case "min":
                                if (args.length >= 5) {
                                    String arenaName = args[3];
                                    Arena arena = core.getArenaManager().getArena(arenaName);
                                    if(arena == null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-arena")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }
                                    try {
                                        int minPlayers = Integer.parseInt(args[4]);
                                        core.getArenaManager().setMinPlayers(arenaName, minPlayers);
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.min-set")
                                                .replace("{arena}", arenaName)
                                                .replace("{min}", String.valueOf(minPlayers)));
                                        core.getArenaManager().saveArena(arenaName, p);
                                        core.getArenaManager().setupTip(arenaName, p);
                                    } catch (NumberFormatException e) {
                                        p.sendMessage(core.getLangManager().getCmdText("arena-setup.invalid-number"));
                                    }
                                } else {
                                    p.sendMessage(core.getLangManager().getCmdText("arena-setup.missing-number"));
                                }
                                break;

                            case "setspectatorspawn":
                                if (args.length >= 4) {
                                    String arenaName = args[3];
                                    Arena arena = core.getArenaManager().getArena(arenaName);
                                    if(arena == null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-arena")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }
                                    core.getArenaManager().setSpectatorSpawn(arenaName, p.getLocation());
                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.spectator-set")
                                            .replace("{arena}", arenaName));
                                    core.getArenaManager().saveArena(arenaName, p);
                                    core.getArenaManager().setupTip(arenaName, p);
                                } else {
                                    p.sendMessage(core.getLangManager().getCmdText("arena-setup.missing-name"));
                                }
                                break;

                            case "addspawn":
                                if (args.length >= 4) {
                                    String arenaName = args[3];
                                    Arena arena = core.getArenaManager().getArena(arenaName);
                                    if(arena == null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-arena")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }
                                    core.getArenaManager().addSpawn(arenaName, p.getLocation());
                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.spawn-added")
                                            .replace("{arena}", arenaName));
                                    core.getArenaManager().saveArena(arenaName, p);
                                    core.getArenaManager().setupTip(arenaName, p);
                                } else {
                                    p.sendMessage(core.getLangManager().getCmdText("arena-setup.missing-name"));
                                }
                                break;

                            case "addprotectedarea":
                                if (args.length >= 4) {
                                    String arenaName = args[3];
                                    Arena arena = core.getArenaManager().getArena(arenaName);
                                    if(arena == null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-arena")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }
                                    if(!requireSelection(p)) return true;
                                    Selection sel = getSelection(p);
                                    core.getArenaManager().addProtectedArea(arenaName, sel);
                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.protected-added")
                                            .replace("{arena}", arenaName));
                                    core.getArenaManager().saveArena(arenaName, p);
                                } else {
                                    p.sendMessage(core.getLangManager().getCmdText("arena-setup.missing-name"));
                                }
                                break;
                            case "removeprotectedarea":
                                if (args.length >= 4) {
                                    String arenaName = args[3];
                                    Arena arena = core.getArenaManager().getArena(arenaName);

                                    if (arena == null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-arena")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }

                                    Location location = p.getLocation();
                                    Selection toRemove = null;

                                    for (Selection sel : arena.getProtectedAreas()) {
                                        Location min = sel.getMinimumPoint();
                                        Location max = sel.getMaximumPoint();

                                        if (location.getWorld().equals(sel.getWorld())
                                                && location.getBlockX() >= min.getBlockX() && location.getBlockX() <= max.getBlockX()
                                                && location.getBlockY() >= min.getBlockY() && location.getBlockY() <= max.getBlockY()
                                                && location.getBlockZ() >= min.getBlockZ() && location.getBlockZ() <= max.getBlockZ()) {
                                            toRemove = sel;
                                            break;
                                        }
                                    }

                                    if (toRemove == null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.protected-not-inside")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }

                                    arena.getProtectedAreas().remove(toRemove);
                                    core.getArenaManager().saveArena(arenaName, p);

                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.protected-removed")
                                            .replace("{arena}", arenaName));
                                } else {
                                    p.sendMessage(core.getLangManager().getCmdText("arena-setup.missing-name"));
                                }
                                break;
                            case "spawnlist":
                                if (args.length >= 4) {
                                    String arenaName = args[3];
                                    Arena arena = core.getArenaManager().getArena(arenaName);

                                    if (arena == null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-arena")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }

                                    List<Location> spawns = arena.getSpawns();
                                    if (spawns.isEmpty()) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.spawns-empty")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }

                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.spawn-list-title")
                                            .replace("{arena}", arenaName));

                                    for (int id = 0; id < spawns.size(); id++) {
                                        Location location = spawns.get(id);
                                        String coords = "X:" + location.getBlockX() + " Y:" + location.getBlockY() + " Z:" + location.getBlockZ();
                                        String spawnlist = core.getLangManager().getText("")
                                                .replace("{coords}", coords)
                                                .replace("{id}", String.valueOf((id+1)));
                                        TextComponent message = new TextComponent(spawnlist);

                                        message.setClickEvent(new ClickEvent(
                                                ClickEvent.Action.RUN_COMMAND,
                                                "/tp " + p.getName() + " "
                                                        + location.getBlockX() + " "
                                                        + location.getBlockY() + " "
                                                        + location.getBlockZ()
                                        ));
                                        message.setHoverEvent(new HoverEvent(
                                                HoverEvent.Action.SHOW_TEXT,
                                                new ComponentBuilder(core.getLangManager().getText("commands.arena-setup.spawn-teleport-tip")).create()
                                        ));
                                        p.spigot().sendMessage(message);
                                    }
                                } else {
                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.missing-name"));
                                }
                                break;
                            case "removespawn":
                                if (args.length >= 4) {
                                    String arenaName = args[3];
                                    Arena arena = core.getArenaManager().getArena(arenaName);

                                    if (arena == null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-arena")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }

                                    if(args.length < 5) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-number"));
                                        return true;
                                    }

                                    int spawnId;
                                    try {
                                        spawnId = Integer.parseInt(args[4]);
                                    } catch (NumberFormatException e) {
                                        core.getLangManager().sendCmdMessage("arena-setup.invalid-number", p);
                                        return true;
                                    }

                                    List<Location> spawns = arena.getSpawns();
                                    if (spawnId < 1 || spawnId > spawns.size()) {
                                        core.getLangManager().sendCmdMessage("arena-setup.invalid-number", p);
                                        return true;
                                    }
                                    Location removed = spawns.remove(spawnId - 1);
                                    core.getArenaManager().saveArena(arenaName, p);
                                    String coordsMsg = core.getLangManager().getCmdText("arena-setup.spawn-removed")
                                            .replace("{id}", String.valueOf(spawnId))
                                            .replace("{arena}", arenaName)
                                            .replace("{coords}", "X:" + removed.getBlockX()
                                                    + " Y:" + removed.getBlockY()
                                                    + " Z:" + removed.getBlockZ()
                                                    + " World:" + removed.getWorld().getName());
                                    p.sendMessage(coordsMsg);
                                } else {
                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.missing-name"));
                                }
                                break;
                            case "kit":
                                if (args.length >= 4) {
                                    String arenaName = args[3];
                                    Arena arena = core.getArenaManager().getArena(arenaName);
                                    if(arena == null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-arena")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }
                                    core.getArenaManager().saveArenaKit(arenaName, p);
                                    core.getArenaManager().saveArena(arenaName, p);
                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.kit-saved")
                                            .replace("{arena}", arenaName));
                                } else {
                                    p.sendMessage(core.getLangManager().getCmdText("arena-setup.missing-name"));
                                }
                                break;
                            case "build":
                                if (args.length >= 4) {
                                    String arenaName = args[3];
                                    Arena arena = core.getArenaManager().getArena(arenaName);
                                    if(arena == null) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-arena")
                                                .replace("{arena}", arenaName));
                                        return true;
                                    }

                                    if(args.length < 5) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.missing-value"));
                                        return true;
                                    }

                                    String value = args[4].toLowerCase();
                                    if (!value.equals("true") && !value.equals("false")) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.invalid-value")
                                                .replace("{value}", args[4]));
                                        return true;
                                    }

                                    boolean build = Boolean.parseBoolean(value);
                                    if (arena.isBuildable() == build) {
                                        p.sendMessage(core.getLangManager()
                                                .getCmdText("arena-setup.already-value")
                                                .replace("{arena}", arenaName)
                                                .replace("{value}", String.valueOf(build)));
                                        return true;
                                    }

                                    core.getArenaManager().setBuildable(arenaName, build);
                                    core.getArenaManager().saveArena(arenaName, p);
                                    p.sendMessage(core.getLangManager()
                                            .getCmdText("arena-setup.buildable")
                                            .replace("{arena}", arenaName)
                                            .replace("{value}", args[4]));
                                } else {
                                    p.sendMessage(core.getLangManager().getCmdText("arena-setup.missing-name"));
                                }
                                break;
                            case "help":
                            default:
                                sendArenaList(p);
                                break;
                        }
                        break;
                    default:
                        sendSetupList(p);
                        break;
                }
                break;
                case "help":
                default:
                    sendHelpList(p);
                    break;
            }
        } else {
            core.getLangManager().sendCmdMessage("not-a-player", cs);
        }
        return true;
    }
}
