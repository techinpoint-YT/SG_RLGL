package com.king.sgrlgl;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class GameManager {

    public enum LightState { GREEN, RED }

    private final SG_RLGL plugin;

    private Location lobby;
    private Location guestLobby;
    private Location finish;

    private boolean gameActive;
    private LightState state;

    private final Set<UUID> admins = new HashSet<>();
    private final Set<UUID> guests = new HashSet<>();
    private final Set<UUID> winners = new HashSet<>();

    public GameManager(SG_RLGL plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public void loadAll() {
        lobby = readLocation("locations.lobby");
        guestLobby = readLocation("locations.guestLobby");
        finish = readLocation("locations.finish");

        admins.clear(); admins.addAll(readUUIDList("roles.admins"));
        guests.clear(); guests.addAll(readUUIDList("roles.guests"));
        winners.clear(); winners.addAll(readUUIDList("roles.winners"));

        gameActive = plugin.getConfig().getBoolean("game.active", false);
        try {
            state = LightState.valueOf(plugin.getConfig().getString("game.state", "GREEN"));
        } catch (IllegalArgumentException e) {
            state = LightState.GREEN;
        }
    }

    public void saveAll() {
        writeLocation("locations.lobby", lobby);
        writeLocation("locations.guestLobby", guestLobby);
        writeLocation("locations.finish", finish);

        writeUUIDList("roles.admins", admins);
        writeUUIDList("roles.guests", guests);
        writeUUIDList("roles.winners", winners);

        plugin.getConfig().set("game.active", gameActive);
        plugin.getConfig().set("game.state", state.name());
        plugin.saveConfig();
    }

    private Location readLocation(String path) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null || !sec.isSet("world")) return null;
        World w = Bukkit.getWorld(sec.getString("world"));
        if (w == null) return null;
        double x = sec.getDouble("x");
        double y = sec.getDouble("y");
        double z = sec.getDouble("z");
        float yaw = (float) sec.getDouble("yaw", 0.0);
        float pitch = (float) sec.getDouble("pitch", 0.0);
        return new Location(w, x, y, z, yaw, pitch);
    }

    private void writeLocation(String path, Location loc) {
        if (loc == null) { plugin.getConfig().set(path, null); return; }
        plugin.getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getConfig().set(path + ".x", loc.getX());
        plugin.getConfig().set(path + ".y", loc.getY());
        plugin.getConfig().set(path + ".z", loc.getZ());
        plugin.getConfig().set(path + ".yaw", loc.getYaw());
        plugin.getConfig().set(path + ".pitch", loc.getPitch());
    }

    private List<UUID> readUUIDList(String path) {
        return plugin.getConfig().getStringList(path).stream()
                .map(s -> {
                    try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void writeUUIDList(String path, Collection<UUID> uuids) {
        List<String> list = uuids.stream().map(UUID::toString).collect(Collectors.toList());
        plugin.getConfig().set(path, list);
    }

    public String msg(String key) {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.prefix", "") +
                plugin.getConfig().getString("messages." + key, ""));
    }

    public void broadcastLight() {
        String title;
        String msgKey;
        if (state == LightState.GREEN) {
            title = ChatColor.GREEN + "Green Light";
            msgKey = "green";
        } else {
            title = ChatColor.RED + "Red Light";
            msgKey = "red";
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, "", 5, 40, 5);
        }
        Bukkit.broadcastMessage(msg(msgKey));
    }

    public void startGame() {
        gameActive = true;
        state = LightState.GREEN;
        Bukkit.broadcastMessage(msg("start"));
        broadcastLight();

        for (UUID id : admins) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                InteractListener.giveAdminDyes(p);
            }
        }
        saveAll();
    }

    public void stopGame() {
        gameActive = false;
        for (UUID id : admins) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                InteractListener.removeAdminDyes(p);
            }
        }
        saveAll();
    }

    public void setGreen() {
        state = LightState.GREEN;
        saveAll();
        broadcastLight();
    }

    public void setRed() {
        state = LightState.RED;
        saveAll();
        broadcastLight();
    }

    public void applyRules(World world) {
        if (world == null) return;
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setStorm(false);
        world.setThundering(false);
        world.setClearWeatherDuration(20 * 60 * 10);
    }

    public boolean isGameActive() { return gameActive; }
    public LightState getState() { return state; }

    public Location getLobby() { return lobby; }
    public void setLobby(Location lobby) { this.lobby = lobby; saveAll(); }

    public Location getGuestLobby() { return guestLobby; }
    public void setGuestLobby(Location guestLobby) { this.guestLobby = guestLobby; saveAll(); }

    public Location getFinish() { return finish; }
    public void setFinish(Location finish) { this.finish = finish; saveAll(); }

    public boolean isAdmin(UUID id) { return admins.contains(id); }
    public boolean isGuest(UUID id) { return guests.contains(id); }
    public boolean isWinner(UUID id) { return winners.contains(id); }

    public void addAdmin(UUID id) { admins.add(id); saveAll(); }
    public void addGuest(UUID id) { guests.add(id); saveAll(); }
    public void addWinner(UUID id) { winners.add(id); saveAll(); }

    public void removeAdmin(UUID id) { admins.remove(id); saveAll(); }
    public void removeGuest(UUID id) { guests.remove(id); saveAll(); }
    public void removeWinner(UUID id) { winners.remove(id); saveAll(); }

    public boolean isImmune(UUID id) { return isAdmin(id) || isGuest(id) || isWinner(id); }

    public static boolean sameBlock(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) return false;
        if (!a.getWorld().equals(b.getWorld())) return false;
        return a.getBlockX() == b.getBlockX()
            && a.getBlockY() == b.getBlockY()
            && a.getBlockZ() == b.getBlockZ();
    }
}
