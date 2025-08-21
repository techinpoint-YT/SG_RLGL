package com.king.sgrlgl;

import com.king.sgrlgl.command.SGCommand;
import com.king.sgrlgl.listener.InteractListener;
import com.king.sgrlgl.listener.MovementListener;
import com.king.sgrlgl.manager.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Main plugin class for SG_RLGL (Squid Game Red Light Green Light).
 * 
 * This plugin implements a Red Light Green Light game for Minecraft servers,
 * where players must stop moving when "Red Light" is called or face consequences.
 * 
 * @author King
 * @version 1.1.0
 * @since 1.0.0
 */
public final class SG_RLGL extends JavaPlugin {

    private static SG_RLGL instance;
    private GameManager gameManager;

    /**
     * Gets the plugin instance.
     * 
     * @return the plugin instance
     * @throws IllegalStateException if the plugin is not enabled
     */
    public static SG_RLGL getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Plugin is not enabled");
        }
        return instance;
    }

    /**
     * Gets the game manager instance.
     * 
     * @return the game manager
     * @throws IllegalStateException if the plugin is not properly initialized
     */
    public GameManager getGameManager() {
        if (gameManager == null) {
            throw new IllegalStateException("GameManager is not initialized");
        }
        return gameManager;
    }

    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default configuration
            saveDefaultConfig();
            
            // Initialize game manager
            this.gameManager = new GameManager(this);
            
            // Register command
            var sgCommand = new SGCommand(this, gameManager);
            var command = getCommand("sg");
            if (command != null) {
                command.setExecutor(sgCommand);
                command.setTabCompleter(sgCommand);
            } else {
                getLogger().severe("Failed to register /sg command - command not found in plugin.yml");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Register event listeners
            var pluginManager = Bukkit.getPluginManager();
            pluginManager.registerEvents(new MovementListener(gameManager), this);
            pluginManager.registerEvents(new InteractListener(gameManager), this);
            
            getLogger().info("SG_RLGL v" + getDescription().getVersion() + " has been enabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable SG_RLGL plugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (gameManager != null) {
                gameManager.saveAll();
                getLogger().info("Game data saved successfully.");
            }
            
            getLogger().info("SG_RLGL has been disabled.");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error occurred while disabling plugin", e);
        } finally {
            instance = null;
        }
    }
}