package com.king.sgrlgl.listener;

import com.king.sgrlgl.manager.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Handles player interactions with special game items.
 * 
 * This listener manages interactions with the finish setter hoe and
 * admin control dyes for changing the light state during the game.
 * 
 * @author King
 * @version 1.1.0
 * @since 1.0.0
 */
public class InteractListener implements Listener {

    private final GameManager gameManager;
    private final Logger logger;

    // Item identification keys
    private static NamespacedKey hoeKey;
    private static NamespacedKey dyeKey;

    // Item display names
    private static final String HOE_NAME = ChatColor.GOLD + "SG Finish Setter";
    private static final String GREEN_DYE_NAME = ChatColor.GREEN + "Green Light";
    private static final String RED_DYE_NAME = ChatColor.RED + "Red Light";

    /**
     * Creates a new InteractListener instance.
     * 
     * @param gameManager the game manager
     * @throws IllegalArgumentException if gameManager is null
     */
    public InteractListener(GameManager gameManager) {
        this.gameManager = Objects.requireNonNull(gameManager, "GameManager cannot be null");
        this.logger = com.king.sgrlgl.SG_RLGL.getInstance().getLogger();
        
        // Initialize namespaced keys
        var plugin = com.king.sgrlgl.SG_RLGL.getInstance();
        hoeKey = new NamespacedKey(plugin, "sg_finish_hoe");
        dyeKey = new NamespacedKey(plugin, "sg_admin_dye");
    }

    /**
     * Gives a finish setter hoe to the specified player.
     * 
     * @param player the player to give the hoe to
     * @throws IllegalArgumentException if player is null
     */
    public static void giveFinishHoe(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        
        var hoe = new ItemStack(Material.NETHERITE_HOE, 1);
        var meta = hoe.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(HOE_NAME);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(hoeKey, PersistentDataType.BYTE, (byte) 1);
            hoe.setItemMeta(meta);
        }
        
        player.getInventory().addItem(hoe);
    }

    /**
     * Gives admin control dyes to the specified player.
     * 
     * @param player the player to give the dyes to
     * @throws IllegalArgumentException if player is null
     */
    public static void giveAdminDyes(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        
        // Create green dye
        var greenDye = createControlDye(Material.GREEN_DYE, GREEN_DYE_NAME, "GREEN");
        
        // Create red dye
        var redDye = createControlDye(Material.RED_DYE, RED_DYE_NAME, "RED");
        
        player.getInventory().addItem(greenDye, redDye);
    }

    /**
     * Creates a control dye item with the specified properties.
     */
    private static ItemStack createControlDye(Material material, String displayName, String dyeType) {
        var dye = new ItemStack(material, 1);
        var meta = dye.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.getPersistentDataContainer().set(dyeKey, PersistentDataType.STRING, dyeType);
            dye.setItemMeta(meta);
        }
        
        return dye;
    }

    /**
     * Removes admin control dyes from the specified player.
     * 
     * @param player the player to remove dyes from
     * @throws IllegalArgumentException if player is null
     */
    public static void removeAdminDyes(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        
        var inventory = player.getInventory();
        
        // Remove all green and red dyes (this will remove both regular and admin dyes)
        inventory.remove(Material.GREEN_DYE);
        inventory.remove(Material.RED_DYE);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle main hand interactions
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        var player = event.getPlayer();
        var item = player.getInventory().getItemInMainHand();
        
        if (item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }

        var meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        var dataContainer = meta.getPersistentDataContainer();

        // Handle finish setter hoe
        if (isFinishHoe(item, dataContainer)) {
            handleFinishHoeInteraction(event, player);
            return;
        }

        // Handle admin control dyes
        if (isAdminDye(dataContainer)) {
            handleAdminDyeInteraction(event, player, dataContainer);
        }
    }

    /**
     * Checks if the item is a finish setter hoe.
     */
    private boolean isFinishHoe(ItemStack item, org.bukkit.persistence.PersistentDataContainer dataContainer) {
        return item.getType() == Material.NETHERITE_HOE && 
               dataContainer.has(hoeKey, PersistentDataType.BYTE);
    }

    /**
     * Checks if the item is an admin control dye.
     */
    private boolean isAdminDye(org.bukkit.persistence.PersistentDataContainer dataContainer) {
        return dataContainer.has(dyeKey, PersistentDataType.STRING);
    }

    /**
     * Handles interaction with the finish setter hoe.
     */
    private void handleFinishHoeInteraction(PlayerInteractEvent event, Player player) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        event.setCancelled(true);
        
        var blockLocation = event.getClickedBlock().getLocation().add(0.5, 0, 0.5);
        blockLocation.setYaw(0f);
        blockLocation.setPitch(0f);
        
        gameManager.setFinish(blockLocation);
        
        var message = String.format("%sFinish location set at %d, %d, %d in world %s.",
            ChatColor.GREEN,
            blockLocation.getBlockX(),
            blockLocation.getBlockY(),
            blockLocation.getBlockZ(),
            blockLocation.getWorld().getName()
        );
        
        player.sendMessage(message);
        logger.info("Finish location set by " + player.getName() + " at " + 
                   blockLocation.getBlockX() + ", " + blockLocation.getBlockY() + ", " + 
                   blockLocation.getBlockZ() + " in " + blockLocation.getWorld().getName());
    }

    /**
     * Handles interaction with admin control dyes.
     */
    private void handleAdminDyeInteraction(PlayerInteractEvent event, Player player, 
                                         org.bukkit.persistence.PersistentDataContainer dataContainer) {
        event.setCancelled(true);
        
        if (!gameManager.isGameActive()) {
            player.sendMessage(gameManager.getMessage("game-not-active"));
            return;
        }

        if (!gameManager.isAdmin(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only SG admins can control the lights.");
            return;
        }

        var dyeType = dataContainer.get(dyeKey, PersistentDataType.STRING);
        if (dyeType == null) {
            return;
        }

        switch (dyeType.toUpperCase()) {
            case "GREEN" -> {
                gameManager.setGreen();
                logger.info("Light set to GREEN by " + player.getName());
            }
            case "RED" -> {
                gameManager.setRed();
                logger.info("Light set to RED by " + player.getName());
            }
            default -> logger.warning("Unknown dye type: " + dyeType);
        }
    }
}