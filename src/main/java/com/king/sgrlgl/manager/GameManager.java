package com.king.sgrlgl.manager;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages the Red Light Green Light game state, player roles, and locations.
 * 
 * This class handles all game logic including player management, location storage,
 * game state transitions, and configuration persistence.
 * 
 * @author King
 * @version 1.1.0
 * @since 1.0.0
 */
public class GameManager {

    /**
     * Represents the current light state in the game.
     */
    public enum LightState { 
        /** Players can move freely */
        GREEN, 
        /** Players must stop moving or face consequences */
        RED 
    }

    /**
     * Represents a game location with world and coordinates.
     */
    public record GameLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        
        /**
         * Converts this GameLocation to a Bukkit Location.
         * 
         * @return the Bukkit Location, or null if the world doesn't exist
         */
        public Location toBukkitLocation() {
            var world = Bukkit.getWorld(worldName);
            return world != null ? new Location(world, x, y, z, yaw, pitch) : null;
        }
        
        /**
         * Creates a GameLocation from a Bukkit Location.
         * 
         * @param location the Bukkit Location
         * @return the GameLocation
         * @throws IllegalArgumentException if location or world is null
         */
        public static GameLocation fromBukkitLocation(Location location) {
            Objects.requireNonNull(location, "Location cannot be null");
            Objects.requireNonNull(location.getWorld(), "Location world cannot be null");
            
            return new GameLocation(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
            );
        }
    }

    private final org.bukkit.plugin.Plugin plugin;
    private final Logger logger;

    // Game locations
    private GameLocation lobby;
    private GameLocation guestLobby;
    private GameLocation finish;

    // Game state
    private volatile boolean gameActive;
    private volatile LightState state;

    // Player roles - using ConcurrentHashMap for thread safety
    private final Set<UUID> admins = ConcurrentHashMap.newKeySet();
    private final Set<UUID> guests = ConcurrentHashMap.newKeySet();
    private final Set<UUID> winners = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new GameManager instance.
     * 
     * @param plugin the plugin instance
     * @throws IllegalArgumentException if plugin is null
     */
    public GameManager(org.bukkit.plugin.Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.logger = plugin.getLogger();
        loadAll();
    }

    /**
     * Loads all game data from the configuration file.
     */
    public void loadAll() {
        try {
            var config = plugin.getConfig();
            
            // Load locations
            lobby = readLocation("locations.lobby");
            guestLobby = readLocation("locations.guestLobby");
            finish = readLocation("locations.finish");

            // Load player roles
            admins.clear();
            admins.addAll(readUUIDList("roles.admins"));
            
            guests.clear();
            guests.addAll(readUUIDList("roles.guests"));
            
            winners.clear();
            winners.addAll(readUUIDList("roles.winners"));

            // Load game state
            gameActive = config.getBoolean("game.active", false);
            try {
                state = LightState.valueOf(config.getString("game.state", "GREEN"));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid light state in config, defaulting to GREEN");
                state = LightState.GREEN;
            }
            
            logger.info("Game data loaded successfully");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load game data", e);
        }
    }

    /**
     * Saves all game data to the configuration file.
     */
    public void saveAll() {
        try {
            var config = plugin.getConfig();
            
            // Save locations
            writeLocation("locations.lobby", lobby);
            writeLocation("locations.guestLobby", guestLobby);
            writeLocation("locations.finish", finish);

            // Save player roles
            writeUUIDList("roles.admins", admins);
            writeUUIDList("roles.guests", guests);
            writeUUIDList("roles.winners", winners);

            // Save game state
            config.set("game.active", gameActive);
            config.set("game.state", state.name());
            
            plugin.saveConfig();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save game data", e);
        }
    }

    private GameLocation readLocation(String path) {
        var config = plugin.getConfig();
        var section = config.getConfigurationSection(path);
        
        if (section == null || !section.isSet("world")) {
            return null;
        }
        
        try {
            return new GameLocation(
                section.getString("world"),
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw", 0.0),
                (float) section.getDouble("pitch", 0.0)
            );
        } catch (Exception e) {
            logger.warning("Failed to read location from path: " + path);
            return null;
        }
    }

    private void writeLocation(String path, GameLocation location) {
        var config = plugin.getConfig();
        
        if (location == null) {
            config.set(path, null);
            return;
        }
        
        config.set(path + ".world", location.worldName());
        config.set(path + ".x", location.x());
        config.set(path + ".y", location.y());
        config.set(path + ".z", location.z());
        config.set(path + ".yaw", location.yaw());
        config.set(path + ".pitch", location.pitch());
    }

    private List<UUID> readUUIDList(String path) {
        return plugin.getConfig().getStringList(path).stream()
                .map(this::parseUUID)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private UUID parseUUID(String uuidString) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid UUID in config: " + uuidString);
            return null;
        }
    }

    private void writeUUIDList(String path, Collection<UUID> uuids) {
        var uuidStrings = uuids.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        plugin.getConfig().set(path, uuidStrings);
    }

    /**
     * Gets a formatted message from the configuration.
     * 
     * @param key the message key
     * @return the formatted message
     */
    public String getMessage(String key) {
        var config = plugin.getConfig();
        var prefix = config.getString("messages.prefix", "");
        var message = config.getString("messages." + key, "");
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    /**
     * Broadcasts the current light state to all players.
     */
    public void broadcastLight() {
        if (!plugin.getConfig().getBoolean("settings.broadcast-messages", true)) {
            return;
        }
        
        var titleColor = state == LightState.GREEN ? ChatColor.GREEN : ChatColor.RED;
        var title = titleColor + (state == LightState.GREEN ? "Green Light" : "Red Light");
        var messageKey = state == LightState.GREEN ? "green" : "red";
        
        var fadeIn = plugin.getConfig().getInt("settings.title-fade-duration", 5);
        var stay = plugin.getConfig().getInt("settings.title-display-duration", 40);
        var fadeOut = plugin.getConfig().getInt("settings.title-fade-duration", 5);
        
        for (var player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, "", fadeIn, stay, fadeOut);
        }
        
        Bukkit.broadcastMessage(getMessage(messageKey));
    }

    /**
     * Starts the Red Light Green Light game.
     */
    public void startGame() {
        gameActive = true;
        state = LightState.GREEN;
        
        Bukkit.broadcastMessage(getMessage("start"));
        broadcastLight();

        // Give admin dyes to all admins
        for (var adminId : admins) {
            var player = Bukkit.getPlayer(adminId);
            if (player != null && player.isOnline()) {
                com.king.sgrlgl.listener.InteractListener.giveAdminDyes(player);
            }
        }
        
        saveAll();
        logger.info("Red Light Green Light game started");
    }

    /**
     * Stops the Red Light Green Light game.
     */
    public void stopGame() {
        gameActive = false;
        
        // Remove admin dyes from all admins
        for (var adminId : admins) {
            var player = Bukkit.getPlayer(adminId);
            if (player != null && player.isOnline()) {
                com.king.sgrlgl.listener.InteractListener.removeAdminDyes(player);
            }
        }
        
        saveAll();
        logger.info("Red Light Green Light game stopped");
    }

    /**
     * Sets the light state to GREEN and broadcasts it.
     */
    public void setGreen() {
        state = LightState.GREEN;
        saveAll();
        broadcastLight();
    }

    /**
     * Sets the light state to RED and broadcasts it.
     */
    public void setRed() {
        state = LightState.RED;
        saveAll();
        broadcastLight();
    }

    /**
     * Applies game rules to the specified world.
     * 
     * @param world the world to apply rules to
     */
    public void applyRules(World world) {
        if (world == null) {
            return;
        }
        
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setStorm(false);
        world.setThundering(false);
        world.setClearWeatherDuration(20 * 60 * 10); // 10 minutes
        
        logger.info("Applied game rules to world: " + world.getName());
    }

    /**
     * Checks if two locations are in the same block.
     * 
     * @param a the first location
     * @param b the second location
     * @return true if both locations are in the same block
     */
    public static boolean sameBlock(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) {
            return false;
        }
        
        if (!a.getWorld().equals(b.getWorld())) {
            return false;
        }
        
        return a.getBlockX() == b.getBlockX()
            && a.getBlockY() == b.getBlockY()
            && a.getBlockZ() == b.getBlockZ();
    }

    // Getters and setters
    public boolean isGameActive() { return gameActive; }
    public LightState getState() { return state; }

    public Optional<Location> getLobby() { 
        return Optional.ofNullable(lobby).map(GameLocation::toBukkitLocation); 
    }
    
    public void setLobby(Location location) { 
        this.lobby = location != null ? GameLocation.fromBukkitLocation(location) : null; 
        saveAll(); 
    }

    public Optional<Location> getGuestLobby() { 
        return Optional.ofNullable(guestLobby).map(GameLocation::toBukkitLocation); 
    }
    
    public void setGuestLobby(Location location) { 
        this.guestLobby = location != null ? GameLocation.fromBukkitLocation(location) : null; 
        saveAll(); 
    }

    public Optional<Location> getFinish() { 
        return Optional.ofNullable(finish).map(GameLocation::toBukkitLocation); 
    }
    
    public void setFinish(Location location) { 
        this.finish = location != null ? GameLocation.fromBukkitLocation(location) : null; 
        saveAll(); 
    }

    // Role management
    public boolean isAdmin(UUID playerId) { return admins.contains(playerId); }
    public boolean isGuest(UUID playerId) { return guests.contains(playerId); }
    public boolean isWinner(UUID playerId) { return winners.contains(playerId); }
    public boolean isImmune(UUID playerId) { 
        return isAdmin(playerId) || isGuest(playerId) || isWinner(playerId); 
    }

    public void addAdmin(UUID playerId) { admins.add(playerId); saveAll(); }
    public void addGuest(UUID playerId) { guests.add(playerId); saveAll(); }
    public void addWinner(UUID playerId) { winners.add(playerId); saveAll(); }

    public void removeAdmin(UUID playerId) { admins.remove(playerId); saveAll(); }
    public void removeGuest(UUID playerId) { guests.remove(playerId); saveAll(); }
    public void removeWinner(UUID playerId) { winners.remove(playerId); saveAll(); }

    // Read-only collections
    public Set<UUID> getAdmins() { return Set.copyOf(admins); }
    public Set<UUID> getGuests() { return Set.copyOf(guests); }
    public Set<UUID> getWinners() { return Set.copyOf(winners); }
}