package com.king.sgrlgl;

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

public class InteractListener implements Listener {

    private final GameManager gm;

    private static final String HOE_NAME = ChatColor.GOLD + "SG Finish Setter";
    private static NamespacedKey HOE_KEY;
    private static NamespacedKey DYE_KEY;

    public InteractListener(GameManager gm) {
        this.gm = gm;
        HOE_KEY = new NamespacedKey(SG_RLGL.getInstance(), "sg_finish_hoe");
        DYE_KEY = new NamespacedKey(SG_RLGL.getInstance(), "sg_admin_dye");
    }

    public static void giveFinishHoe(Player p) {
        ItemStack item = new ItemStack(Material.NETHERITE_HOE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(HOE_NAME);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(HOE_KEY, PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        p.getInventory().addItem(item);
    }

    public static void giveAdminDyes(Player p) {
        ItemStack green = new ItemStack(Material.GREEN_DYE, 1);
        ItemMeta gmMeta = green.getItemMeta();
        gmMeta.setDisplayName(ChatColor.GREEN + "Green Light");
        gmMeta.getPersistentDataContainer().set(DYE_KEY, PersistentDataType.STRING, "GREEN");
        green.setItemMeta(gmMeta);

        ItemStack red = new ItemStack(Material.RED_DYE, 1);
        ItemMeta rdMeta = red.getItemMeta();
        rdMeta.setDisplayName(ChatColor.RED + "Red Light");
        rdMeta.getPersistentDataContainer().set(DYE_KEY, PersistentDataType.STRING, "RED");
        red.setItemMeta(rdMeta);

        p.getInventory().addItem(green, red);
    }

    public static void removeAdminDyes(Player p) {
        p.getInventory().remove(Material.GREEN_DYE);
        p.getInventory().remove(Material.RED_DYE);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        if (item.getType() == Material.NETHERITE_HOE &&
            item.getItemMeta().getPersistentDataContainer().has(HOE_KEY, PersistentDataType.BYTE)) {

            if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
                e.setCancelled(true);
                var bLoc = e.getClickedBlock().getLocation().add(0.5, 0, 0.5);
                bLoc.setYaw(0f); bLoc.setPitch(0f);
                gm.setFinish(bLoc);
                p.sendMessage(ChatColor.GREEN + "Finish location set at " +
                        bLoc.getBlockX() + ", " + bLoc.getBlockY() + ", " + bLoc.getBlockZ() +
                        " in world " + bLoc.getWorld().getName() + ".");
            }
            return;
        }

        if (item.getItemMeta().getPersistentDataContainer().has(DYE_KEY, PersistentDataType.STRING)) {
            String val = item.getItemMeta().getPersistentDataContainer().get(DYE_KEY, PersistentDataType.STRING);
            if (!gm.isGameActive()) {
                p.sendMessage(ChatColor.RED + "Game is not active. Use /sg game start.");
                return;
            }
            if (!gm.isAdmin(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "Only SG admins can use the dyes.");
                return;
            }
            if (val != null) {
                if (val.equalsIgnoreCase("GREEN")) {
                    gm.setGreen();
                } else if (val.equalsIgnoreCase("RED")) {
                    gm.setRed();
                }
            }
            e.setCancelled(true);
        }
    }
}
