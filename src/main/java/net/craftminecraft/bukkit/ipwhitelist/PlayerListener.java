package net.craftminecraft.bukkit.ipwhitelist;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

 import java.net.InetAddress;

public class PlayerListener implements Listener {
    
    IPWhitelist plugin;
    ReflectUtils reflect = new ReflectUtils();
    
    public PlayerListener(IPWhitelist plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent ev) {
            InetAddress addr = ev.getRealAddress();
            this.plugin.debug("Player " + ev.getPlayer().getName() + " is connecting with IP : " + addr);
            if (!this.plugin.allow(addr)) {
            ev.setKickMessage(ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("playerKickMessage")));
            ev.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
        }
    }
}