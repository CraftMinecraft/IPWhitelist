package net.craftminecraft.sponge.ipwhitelist;

import java.util.ArrayList;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.common.collect.Lists;

import org.spongepowered.api.Game;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.network.ChannelRegistrationEvent;

import net.craftminecraft.common.ipwhitelist.BungeeIPs;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id = "ipwhitelist", name = "IPWhitelist", version = "1.8")
public class IPWhitelist {

	@Inject
	private Game game;
	@Inject
	private Logger logger;

	private BungeeIPs bungeeips;

	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;

	@Listener
	public void onInit(GameInitializationEvent e) {
		this.bungeeips = new BungeeIPs(this.configManager, new ArrayList());
	}

	@Listener
	public void onServerStart(GameStartingServerEvent e) {
		this.registerCommands();
	}

	@Listener
	public void onPlayerJoin(ClientConnectionEvent.Join e) {
		if (this.bungeeips.isSetupModeEnabled()) {
			// Wait 2 seconds for a BungeeCord channel to be registered
			game.getScheduler().createTaskBuilder()
				.delayTicks(40)
				.execute(() -> {
					if (!this.bungeeips.allow(e.getTargetEntity().getConnection().getAddress()))
						e.getTargetEntity().kick(Text.of("Server is in setup mode"));
				});
		}
	}

	@Listener
	public void onPlayerAuth(ClientConnectionEvent.Auth e) {
        this.logger.debug("Player " + e.getProfile().getName().orElse("Unknown") + " is connecting with IP : " + e.getConnection().getAddress());
		if (!this.bungeeips.isSetupModeEnabled() &&
			!this.bungeeips.allow(e.getConnection().getAddress())) {
			e.setCancelled(true);
			// TODO: Should really be a Text...
			e.setMessage(TextSerializers.FORMATTING_CODE.deserialize(this.bungeeips.getKickMsg()));
		}
	}

	@Listener
	public void onPlayerChannelRegistered(ChannelRegistrationEvent.Register e) {
		if (this.bungeeips.isSetupModeEnabled() &&
			e.getChannel().equals("BungeeCord")) {
			// TODO: Handle setup mode
		}
	}

    public Text getTag() {
        return Text.of(TextStyles.ITALIC, TextColors.GREEN, "[", TextColors.AQUA, "IPWhitelist", TextColors.GREEN, "] ", TextStyles.RESET, TextColors.RESET);
    }

	public void registerCommands() {
		PaginationService pagination = game.getServiceManager().provide(PaginationService.class).get();
		CommandSpec list = CommandSpec.builder()
			.description(Text.of("List whitelisted IPs"))
			.arguments(GenericArguments.optional(GenericArguments.integer(Text.of("page")), 1))
			.executor((CommandSource src, CommandContext args) -> {
				int page = args.<Integer>getOne("page").orElse(0);
				pagination.builder()
					.title(Text.of("Whitelisted IPs"))
					.contents(Lists.transform(this.bungeeips.getIPs(), (v) ->
						Text.of(v)))
					.sendTo(src);
				return CommandResult.success();
			})
			.build();
		CommandSpec addip = CommandSpec.builder()
			.description(Text.of("Add IP to whitelist"))
			.arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("ip"))))
			.executor((src, args) -> {
				String ip = args.<String>getOne("ip").get();
				if (this.bungeeips.whitelist(ip))
					src.sendMessage(Text.of(this.getTag(), TextColors.AQUA, "Successfully whitelisted IP " + ip + "!"));
				else
					src.sendMessage(Text.of(this.getTag(), TextColors.AQUA, "IP " + ip + " was already whitelisted!"));
				return CommandResult.success();
			})
			.build();
		CommandSpec remip = CommandSpec.builder()
			.description(Text.of("Removes IP to whitelist"))
			.arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("ip"))))
			.executor((src, args) -> {
				String ip = args.<String>getOne("ip").get();
				switch (this.bungeeips.unwhitelist(ip)) {
					case 0:
						src.sendMessage(Text.of(this.getTag(), TextColors.AQUA, "Successfully unwhitelisted IP " + ip + "!"));
						break;
					case 1:
						src.sendMessage(Text.of(this.getTag(), TextColors.AQUA, "IP " + ip + " is in your bukkit.yml or spigot.yml bungee-proxies. Remove it there!"));
						break;
					case 2:
						src.sendMessage(Text.of(this.getTag(), TextColors.AQUA, "IP " + ip + " was not whitelisted!"));
				}
				return CommandResult.success();
			})
			.build();
		CommandSpec reload = CommandSpec.builder()
			.description(Text.of("Reload whitelist"))
			.executor((src, args) -> {
				this.bungeeips.reload();
				src.sendMessage(Text.of(this.getTag(), TextColors.AQUA, "Successfully reloaded config!"));
				return CommandResult.success();
			})
			.build();
		CommandSpec debug = CommandSpec.builder()
			.description(Text.of("Toggles debug output in console"))
			.executor((src, args) -> {
				if (this.bungeeips.toggleDebug()) {
					src.sendMessage(Text.of(this.getTag(), TextColors.AQUA, "Debug mode : ", TextColors.RED, "true"));
				} else {
					src.sendMessage(Text.of(this.getTag(), TextColors.AQUA, "Debug mode : ", TextColors.RED, "false"));
				}
				return CommandResult.success();
			})
			.build();
		CommandSpec setup = CommandSpec.builder()
			.description(Text.of("Toggles setup mode"))
			.executor((src, args) -> {
				// TODO: Move this logic to toggleSetup !
				if (!this.bungeeips.isSetupModeEnabled() &&
					!this.bungeeips.getIPs().isEmpty()) {
					src.sendMessage(Text.of(getTag(), TextColors.RED, "Cannot enable setup mode, some IPs are already whitelisted"));
					return CommandResult.success();
				}
				if (this.bungeeips.toggleSetup()) {
					src.sendMessage(Text.of(this.getTag(), TextColors.AQUA, "Setup mode : ", TextColors.RED, "true"));
				} else {
					src.sendMessage(Text.of(this.getTag(), TextColors.AQUA, "Setup mode : ", TextColors.RED, "false"));
				}
				return CommandResult.success();
			})
			.build();
		CommandSpec cmd = CommandSpec.builder()
			.permission("ipwhitelist.setup")
			.child(list, "list")
			.child(addip, "addip")
			.child(remip, "remip")
			.child(reload, "reload")
			//.child(debug, "debug") TODO: Change default verbosity level of slf4j
			.child(setup, "setup")
			.build();
		this.game.getCommandManager().register(this, cmd, "ipwl", "ipwhitelist");
	}
}
