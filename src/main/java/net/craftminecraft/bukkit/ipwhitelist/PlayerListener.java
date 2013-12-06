package net.craftminecraft.bukkit.ipwhitelist;

import com.avaje.ebean.LogLevel;
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
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class PlayerListener implements Listener {

    IPWhitelist plugin;
    Map<String, Method> methods = new HashMap<String, Method>();
    Map<String, Field> fields = new HashMap<String, Field>();

    public PlayerListener(IPWhitelist plugin) {
        this.plugin = plugin;
    }
    private boolean printShit = true;

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent ev) {
        try {
            Object entityPlayer = invokeMethod("getHandle", ev.getPlayer());
            Object playerConnection = getField("playerConnection", entityPlayer);
            Object networkManager = getField("networkManager", playerConnection);
            InetAddress addr;
            if (methodExists("getSocket", networkManager)) {
                addr = ((Socket) invokeMethod("getSocket", networkManager)).getInetAddress();
            } else if (fieldExists("socket", networkManager)) {
                addr = ((Socket) getField("socket", networkManager)).getInetAddress();
            } else if (fieldExists("k", networkManager)) {
                Object channel = getField("k", networkManager);
                addr = ((InetSocketAddress) invokeMethod("remoteAddress", channel)).getAddress();
            } else if (printShit) {
                for (Field f : networkManager.getClass().getDeclaredFields()) {
                    this.plugin.getLogger().log(Level.INFO, f.getType().getName() + " " + f.getName());
                }
                printShit = false;
                return;
            } else {
                return;
            }
            if (!this.plugin.allow(addr)) {
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

    public boolean fieldExists(String fieldname, Object obj) {
        String fieldid = obj.getClass().getName() + " " + fieldname;
        if (fields.containsKey(fieldid)) {
            return true;
        }
        
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (f.getName().equals(fieldname)) {
                fields.put(fieldid, f);
                return true;
            }
        }
        for (Field f : obj.getClass().getFields()) {
            if (f.getName().equals(fieldname)) {
                fields.put(fieldid, f);
                return true;
            }
        }
        return false;
    }

    public Object getField(String fieldname, Object obj) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        String fieldid = obj.getClass().getName() + " " + fieldname;
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (f.getName().equals(fieldname)) {
                f.setAccessible(true);
                fields.put(fieldid, f);
                return f.get(obj);
            }
        }
        for (Field f : obj.getClass().getFields()) {
            if (f.getName().equals(fieldname)) {
                f.setAccessible(true);
                fields.put(fieldid, f);
                return f.get(obj);
            }
        }
        return null;
    }

    public boolean methodExists(String methodname, Object obj, Object... args) {
        Class[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args.getClass();
        }
        
        String methodid = getMethodId(methodname, obj, paramTypes);
        
        if (methods.containsKey(methodid)) {
            return true;
        }
        
        for (Method m : obj.getClass().getDeclaredMethods()) {
            if (m.getName().equals(methodname) && Arrays.equals(m.getParameterTypes(), paramTypes)) {
                methods.put(methodid, m);
                return true;
            }
        }
        for (Method m : obj.getClass().getMethods()) {
            if (m.getName().equals(methodname) && Arrays.equals(m.getParameterTypes(), paramTypes)) {
                methods.put(methodid, m);
                return true;
            }
        }

        return false;
    }

    public Object invokeMethod(String methodname, Object obj, Object... args) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args.getClass();
        }

        String methodid = getMethodId(methodname, obj, paramTypes);

        if (methods.containsKey(methodid)) {
            return methods.get(methodid).invoke(obj, args);
        }

        for (Method m : obj.getClass().getDeclaredMethods()) {
            if (m.getName().equals(methodname) && Arrays.equals(m.getParameterTypes(), paramTypes)) {
                m.setAccessible(true);
                methods.put(methodid, m);
                return m.invoke(obj, args);
            }
        }
        for (Method m : obj.getClass().getMethods()) {
            if (m.getName().equals(methodname) && Arrays.equals(m.getParameterTypes(), paramTypes)) {
                m.setAccessible(true);
                methods.put(methodid, m);
                return m.invoke(obj, args);
            }
        }
        return null;
    }

    public String getMethodId(String methodname, Object obj, Class[] paramTypes) {
        StringBuilder b = new StringBuilder(obj.getClass().getName() + "." + methodname + "(");
        if (paramTypes.length > 0) {
            b.append(paramTypes[0].getName());
            for (int i = 1; i < paramTypes.length; i++) {
                b.append(", ");
                b.append(paramTypes[i].getName());
            }
        }
        b.append(");");
        return b.toString();
    }
}