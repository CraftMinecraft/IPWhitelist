package net.craftminecraft.bukkit.ipwhitelist;

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
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
	    String address;
	    try {
	        address = event.getPlayer().getRawAddress().getAddress().getHostAddress();
	    } catch (NoSuchMethodError err) {
	        address = event.getAddress().getHostAddress();
	    }
		if (!this.plugin.getConfig().getStringList("whitelist").contains(address)
		    && !this.plugin.getBungeeIPs().contains(address))
			event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("playerKickMessage")));
	}
}
