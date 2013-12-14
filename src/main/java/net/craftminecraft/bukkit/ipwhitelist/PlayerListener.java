package net.craftminecraft.bukkit.ipwhitelist;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class PlayerListener implements Listener {

    IPWhitelist plugin;
    Map<String, Method> methods = new HashMap<String, Method>();
    Map<String, Field> fields = new HashMap<String, Field>();

    public PlayerListener(IPWhitelist plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent ev) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
        Object mcServer = getField("console", plugin.getServer());
        Object serverConnection = invokeMethod("ag", mcServer);
        List listOfNetworkManager = (List)getField("f", serverConnection);
        for (Object networkManager : listOfNetworkManager) {
            Object packetListener = invokeMethod("getPacketListener", networkManager);
            if (!packetListener.getClass().getSimpleName().equals("LoginListener")) {
                plugin.getLogger().log(Level.INFO, packetListener.getClass().getSimpleName());
                continue;
            }
            Object gameProfile = getField("i", packetListener);
            String name = (String)invokeMethod("getName", gameProfile);
            if (!name.equalsIgnoreCase(ev.getPlayer().getName()))
            {
                plugin.getLogger().log(Level.INFO, name);
                continue;
            }
            Object channel = getField("k", networkManager);
            InetAddress addr = ((InetSocketAddress) invokeMethod("remoteAddress", channel)).getAddress();
            this.plugin.debug("Player " + name + " is connecting with IP : " + addr);
            if (!this.plugin.allow(addr)) {
                ev.setKickMessage(ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("playerKickMessage")));
                ev.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
            }
            break;
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