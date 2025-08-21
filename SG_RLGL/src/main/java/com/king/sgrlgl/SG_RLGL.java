package com.king.sgrlgl;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SG_RLGL extends JavaPlugin {

    private static SG_RLGL instance;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.gameManager = new GameManager(this);

        SGCommand sgCommand = new SGCommand(this, gameManager);
        getCommand("sg").setExecutor(sgCommand);
        getCommand("sg").setTabCompleter(sgCommand);

        Bukkit.getPluginManager().registerEvents(new MovementListener(gameManager), this);
        Bukkit.getPluginManager().registerEvents(new InteractListener(gameManager), this);

        getLogger().info("SG_RLGL enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SG_RLGL disabled.");
        gameManager.saveAll();
    }

    public static SG_RLGL getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
}
