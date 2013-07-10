package net.craftminecraft.bukkit.ipwhitelist;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import com.google.common.reflect.ClassPath;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Level;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class PlayerListener implements Listener {

    IPWhitelist plugin;

    public PlayerListener(IPWhitelist plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent ev) {
        try {
            Object entityPlayer = invokeMethod("getHandle", ev.getPlayer());
            Object playerConnection = getField("playerConnection", entityPlayer);
            Object networkManager = getField("networkManager", playerConnection);
            Object objaddress = invokeMethod("getSocketAddress", networkManager);
            InetSocketAddress address = (InetSocketAddress) objaddress;
            if (!this.plugin.allow(address)) {
                ev.setJoinMessage(null);
                if (ev.getPlayer().getMetadata("IPWhitelist_kick").isEmpty()) { // we only need one metadata. If for some reason it didn't get removed, no need to re-add it.
                    ev.getPlayer().setMetadata("IPWhitelist_kick", new FixedMetadataValue(plugin, true));
                }
                ev.getPlayer().kickPlayer(ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("playerKickMessage")));
            }
        } catch (InvocationTargetException ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex.getCause());
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerKick(PlayerKickEvent ev) {
        if (!ev.getPlayer().getMetadata("IPWhitelist_kick").isEmpty()) {
            ev.setCancelled(false); // Force the kick to happen.
            ev.setLeaveMessage(null);
            ev.getPlayer().removeMetadata("IPWhitelist_kick", plugin);
        }
    }

    public Object getField(String fieldname, Object obj) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getField(fieldname);
        f.setAccessible(true);
        return f.get(obj);
    }

    public Object invokeMethod(String methodname, Object obj, Object... args) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args.getClass();
        }
        Method m = obj.getClass().getMethod(methodname, paramTypes);
        m.setAccessible(true);
        return m.invoke(obj, args);
    }
}