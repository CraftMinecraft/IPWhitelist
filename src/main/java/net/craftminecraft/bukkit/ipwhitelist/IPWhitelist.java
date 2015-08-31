package net.craftminecraft.bukkit.ipwhitelist;

import java.io.File;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.ChatPaginator;
import org.bukkit.util.ChatPaginator.ChatPage;

import com.google.common.collect.Lists;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Level;

public class IPWhitelist extends JavaPlugin {

    private List<String> bungeeips = Lists.newArrayList();
    private List pendingList = null;

    public List getPendingList() {
        return pendingList;
    }

    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        reloadBukkitConfig();
    }

    private void reloadBukkitConfig() {
        bungeeips.clear();
        File spigotyml = new File(this.getDataFolder().getParentFile().getParentFile(), "spigot.yml");
        File bukkityml = new File(this.getDataFolder().getParentFile().getParentFile(), "bukkit.yml");
        if (spigotyml.exists()) {
            Configuration spigotcfg = YamlConfiguration.loadConfiguration(spigotyml);
            if (spigotcfg.getBoolean("settings.bungeecord")) {
                bungeeips.addAll(spigotcfg.getStringList("settings.bungeecord-addresses"));
            }
        } else if (bukkityml.exists()) {
            Configuration bukkitcfg = YamlConfiguration.loadConfiguration(new File(this.getDataFolder().getParentFile().getParentFile(), "bukkit.yml"));
            bungeeips.addAll(bukkitcfg.getStringList("settings.bungee-proxies"));
        }
    }

    public void onDisable() {
        bungeeips.clear();
    }

    public List<String> getBungeeIPs() {
        return this.bungeeips;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("ipwhitelist")) {
            return false;
        }
        if (!sender.hasPermission("ipwhitelist.setup")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to perform this action.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(getTag() + ChatColor.AQUA + "Commands : ");
            sender.sendMessage(ChatColor.AQUA + "/ipwhitelist list [page] - List whitelisted IPs");
            sender.sendMessage(ChatColor.AQUA + "/ipwhitelist addip <ip> - Add IP to whitelist");
            sender.sendMessage(ChatColor.AQUA + "/ipwhitelist remip <ip> - Removes IP to whitelist");
            sender.sendMessage(ChatColor.AQUA + "/ipwhitelist reload - Reload whitelist");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(getTag() + ChatColor.AQUA + "Whitelisted IPs :");
            StringBuilder iplistbuff = new StringBuilder();
            for (String ip : bungeeips) {
                iplistbuff.append(ChatColor.AQUA + ip + "\n");
            }
            for (String ip : getConfig().getStringList("whitelist")) {
                iplistbuff.append(ChatColor.AQUA + ip + "\n");
            }
            // Delete last newline if there is one.
            if (iplistbuff.length() > 0) {
                iplistbuff.deleteCharAt(iplistbuff.length() - 1);
            }
            String iplist = iplistbuff.toString();
            ChatPage page;
            if (args.length > 1) {
                page = ChatPaginator.paginate(iplist, Integer.parseInt(args[1]), ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH, ChatPaginator.CLOSED_CHAT_PAGE_HEIGHT - 2);
                sender.sendMessage(page.getLines());
            } else {
                page = ChatPaginator.paginate(iplist, 1, ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH, ChatPaginator.CLOSED_CHAT_PAGE_HEIGHT - 2);
                sender.sendMessage(page.getLines());
            }
            sender.sendMessage(ChatColor.AQUA + "Page " + page.getPageNumber() + "/" + page.getTotalPages() + ".");
            return true;
        }
        if (args[0].equalsIgnoreCase("addip")) {
            if (args.length < 2) {
                sender.sendMessage(getTag() + ChatColor.AQUA + "Command usage : ");
                sender.sendMessage(ChatColor.AQUA + "/ipwhitelist addip <ip>");
                return true;
            }
            if (!bungeeips.contains(args[1])
                    && !getConfig().getStringList("whitelist").contains(args[1])) {
                List<String> whitelist = getConfig().getStringList("whitelist");
                whitelist.add(args[1]);
                getConfig().set("whitelist", whitelist);
                this.saveConfig();
                sender.sendMessage(getTag() + ChatColor.AQUA + "Successfully whitelisted IP " + args[1] + "!");
                return true;
            }
            sender.sendMessage(getTag() + ChatColor.AQUA + "IP " + args[1] + " was already whitelisted!");
            return true;
        }
        if (args[0].equalsIgnoreCase("remip")) {
            if (args.length < 2) {
                sender.sendMessage(getTag() + ChatColor.AQUA + "Command usage : ");
                sender.sendMessage(ChatColor.AQUA + "/ipwhitelist remip <ip>");
                return true;
            }
            List<String> whitelist = getConfig().getStringList("whitelist");
            if (whitelist.remove(args[1])) {
                getConfig().set("whitelist", whitelist);
                this.saveConfig();
                sender.sendMessage(getTag() + ChatColor.AQUA + "Successfully unwhitelisted IP " + args[1] + "!");
                return true;
            }
            if (bungeeips.contains(args[1])) {
                sender.sendMessage(getTag() + ChatColor.AQUA + "IP " + args[1] + " is in your bukkit.yml or spigot.yml bungee-proxies. Remove it there!");
                return true;
            }
            sender.sendMessage(getTag() + ChatColor.AQUA + "IP " + args[1] + " was not whitelisted!");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            this.reloadConfig();
            reloadBukkitConfig();

            sender.sendMessage(getTag() + ChatColor.AQUA + "Successfully reloaded config!");
            return true;
        }
        if (args[0].equalsIgnoreCase("debug")) {
            this.getConfig().set("debug", !this.getConfig().getBoolean("debug", false));
            this.saveConfig();
            sender.sendMessage(getTag() + ChatColor.AQUA + "Debug mode : " + ChatColor.RED + this.getConfig().getBoolean("debug"));
            return true;
        }

        if (args[0].equalsIgnoreCase("setup")) {
            this.getConfig().set("setup", !this.getConfig().getBoolean("setup", false));
            this.saveConfig();
            sender.sendMessage(getTag() + ChatColor.AQUA + "Setup mode : " + ChatColor.RED + this.getConfig().getBoolean("setup"));
            return true;
        }
        sender.sendMessage(getTag() + ChatColor.AQUA + "Commands : ");
        sender.sendMessage(ChatColor.AQUA + "/ipwhitelist list [page] - List whitelisted IPs");
        sender.sendMessage(ChatColor.AQUA + "/ipwhitelist addip <ip> - Add IP to whitelist");
        sender.sendMessage(ChatColor.AQUA + "/ipwhitelist remip <ip> - Removes IP to whitelist");
        sender.sendMessage(ChatColor.AQUA + "/ipwhitelist reload - Reload whitelist");
        sender.sendMessage(ChatColor.AQUA + "/ipwhtelist debug - Toggles debug state");
        sender.sendMessage(ChatColor.AQUA + "/ipwhitelist setup - Turn setup mode on");
        return true;
    }

    public String getTag() {
        return ChatColor.ITALIC.toString() + ChatColor.GREEN + "[" + ChatColor.AQUA + this.getName() + ChatColor.GREEN + "] " + ChatColor.RESET;
    }

    public boolean allow(String ip) {
        return this.bungeeips.contains(ip)
                || this.getConfig().getStringList("whitelist").contains(ip);
    }

    public boolean allow(InetSocketAddress addr) {
        return allow(addr.getAddress().getHostAddress());
    }

    public boolean allow(InetAddress addr) {
        return allow(addr.getHostAddress());
    }

    public void whitelist(InetSocketAddress ip) {
        whitelist(ip.getAddress().getHostAddress());
    }

    public void whitelist(String ip) {
        this.getConfig().getStringList("whitelist").add(ip);
        this.saveConfig();
    }

    public void debug(String s) {
        if (this.getConfig().getBoolean("debug", false)) {
            this.getLogger().log(Level.INFO, s);
        }
    }
}
