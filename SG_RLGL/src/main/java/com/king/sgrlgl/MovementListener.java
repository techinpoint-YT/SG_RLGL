package com.king.sgrlgl;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class MovementListener implements Listener {

    private final GameManager gm;

    public MovementListener(GameManager gm) {
        this.gm = gm;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        // Finish detection
        Location finish = gm.getFinish();
        if (finish != null && !gm.isWinner(id)) {
            Location to = e.getTo();
            if (GameManager.sameBlock(to, finish)) {
                gm.addWinner(id);
                p.sendMessage("§aYou reached the finish! You're safe.");
            }
        }

        // Red light enforcement
        if (!gm.isGameActive() || gm.getState() != GameManager.LightState.RED) return;
        if (gm.isImmune(id)) return;

        var from = e.getFrom();
        var to = e.getTo();
        if (to == null) return;

        boolean moved = (from.getX() != to.getX()) || (from.getY() != to.getY()) || (from.getZ() != to.getZ())
                || (from.getPitch() != to.getPitch()) || (from.getYaw() != to.getYaw());

        if (moved) {
            World w = p.getWorld();
            w.strikeLightning(p.getLocation());
            String reason = "Moved during Red Light";
            Bukkit.getBanList(BanList.Type.NAME).addBan(p.getName(), reason, null, "SG_RLGL");
            p.kickPlayer("§c" + reason);
        }
    }
}
