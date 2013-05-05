package net.craftminecraft.bukkit.ipwhitelist;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import com.google.common.reflect.ClassPath;

public class PlayerListener implements Listener {
	IPWhitelist plugin;
	Class clazz = null;
	
	public PlayerListener(IPWhitelist plugin) {
		this.plugin = plugin;
	}
	
	// This is to avoid people disconnecting others. BungeeCord
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent ev) {
		// If we're in a case of using bungeecord, we shouldn't let people dc others
		if (plugin.getConfig().getBoolean("spigot_realip")) {
			for (Player p : plugin.getServer().getOnlinePlayers()) {
				if (ev.getPlayer().getName().equalsIgnoreCase(p.getName())) {
					ev.disallow(PlayerLoginEvent.Result.KICK_OTHER,
								"A player with this name is already connected to this server.");
				}
			}
		// Otherwise, act like all the other plugins out there.
		} else if (!this.plugin.getConfig().getStringList("whitelist").contains(ev.getAddress().getHostAddress())) {
			ev.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("playerKickMessage")));
		}
	}
	
	@SuppressWarnings("unchecked")
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (plugin.getConfig().getBoolean("spigot_realip")) {
			String address;

			try {
				// Get the class of the CraftPlayer.
				if (clazz == null) {
					ClassPath path = ClassPath.from(event.getPlayer().getClass().getClassLoader());
					for (ClassPath.ClassInfo classinfo : path.getTopLevelClasses()) {
						if (classinfo.getSimpleName().equals("CraftPlayer")) {
							clazz = Class.forName(classinfo.getName());
							break;
						}
					}
				}
	
				// Get the address from CraftPlayer
				Object playerEntity = clazz.getMethod("getHandle").invoke(event.getPlayer());
				
				Field f = playerEntity.getClass().getField("playerConnection");
				f.setAccessible(true);
				Object playerConnection = f.get(playerEntity);
				if (playerConnection == null) {
					plugin.getLogger().log(Level.INFO, "playerConnection is null");
					return;
				}
	
				f = playerConnection.getClass().getField("networkManager");
				f.setAccessible(true);
				Object networkManager = f.get(playerConnection);
				
				Object addr = networkManager.getClass().getMethod("getSocketAddress").invoke(networkManager);
				address = ((InetSocketAddress) addr).getAddress().getHostAddress();
			} catch (IOException ex) {
				plugin.getLogger().log(Level.SEVERE, "An exception has occured !", ex);
				return;
			} catch (ClassNotFoundException ex) {
				plugin.getLogger().log(Level.SEVERE, "Unsupported bukkit version. Please report to roblabla.", ex);
				return;
			} catch (NoSuchMethodException ex) {
				plugin.getLogger().log(Level.SEVERE, "Unsupported bukkit version. Please report to roblabla.", ex);
				return;
			} catch (NoSuchFieldException ex) {
				plugin.getLogger().log(Level.SEVERE, "Unsupported bukkit version. Please report to roblabla.", ex);
				return;
			} catch (InvocationTargetException ex) {
				plugin.getLogger().log(Level.SEVERE, "An exception has occured !", ex.getCause());
				return;
			} catch (Exception ex) {
				plugin.getLogger().log(Level.SEVERE, "An exception has occured !", ex);
				return;
			}
	
			if (!this.plugin.getConfig().getStringList("whitelist").contains(address)) {
				event.setJoinMessage(null);
				event.getPlayer().setMetadata("ipwhitelistkick", new FixedMetadataValue(plugin, true));
				event.getPlayer().kickPlayer(ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("playerKickMessage")));
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerLeave(PlayerQuitEvent ev) {
		if (!ev.getPlayer().getMetadata("ipwhitelistkick").isEmpty()) {
			ev.setQuitMessage(null);
			// Dunno if this is necessary... but I'd rather stay on the safe side
			ev.getPlayer().removeMetadata("ipwhitelistkick", plugin);
		}
	}
}