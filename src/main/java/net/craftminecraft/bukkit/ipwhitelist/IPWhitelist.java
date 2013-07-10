package net.craftminecraft.bukkit.ipwhitelist;

import java.io.File;
import java.io.IOException;
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
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import org.bukkit.entity.Player;

public class IPWhitelist extends JavaPlugin {

    private boolean spigotSupport = false;
    private List<String> bungeeips = Lists.newArrayList();
    private List pendingList = null;
    private Class<? extends Player> clazz;

    public List getPendingList() {
        return pendingList;
    }

    public boolean getSpigotSupport() {
        return this.spigotSupport;
    }

    public Class getPlayerClass() {
        return clazz;
    }

    public void setPlayerClass(Class clazz) {
        this.clazz = clazz;
    }

    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        Configuration bukkityml = YamlConfiguration.loadConfiguration(new File(this.getDataFolder().getParentFile().getParentFile(), "bukkit.yml"));
        if (bukkityml.contains("settings.bungee-proxies")) {
            bungeeips.addAll(bukkityml.getStringList("settings.bungee-proxies"));
            spigotSupport = true;
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
            StringBuffer iplistbuff = new StringBuffer();
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
                sender.sendMessage(getTag() + ChatColor.AQUA + "IP " + args[1] + " is in your bukkit.yml's bungee-proxies. Remove it there!");
                return true;
            }
            sender.sendMessage(getTag() + ChatColor.AQUA + "IP " + args[1] + " was not whitelisted!");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            this.reloadConfig();
            bungeeips.clear();
            Configuration bukkityml = YamlConfiguration.loadConfiguration(new File(this.getDataFolder().getParentFile().getParentFile(), "bukkit.yml"));
            if (bukkityml.contains("settings.bungee-proxies")) {
                bungeeips.addAll(bukkityml.getStringList("settings.bungee-proxies"));
                spigotSupport = true;
            }

            sender.sendMessage(getTag() + ChatColor.AQUA + "Successfully reloaded config!");
            return true;
        }
        sender.sendMessage(getTag() + ChatColor.AQUA + "Commands : ");
        sender.sendMessage(ChatColor.AQUA + "/ipwhitelist list [page] - List whitelisted IPs");
        sender.sendMessage(ChatColor.AQUA + "/ipwhitelist addip <ip> - Add IP to whitelist");
        sender.sendMessage(ChatColor.AQUA + "/ipwhitelist remip <ip> - Removes IP to whitelist");
        sender.sendMessage(ChatColor.AQUA + "/ipwhitelist reload - Reload whitelist");
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
}
