package com.king.sgrlgl.listener;

import com.king.sgrlgl.manager.GameManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Handles player movement during the Red Light Green Light game.
 * 
 * This listener monitors player movement and enforces the red light rule,
 * where players must stop moving when the light is red or face consequences.
 * It also detects when players reach the finish line.
 * 
 * @author King
 * @version 1.1.0
 * @since 1.0.0
 */
public class MovementListener implements Listener {

    private final GameManager gameManager;
    private final Logger logger;

    /**
     * Creates a new MovementListener instance.
     * 
     * @param gameManager the game manager
     * @throws IllegalArgumentException if gameManager is null
     */
    public MovementListener(GameManager gameManager) {
        this.gameManager = Objects.requireNonNull(gameManager, "GameManager cannot be null");
        this.logger = com.king.sgrlgl.SG_RLGL.getInstance().getLogger();
    }

    /**
     * Handles player movement events.
     * 
     * This method checks for finish line detection and red light violations.
     * It uses HIGH priority to ensure it runs after other plugins but before
     * any plugins that might cancel the event.
     * 
     * @param event the player move event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
        var playerId = player.getUniqueId();
        var to = event.getTo();
        
        if (to == null) {
            return;
        }

        // Check for finish line detection
        checkFinishLine(player, playerId, to);

        // Check for red light violations
        checkRedLightViolation(event, player, playerId);
    }

    /**
     * Checks if the player has reached the finish line.
     */
    private void checkFinishLine(Player player, java.util.UUID playerId, Location to) {
        // Skip if player is already a winner
        if (gameManager.isWinner(playerId)) {
            return;
        }

        var finishLocation = gameManager.getFinish();
        if (finishLocation.isEmpty()) {
            return;
        }

        if (GameManager.sameBlock(to, finishLocation.get())) {
            gameManager.addWinner(playerId);
            player.sendMessage(gameManager.getMessage("winner"));
            logger.info("Player " + player.getName() + " reached the finish line!");
            
            // Broadcast to other players
            var message = "§e" + player.getName() + " §areached the finish line!";
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(player))
                    .forEach(p -> p.sendMessage(message));
        }
    }

    /**
     * Checks for red light violations and applies consequences.
     */
    private void checkRedLightViolation(PlayerMoveEvent event, Player player, java.util.UUID playerId) {
        // Only check during active games with red light
        if (!gameManager.isGameActive() || gameManager.getState() != GameManager.LightState.RED) {
            return;
        }

        // Skip immune players (admins, guests, winners)
        if (gameManager.isImmune(playerId)) {
            return;
        }

        var from = event.getFrom();
        var to = event.getTo();
        
        if (to == null) {
            return;
        }

        // Check if player actually moved
        if (hasPlayerMoved(from, to)) {
            handleRedLightViolation(player);
        }
    }

    /**
     * Determines if a player has actually moved between two locations.
     * 
     * This method checks position and rotation changes to detect any movement.
     * 
     * @param from the starting location
     * @param to the ending location
     * @return true if the player moved
     */
    private boolean hasPlayerMoved(Location from, Location to) {
        // Check position changes
        boolean positionChanged = from.getX() != to.getX() || 
                                 from.getY() != to.getY() || 
                                 from.getZ() != to.getZ();
        
        // Check rotation changes
        boolean rotationChanged = from.getPitch() != to.getPitch() || 
                                 from.getYaw() != to.getYaw();
        
        return positionChanged || rotationChanged;
    }

    /**
     * Handles a red light violation by applying consequences to the player.
     * 
     * @param player the player who violated the red light rule
     */
    private void handleRedLightViolation(Player player) {
        var plugin = com.king.sgrlgl.SG_RLGL.getInstance();
        var config = plugin.getConfig();
        
        logger.info("Red light violation by " + player.getName());
        
        // Strike lightning if enabled
        if (config.getBoolean("settings.lightning-on-violation", true)) {
            var world = player.getWorld();
            world.strikeLightning(player.getLocation());
        }
        
        // Ban player if enabled
        if (config.getBoolean("settings.ban-on-violation", true)) {
            var reason = "Moved during Red Light";
            var banList = Bukkit.getBanList(BanList.Type.NAME);
            banList.addBan(player.getName(), reason, null, "SG_RLGL");
            
            var kickMessage = gameManager.getMessage("banned");
            player.kickPlayer(kickMessage);
            
            logger.info("Player " + player.getName() + " was banned for moving during red light");
            
            // Broadcast violation to other players
            var broadcastMessage = "§c" + player.getName() + " §ewas eliminated for moving during Red Light!";
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(player))
                    .forEach(p -> p.sendMessage(broadcastMessage));
        }
    }
}