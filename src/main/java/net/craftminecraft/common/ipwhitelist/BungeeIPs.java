package net.craftminecraft.common.ipwhitelist;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import com.google.common.reflect.TypeToken;

import com.google.common.collect.ImmutableList;
import java.net.InetSocketAddress;
import java.net.InetAddress;

public class BungeeIPs {
    private CommentedConfigurationNode node;
    private List<String> bungeeips = new ArrayList();
    private ConfigurationLoader<CommentedConfigurationNode> loader;

    public BungeeIPs(ConfigurationLoader<CommentedConfigurationNode> loader, List<String> ips) {
        this.bungeeips = ips;
        this.loader = loader;
        this.reload();
    }

    public boolean isDebugEnabled() {
        return this.node.getNode("debug").getBoolean(false);
    }

    public boolean isSetupModeEnabled() {
        return this.node.getNode("setup").getBoolean(false);
    }

    public void reload() {
        try {
            this.node = loader.load();
            if (this.node.getNode("setup").getBoolean(false) &&
                !bungeeips.isEmpty() &&
                !this.node.getNode("whitelist").getList(TypeToken.of(String.class)).isEmpty()) {
                this.node.getNode("setup").setValue(false);
                this.loader.save(this.node);
            }
        } catch (ObjectMappingException e) {
        } catch (IOException e) {
            // TODO: Log IOException
            this.node = SimpleCommentedConfigurationNode.root();
        }
    }

    public ImmutableList<String> getIPs() {
        try {
        return ImmutableList.builder()
            .addAll(this.bungeeips)
            .addAll(this.node.getNode("whitelist").getList(TypeToken.of(String.class), new ArrayList()))
            .build();
        } catch (ObjectMappingException e) { return null; } // Impossible
    }

    public String getKickMsg() {
        return this.node.getNode("playerKickMessage").getString("&cYou have to join through the proxy.");
    }

    public boolean allow(String ip) {
        try {
            return this.bungeeips.contains(ip)
                || this.node.getNode("whitelist").getList(TypeToken.of(String.class), new ArrayList()).contains(ip);
        } catch (ObjectMappingException e) { return false; }
    }

    public boolean allow(InetSocketAddress addr) {
        return allow(addr.getAddress().getHostAddress());
    }

    public boolean allow(InetAddress addr) {
        return allow(addr.getHostAddress());
    }

    public boolean whitelist(InetSocketAddress ip) {
        return whitelist(ip.getAddress().getHostAddress());
    }

    public boolean whitelist(String ip) {
        List<String> whitelist;
        try {
            whitelist = new ArrayList(this.node.getNode("whitelist").getList(TypeToken.of(String.class)));
        } catch (ObjectMappingException e) { whitelist = new ArrayList(); }
        if (whitelist.contains(ip))
            return false;
        whitelist.add(ip);
        this.node.getNode("whitelist").setValue(whitelist);
        this.node.getNode("setup").setValue(false);
        try {
            this.loader.save(this.node);
        } catch (IOException e) {
            // TODO: log
        }
        return true;
    }

    public int unwhitelist(String ip) {
        List<String> whitelist;
        try {
            whitelist = new ArrayList(this.node.getNode("whitelist").getList(TypeToken.of(String.class)));
        } catch (ObjectMappingException e) { whitelist = new ArrayList(); }
        boolean removed = whitelist.remove(ip);
        this.node.getNode("whitelist").setValue(whitelist);
        try {
            this.loader.save(this.node);
        } catch (IOException e) {
            // TODO: log
        }
        if (removed)
            return 0;
        else if (bungeeips.contains(ip))
            return 1;
        else
            return 2;
    }

    public boolean toggleDebug() {
        boolean newV = !this.node.getNode("debug").getBoolean(false);
        this.node.getNode("debug").setValue(newV);
        try {
            this.loader.save(this.node);
        } catch (IOException e) {
            // TODO: log
        }
        return newV;
    }

    public boolean toggleSetup() {
        boolean newV = !this.node.getNode("setup").getBoolean(false);
        this.node.getNode("setup").setValue(newV);
        try {
            this.loader.save(this.node);
        } catch (IOException e) {
            // TODO: log
        }
        return newV;
    }
}
