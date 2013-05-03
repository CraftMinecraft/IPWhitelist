package net.craftminecraft.bukkit.ipwhitelist;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerListener implements Listener {
	IPWhitelist plugin;
	public PlayerListener(IPWhitelist plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@SuppressWarnings("unchecked")
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		String address;
		
		if (plugin.getConfig().getBoolean("spigot_realip")) {
			// Get the class of the CraftPlayer.
			Class clazz;
			try {
				// Already support next version *grin*
				clazz = Class.forName("org.bukkit.craftbukkit.v1_5_R4.entity.CraftPlayer");
			} catch (ClassNotFoundException ex0) {
				try {
					clazz = Class.forName("org.bukkit.craftbukkit.v1_5_R3.entity.CraftPlayer");
				} catch (ClassNotFoundException ex1) {
					try {
						clazz = Class.forName("org.bukkit.craftbukkit.v1_5_R2.entity.CraftPlayer");
					} catch (ClassNotFoundException ex2) {
						try {
							clazz = Class.forName("org.bukkit.craftbukkit.v1_5_R1.entity.CraftPlayer");
						} catch (ClassNotFoundException ex3) {
							plugin.getLogger().severe("Unsupported bukkit version. Please report to roblabla.");
							return;
						}
					}
					// Should be enough for now ?
				}
			}
			
			// Get the address from CraftPlayer
			try {
				Object obj = clazz.getMethod("getHandle").invoke(event.getPlayer());
				Field f = obj.getClass().getField("playerConnection");
				f.setAccessible(true);
				Object playerConnection = f.get(obj);
				f = obj.getClass().getField("networkManager");
				f.setAccessible(true);
				Object networkManager = f.get(playerConnection);
				Object addr = networkManager.getClass().getMethod("getSocketAddress").invoke(networkManager);
				address = ((InetSocketAddress) addr).getAddress().getHostAddress();
			} catch (NoSuchMethodException ex) {
				plugin.getLogger().log(Level.SEVERE, "Unsupported bukkit version. Please report to roblabla.");
				return;
			} catch (NoSuchFieldException ex) {
				plugin.getLogger().log(Level.SEVERE, "Unsupported bukkit version. Please report to roblabla.");
				return;
			} catch (InvocationTargetException ex) {
				plugin.getLogger().log(Level.SEVERE, "An exception has occured !", ex.getCause());
				return;
			} catch (Exception ex) {
				plugin.getLogger().log(Level.SEVERE, "An exception has occured", ex);
				return;
			}
		} else {
			address = event.getHostname();
		}
		if (!this.plugin.getConfig().getStringList("whitelist").contains(address))
			event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("playerKickMessage")));
	}
}