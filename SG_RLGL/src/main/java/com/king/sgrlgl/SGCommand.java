package com.king.sgrlgl;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class SGCommand implements CommandExecutor, TabCompleter {

    private final SG_RLGL plugin;
    private final GameManager gm;

    public SGCommand(SG_RLGL plugin, GameManager gm) {
        this.plugin = plugin;
        this.gm = gm;
    }

    private boolean mustBePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        return false;
    }

    private boolean requireOpOrAdmin(CommandSender sender) {
        if (sender.isOp()) return true;
        if (sender instanceof Player) {
            return gm.isAdmin(((Player) sender).getUniqueId());
        }
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {
        if (args.length == 0) {
            return help(sender);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help":
                return help(sender);

            case "set":
                if (!requireOpOrAdmin(sender)) {
                    sender.sendMessage(ChatColor.RED + "You must be OP or SG admin to use this.");
                    return true;
                }
                return handleSet(sender, args);

            case "remove":
                if (!requireOpOrAdmin(sender)) {
                    sender.sendMessage(ChatColor.RED + "You must be OP or SG admin to use this.");
                    return true;
                }
                return handleRemove(sender, args);

            case "game":
                if (!requireOpOrAdmin(sender)) {
                    sender.sendMessage(ChatColor.RED + "You must be OP or SG admin to use this.");
                    return true;
                }
                return handleGame(sender, args);

            case "tp":
                if (!requireOpOrAdmin(sender)) {
                    sender.sendMessage(ChatColor.RED + "You must be OP or SG admin to use this.");
                    return true;
                }
                return handleTp(sender, args);

            default:
                return help(sender);
        }
    }

    private boolean help(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "----- SG_RLGL Commands -----");
        s.sendMessage(ChatColor.YELLOW + "/sg set lobby" + ChatColor.GRAY + " - Set main game lobby");
        s.sendMessage(ChatColor.YELLOW + "/sg set guestlobby" + ChatColor.GRAY + " - Set guest lobby");
        s.sendMessage(ChatColor.YELLOW + "/sg set admin <player>" + ChatColor.GRAY + " - Add admin");
        s.sendMessage(ChatColor.YELLOW + "/sg set guest <player>" + ChatColor.GRAY + " - Add guest");
        s.sendMessage(ChatColor.YELLOW + "/sg set rules" + ChatColor.GRAY + " - Apply world rules");
        s.sendMessage(ChatColor.YELLOW + "/sg set finish" + ChatColor.GRAY + " - Get SG Hoe to set finish block");
        s.sendMessage(ChatColor.YELLOW + "/sg remove <admin|guest|winner> <player>" + ChatColor.GRAY + " - Remove role");
        s.sendMessage(ChatColor.YELLOW + "/sg game start|off" + ChatColor.GRAY + " - Start/Stop game");
        s.sendMessage(ChatColor.YELLOW + "/sg tp <guest|admin|player> <Guestlobby|Gamelobby>" + ChatColor.GRAY + " - Teleport targets");
        s.sendMessage(ChatColor.YELLOW + "/sg help" + ChatColor.GRAY + " - This help");
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg set <lobby|guestlobby|admin|guest|rules|finish> [player]");
            return true;
        }
        String what = args[1].toLowerCase(Locale.ROOT);

        switch (what) {
            case "lobby": {
                if (mustBePlayer(sender)) return true;
                Player p = (Player) sender;
                gm.setLobby(p.getLocation());
                sender.sendMessage(ChatColor.GREEN + "Lobby set to your current location.");
                return true;
            }
            case "guestlobby": {
                if (mustBePlayer(sender)) return true;
                Player p = (Player) sender;
                gm.setGuestLobby(p.getLocation());
                sender.sendMessage(ChatColor.GREEN + "Guest lobby set to your current location.");
                return true;
            }
            case "admin": {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /sg set admin <player>");
                    return true;
                }
                Player t = Bukkit.getPlayerExact(args[2]);
                if (t == null) { sender.sendMessage(ChatColor.RED + "Player not found online."); return true; }
                gm.addAdmin(t.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + t.getName() + " is now an SG admin.");
                return true;
            }
            case "guest": {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /sg set guest <player>");
                    return true;
                }
                Player t = Bukkit.getPlayerExact(args[2]);
                if (t == null) { sender.sendMessage(ChatColor.RED + "Player not found online."); return true; }
                gm.addGuest(t.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + t.getName() + " is now an SG guest.");
                return true;
            }
            case "rules": {
                World w;
                if (sender instanceof Player) w = ((Player) sender).getWorld();
                else w = Bukkit.getWorlds().get(0);
                gm.applyRules(w);
                sender.sendMessage(ChatColor.GREEN + "World rules applied.");
                return true;
            }
            case "finish": {
                if (mustBePlayer(sender)) return true;
                Player p = (Player) sender;
                InteractListener.giveFinishHoe(p);
                p.sendMessage(ChatColor.GREEN + "SG Hoe given. Right-click a block to set finish.");
                return true;
            }
            default:
                sender.sendMessage(ChatColor.RED + "Unknown set option.");
                return true;
        }
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg remove <admin|guest|winner> <player>");
            return true;
        }
        String role = args[1].toLowerCase(Locale.ROOT);
        OfflinePlayer t = Bukkit.getOfflinePlayer(args[2]);
        UUID id = t.getUniqueId();

        switch (role) {
            case "admin": gm.removeAdmin(id); break;
            case "guest": gm.removeGuest(id); break;
            case "winner": gm.removeWinner(id); break;
            default:
                sender.sendMessage(ChatColor.RED + "Role must be admin|guest|winner");
                return true;
        }
        sender.sendMessage(ChatColor.GREEN + "Removed " + t.getName() + " from " + role + ".");
        return true;
    }

    private boolean handleGame(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg game <start|off>");
            return true;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start":
                gm.startGame();
                return true;
            case "off":
                gm.stopGame();
                sender.sendMessage(ChatColor.YELLOW + "Game stopped.");
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /sg game <start|off>");
                return true;
        }
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg tp <guest|admin|player> <Guestlobby|Gamelobby>");
            return true;
        }
        String who = args[1].toLowerCase(Locale.ROOT);
        String where = args[2].toLowerCase(Locale.ROOT);

        Location targetLoc = null;
        if (where.equals("guestlobby")) targetLoc = gm.getGuestLobby();
        if (where.equals("gamelobby")) targetLoc = gm.getLobby();
        if (targetLoc == null) {
            sender.sendMessage(ChatColor.RED + "Target location not set.");
            return true;
        }

        switch (who) {
            case "guest": {
                int c = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (gm.isGuest(p.getUniqueId())) { p.teleport(targetLoc); c++; }
                }
                sender.sendMessage(ChatColor.GREEN + "Teleported " + c + " guest(s).");
                return true;
            }
            case "admin": {
                int c = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (gm.isAdmin(p.getUniqueId())) { p.teleport(targetLoc); c++; }
                }
                sender.sendMessage(ChatColor.GREEN + "Teleported " + c + " admin(s).");
                return true;
            }
            case "player": {
                if (mustBePlayer(sender)) return true;
                ((Player) sender).teleport(targetLoc);
                sender.sendMessage(ChatColor.GREEN + "Teleported.");
                return true;
            }
            default:
                sender.sendMessage(ChatColor.RED + "Who must be guest|admin|player");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("help","set","remove","game","tp").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set":
                    return Arrays.asList("lobby","guestlobby","admin","guest","rules","finish");
                case "remove":
                    return Arrays.asList("admin","guest","winner");
                case "game":
                    return Arrays.asList("start","off");
                case "tp":
                    return Arrays.asList("guest","admin","player");
            }
        }
        if (args.length == 3) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set":
                    if (args[1].equalsIgnoreCase("admin") || args[1].equalsIgnoreCase("guest")) {
                        return null;
                    }
                    break;
                case "remove":
                    return null;
                case "tp":
                    return Arrays.asList("Guestlobby","Gamelobby");
            }
        }
        return Collections.emptyList();
    }
}
