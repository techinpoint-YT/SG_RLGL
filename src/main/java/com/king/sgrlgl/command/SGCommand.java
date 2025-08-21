package com.king.sgrlgl.command;

import com.king.sgrlgl.listener.InteractListener;
import com.king.sgrlgl.manager.GameManager;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Handles all /sg commands for the Red Light Green Light game.
 * 
 * This class processes command execution and provides tab completion
 * for all game-related commands including setup, player management,
 * and game control.
 * 
 * @author King
 * @version 1.1.0
 * @since 1.0.0
 */
public class SGCommand implements CommandExecutor, TabCompleter {

    private final org.bukkit.plugin.Plugin plugin;
    private final GameManager gameManager;
    private final Logger logger;

    /**
     * Creates a new SGCommand instance.
     * 
     * @param plugin the plugin instance
     * @param gameManager the game manager
     * @throws IllegalArgumentException if any parameter is null
     */
    public SGCommand(org.bukkit.plugin.Plugin plugin, GameManager gameManager) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.gameManager = Objects.requireNonNull(gameManager, "GameManager cannot be null");
        this.logger = plugin.getLogger();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return showHelp(sender);
        }

        var subCommand = args[0].toLowerCase(Locale.ROOT);
        
        return switch (subCommand) {
            case "help" -> showHelp(sender);
            case "set" -> handleSet(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "game" -> handleGame(sender, args);
            case "tp", "teleport" -> handleTeleport(sender, args);
            case "info", "status" -> handleInfo(sender);
            default -> {
                sender.sendMessage(gameManager.getMessage("invalid-usage"));
                yield true;
            }
        };
    }

    /**
     * Shows the help message with all available commands.
     * 
     * @param sender the command sender
     * @return true to indicate the command was handled
     */
    private boolean showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "═══════ SG_RLGL Commands ═══════");
        sender.sendMessage(ChatColor.YELLOW + "/sg set lobby" + ChatColor.GRAY + " - Set main game lobby");
        sender.sendMessage(ChatColor.YELLOW + "/sg set guestlobby" + ChatColor.GRAY + " - Set guest lobby");
        sender.sendMessage(ChatColor.YELLOW + "/sg set admin <player>" + ChatColor.GRAY + " - Add admin");
        sender.sendMessage(ChatColor.YELLOW + "/sg set guest <player>" + ChatColor.GRAY + " - Add guest");
        sender.sendMessage(ChatColor.YELLOW + "/sg set rules" + ChatColor.GRAY + " - Apply world rules");
        sender.sendMessage(ChatColor.YELLOW + "/sg set finish" + ChatColor.GRAY + " - Get SG Hoe to set finish");
        sender.sendMessage(ChatColor.YELLOW + "/sg remove <admin|guest|winner> <player>" + ChatColor.GRAY + " - Remove role");
        sender.sendMessage(ChatColor.YELLOW + "/sg game <start|stop>" + ChatColor.GRAY + " - Control game state");
        sender.sendMessage(ChatColor.YELLOW + "/sg tp <guest|admin|player> <guestlobby|gamelobby>" + ChatColor.GRAY + " - Teleport");
        sender.sendMessage(ChatColor.YELLOW + "/sg info" + ChatColor.GRAY + " - Show game status");
        sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
        return true;
    }

    /**
     * Handles the 'set' subcommand for configuring game elements.
     */
    private boolean handleSet(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "sgrlgl.admin")) {
            sender.sendMessage(gameManager.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg set <lobby|guestlobby|admin|guest|rules|finish> [player]");
            return true;
        }

        var setType = args[1].toLowerCase(Locale.ROOT);
        
        return switch (setType) {
            case "lobby" -> setLobby(sender);
            case "guestlobby" -> setGuestLobby(sender);
            case "admin" -> setAdmin(sender, args);
            case "guest" -> setGuest(sender, args);
            case "rules" -> setRules(sender);
            case "finish" -> setFinish(sender);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown set option: " + setType);
                yield true;
            }
        };
    }

    private boolean setLobby(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        gameManager.setLobby(player.getLocation());
        sender.sendMessage(gameManager.getMessage("location-set"));
        logger.info("Lobby set by " + player.getName() + " at " + formatLocation(player.getLocation()));
        return true;
    }

    private boolean setGuestLobby(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        gameManager.setGuestLobby(player.getLocation());
        sender.sendMessage(gameManager.getMessage("location-set"));
        logger.info("Guest lobby set by " + player.getName() + " at " + formatLocation(player.getLocation()));
        return true;
    }

    private boolean setAdmin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg set admin <player>");
            return true;
        }

        var targetPlayer = Bukkit.getPlayerExact(args[2]);
        if (targetPlayer == null) {
            sender.sendMessage(gameManager.getMessage("player-not-found"));
            return true;
        }

        gameManager.addAdmin(targetPlayer.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " is now an SG admin.");
        targetPlayer.sendMessage(ChatColor.GREEN + "You have been made an SG admin!");
        logger.info(sender.getName() + " made " + targetPlayer.getName() + " an SG admin");
        return true;
    }

    private boolean setGuest(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg set guest <player>");
            return true;
        }

        var targetPlayer = Bukkit.getPlayerExact(args[2]);
        if (targetPlayer == null) {
            sender.sendMessage(gameManager.getMessage("player-not-found"));
            return true;
        }

        gameManager.addGuest(targetPlayer.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " is now an SG guest.");
        targetPlayer.sendMessage(ChatColor.GREEN + "You have been made an SG guest!");
        logger.info(sender.getName() + " made " + targetPlayer.getName() + " an SG guest");
        return true;
    }

    private boolean setRules(CommandSender sender) {
        World world;
        if (sender instanceof Player player) {
            world = player.getWorld();
        } else {
            world = Bukkit.getWorlds().get(0);
        }

        gameManager.applyRules(world);
        sender.sendMessage(ChatColor.GREEN + "World rules applied to " + world.getName() + ".");
        return true;
    }

    private boolean setFinish(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        InteractListener.giveFinishHoe(player);
        player.sendMessage(ChatColor.GREEN + "SG Hoe given. Right-click a block to set the finish line.");
        return true;
    }

    /**
     * Handles the 'remove' subcommand for removing player roles.
     */
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "sgrlgl.admin")) {
            sender.sendMessage(gameManager.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg remove <admin|guest|winner> <player>");
            return true;
        }

        var role = args[1].toLowerCase(Locale.ROOT);
        var targetPlayer = Bukkit.getOfflinePlayer(args[2]);
        var playerId = targetPlayer.getUniqueId();

        switch (role) {
            case "admin" -> {
                gameManager.removeAdmin(playerId);
                sender.sendMessage(ChatColor.GREEN + "Removed " + targetPlayer.getName() + " from admin role.");
            }
            case "guest" -> {
                gameManager.removeGuest(playerId);
                sender.sendMessage(ChatColor.GREEN + "Removed " + targetPlayer.getName() + " from guest role.");
            }
            case "winner" -> {
                gameManager.removeWinner(playerId);
                sender.sendMessage(ChatColor.GREEN + "Removed " + targetPlayer.getName() + " from winner role.");
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Role must be admin, guest, or winner");
                return true;
            }
        }

        logger.info(sender.getName() + " removed " + targetPlayer.getName() + " from " + role + " role");
        return true;
    }

    /**
     * Handles the 'game' subcommand for controlling game state.
     */
    private boolean handleGame(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "sgrlgl.admin")) {
            sender.sendMessage(gameManager.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg game <start|stop>");
            return true;
        }

        var gameAction = args[1].toLowerCase(Locale.ROOT);
        
        return switch (gameAction) {
            case "start" -> {
                gameManager.startGame();
                sender.sendMessage(ChatColor.GREEN + "Game started successfully!");
                yield true;
            }
            case "stop", "off" -> {
                gameManager.stopGame();
                sender.sendMessage(gameManager.getMessage("game-stopped"));
                yield true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: /sg game <start|stop>");
                yield true;
            }
        };
    }

    /**
     * Handles the 'tp' subcommand for teleporting players.
     */
    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "sgrlgl.admin")) {
            sender.sendMessage(gameManager.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg tp <guest|admin|player> <guestlobby|gamelobby>");
            return true;
        }

        var who = args[1].toLowerCase(Locale.ROOT);
        var where = args[2].toLowerCase(Locale.ROOT);

        Optional<Location> targetLocation = switch (where) {
            case "guestlobby" -> gameManager.getGuestLobby();
            case "gamelobby", "lobby" -> gameManager.getLobby();
            default -> Optional.empty();
        };

        if (targetLocation.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Target location '" + where + "' is not set.");
            return true;
        }

        var location = targetLocation.get();
        
        return switch (who) {
            case "guest" -> teleportRole(sender, gameManager.getGuests(), location, "guests");
            case "admin" -> teleportRole(sender, gameManager.getAdmins(), location, "admins");
            case "player" -> teleportSelf(sender, location);
            default -> {
                sender.sendMessage(ChatColor.RED + "Who must be guest, admin, or player");
                yield true;
            }
        };
    }

    private boolean teleportRole(CommandSender sender, Set<UUID> playerIds, Location location, String roleName) {
        var count = 0;
        for (var playerId : playerIds) {
            var player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.teleport(location);
                count++;
            }
        }
        sender.sendMessage(ChatColor.GREEN + "Teleported " + count + " " + roleName + ".");
        return true;
    }

    private boolean teleportSelf(CommandSender sender, Location location) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        player.teleport(location);
        player.sendMessage(ChatColor.GREEN + "Teleported successfully!");
        return true;
    }

    /**
     * Handles the 'info' subcommand for showing game status.
     */
    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "═══════ Game Status ═══════");
        sender.sendMessage(ChatColor.YELLOW + "Game Active: " + 
            (gameManager.isGameActive() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        sender.sendMessage(ChatColor.YELLOW + "Light State: " + 
            (gameManager.getState() == GameManager.LightState.GREEN ? 
                ChatColor.GREEN + "GREEN" : ChatColor.RED + "RED"));
        sender.sendMessage(ChatColor.YELLOW + "Admins: " + ChatColor.WHITE + gameManager.getAdmins().size());
        sender.sendMessage(ChatColor.YELLOW + "Guests: " + ChatColor.WHITE + gameManager.getGuests().size());
        sender.sendMessage(ChatColor.YELLOW + "Winners: " + ChatColor.WHITE + gameManager.getWinners().size());
        
        var lobby = gameManager.getLobby();
        var guestLobby = gameManager.getGuestLobby();
        var finish = gameManager.getFinish();
        
        sender.sendMessage(ChatColor.YELLOW + "Lobby: " + 
            (lobby.isPresent() ? ChatColor.GREEN + "Set" : ChatColor.RED + "Not Set"));
        sender.sendMessage(ChatColor.YELLOW + "Guest Lobby: " + 
            (guestLobby.isPresent() ? ChatColor.GREEN + "Set" : ChatColor.RED + "Not Set"));
        sender.sendMessage(ChatColor.YELLOW + "Finish: " + 
            (finish.isPresent() ? ChatColor.GREEN + "Set" : ChatColor.RED + "Not Set"));
        
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.isOp() || sender.hasPermission(permission) || 
               (sender instanceof Player player && gameManager.isAdmin(player.getUniqueId()));
    }

    private String formatLocation(Location location) {
        return String.format("%s: %.1f, %.1f, %.1f", 
            location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("help", "set", "remove", "game", "tp", "info").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set" -> Arrays.asList("lobby", "guestlobby", "admin", "guest", "rules", "finish");
                case "remove" -> Arrays.asList("admin", "guest", "winner");
                case "game" -> Arrays.asList("start", "stop");
                case "tp" -> Arrays.asList("guest", "admin", "player");
                default -> Collections.emptyList();
            };
        }
        
        if (args.length == 3) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set" -> {
                    if (args[1].equalsIgnoreCase("admin") || args[1].equalsIgnoreCase("guest")) {
                        yield Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toList());
                    }
                    yield Collections.emptyList();
                }
                case "remove" -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                case "tp" -> Arrays.asList("guestlobby", "gamelobby");
                default -> Collections.emptyList();
            };
        }
        
        return Collections.emptyList();
    }
}