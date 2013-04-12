package net.craftminecraft.bungee.bungeejoin;

import org.bukkit.plugin.java.JavaPlugin;

public class BungeeJoin extends JavaPlugin {
	  public void onEnable()
	  {
	    getConfig().options().copyDefaults(true);
	    saveDefaultConfig();
	    this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
	  }

	  public void onDisable() {
	  }
}
