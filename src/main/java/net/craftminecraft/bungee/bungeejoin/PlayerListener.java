package net.craftminecraft.bungee.bungeejoin;

import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerListener implements Listener {
	BungeeJoin plugin;
	public PlayerListener(BungeeJoin plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		CraftPlayer player = (CraftPlayer) event.getPlayer();
		String address = player.getHandle().playerConnection.networkManager.getSocket().getInetAddress().getHostAddress();
		if (!this.plugin.getConfig().getStringList("bungeeips").contains(address))
			event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("playerKickMessage")));
	}
}
