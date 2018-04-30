package io.github.icohedron.blockdisguises;

import com.google.inject.Inject;
import io.github.icohedron.blockdisguises.cmds.DisguiseCmd;
import io.github.icohedron.blockdisguises.cmds.ListCmd;
import io.github.icohedron.blockdisguises.cmds.UndisguiseAllCmd;
import io.github.icohedron.blockdisguises.cmds.UndisguiseCmd;
import io.github.icohedron.blockdisguises.data.DisguiseOwnerData;
import io.github.icohedron.blockdisguises.data.ImmutableDisguiseOwnerData;
import io.github.icohedron.blockdisguises.data.DisguiseOwnerDataBuilder;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.TypeTokens;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Plugin(id = PluginInfo.ID, name = PluginInfo.NAME, version = PluginInfo.VERSION, description = PluginInfo.DESCRIPTION, authors = {"Icohedron"})
public class BlockDisguises {

    // Future improvements:
    // - Disguises persist over server restarts. May require additional custom data
    // - Turn off player collision with disguised players (and turn them invisible too if this works). May require additional custom data
    // - Let hiders see their own disguise when solidified. Requires Contextual data to be implemented in Sponge

    public static Key<Value<UUID>> DISGUISE_OWNER;

    private static final Text prefix = Text.of(TextColors.GRAY, "[", TextColors.GOLD, "BlockDisguise", TextColors.GRAY, "] ");

    private final String configFileName = "blockdisguises.conf";

    @Inject @ConfigDir(sharedRoot = true) private Path configurationPath;
    @Inject private Logger logger;
    @Inject private PluginContainer container;

    // Debug mode enabled players
    private static Set<UUID> debug_mode_active = new HashSet(); // Set of players who enabled debug logs for themself
    public static void sendDebugMessage(String msg) {
        for (UUID uuid : debug_mode_active) {
            Optional<Player> player = Sponge.getServer().getPlayer(uuid);
            if (player.isPresent()) {
                player.get().sendMessage(Text.of(getTextPrefix(), TextColors.RED, "[Debug] ", TextColors.WHITE, msg));
            }
        }
    }

    // Configuration Variables

    private DisguiseManager disguiseManager;

    // Handy for having an instance of this plugin available from anywhere
    private static BlockDisguises instance;
    public static BlockDisguises getInstance() {
        if (instance == null) {
            throw new NullPointerException("No instance of BlockDisguises is available!");
        }
        return instance;
    }

    @Listener
    public void onConstruct(GameConstructionEvent event) {
        instance = this;
    }

    @Listener
    public void onInitialization(GameInitializationEvent event) {
        DISGUISE_OWNER = Key.builder()
                .type(TypeTokens.UUID_VALUE_TOKEN)
                .query(DataQuery.of("BlockDisguiseOwner"))
                .id(PluginInfo.ID + ":disguise_owner")
                .name("Block Disguise Owner")
                .build();

        DataRegistration.builder()
                .dataClass(DisguiseOwnerData.class)
                .immutableClass(ImmutableDisguiseOwnerData.class)
                .builder(new DisguiseOwnerDataBuilder())
                .dataName(("Disguise Owner Data"))
                .manipulatorId("disguise_owner_data")
                .buildAndRegister(this.container);

        ConfigurationNode config = loadConfig();
        disguiseManager = new DisguiseManager(config);

        initializeCommands();

        Sponge.getEventManager().registerListeners(this, new DisguiseListener());

        Task.builder()
            .execute(() ->
                Sponge.getServer().getOnlinePlayers().forEach(player -> disguiseManager.sendBlockChanges(player))
            ).intervalTicks(1).submit(this);

        logger.info("Finished initialization");
    }

    private void initializeCommands() {

        CommandSpec disguise = CommandSpec.builder()
                .description(Text.of("Disguise as a block"))
                .permission(PluginInfo.ID + ".command.disguise")
                .arguments( GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of("blocktype"), BlockType.class)),
                            GenericArguments.onlyOne(GenericArguments.playerOrSource(Text.of("player"))),
                            GenericArguments.flags().valueFlag(GenericArguments.string(Text.of("variant")), "-variant")
                                                    .valueFlag(GenericArguments.string(Text.of("facing")), "-facing")
                                                    .valueFlag(GenericArguments.string(Text.of("color")), "-color")
                                                    .valueFlag(GenericArguments.string(Text.of("half")), "-half")
                                                    .valueFlag(GenericArguments.string(Text.of("type")), "-type")
                                                    .valueFlag(GenericArguments.string(Text.of("wet")), "-wet")
                                                    .valueFlag(GenericArguments.string(Text.of("powered")), "-powered")
                                                    .valueFlag(GenericArguments.string(Text.of("delay")), "-delay")
                                                    .valueFlag(GenericArguments.string(Text.of("shape")), "-shape")
                                                    .valueFlag(GenericArguments.string(Text.of("conditional")), "-conditional")
                                                    .valueFlag(GenericArguments.string(Text.of("axis")), "-axis").buildWith(GenericArguments.none()))
                .executor(new DisguiseCmd())
                .build();

        CommandSpec undisguise = CommandSpec.builder()
                .description(Text.of("Remove block disguise"))
                .permission(PluginInfo.ID + ".command.undisguise")
                .arguments(GenericArguments.onlyOne(GenericArguments.playerOrSource(Text.of("player"))))
                .executor(new UndisguiseCmd())
                .build();

        CommandSpec undisguiseAll = CommandSpec.builder()
                .description(Text.of("Undisguise all players"))
                .permission(PluginInfo.ID + ".command.undisguiseall")
                .executor(new UndisguiseAllCmd())
                .build();

        CommandSpec list = CommandSpec.builder()
                .description(Text.of("List all disguised players"))
                .permission(PluginInfo.ID + ".command.list")
                .executor(new ListCmd())
                .build();

        CommandSpec reload = CommandSpec.builder()
                .description(Text.of("Reload the configuration"))
                .permission(PluginInfo.ID + ".command.reload")
                .executor((src, args) -> {
                    onReload(null);
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Reloaded configuration"));
                    return CommandResult.success();
                })
                .build();

        CommandSpec debug = CommandSpec.builder()
                .description(Text.of("Enable debug messages for yourself"))
                .permission(PluginInfo.ID + ".command.debug")
                .executor((src, args) -> {
                    if (!(src instanceof Player)) {
                        src.sendMessage(Text.of("You may only run this command as a player"));
                    }
                    UUID uuid = ((Player) src).getUniqueId();
                    if (debug_mode_active.contains(uuid)) {
                        debug_mode_active.remove(uuid);
                        src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Disabled debug messages for yourself"));
                    } else {
                        debug_mode_active.add(uuid);
                        src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Enabled debug messages for yourself"));
                    }
                    return CommandResult.success();
                })
                .build();

        CommandSpec blockdisguises = CommandSpec.builder()
                .description(Text.of("One command for BlockDisguises"))
                .child(disguise, "disguise")
                .child(undisguise, "undisguise")
                .child(undisguiseAll, "undisguiseall")
                .child(list, "list")
                .child(reload, "reload")
                .child(debug, "debug")
                .build();

        Sponge.getCommandManager().register(this, blockdisguises, "bd");
    }

    private ConfigurationNode loadConfig() {
        File configFile = new File(configurationPath.toFile(), configFileName);
        ConfigurationLoader<CommentedConfigurationNode> configurationLoader = HoconConfigurationLoader.builder().setFile(configFile).build();
        ConfigurationNode rootNode = null;

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            logger.info("Configuration file '" + configFile.getPath() + "' not found. Creating default configuration");
            try {
                Sponge.getAssetManager().getAsset(this, configFileName).get().copyToFile(configFile.toPath());
            } catch (IOException e) {
                logger.error("Failed to load default configuration file!");
                e.printStackTrace();
                logger.info("Falling back to internal defaults");
            }
        }

        try {
            rootNode = configurationLoader.load();
        } catch (IOException e) {
            logger.error("An error has occurred while reading the configuration file: ");
            e.printStackTrace();
            logger.info("Falling back to internal defaults");
            rootNode = configurationLoader.createEmptyNode();
        }

        return rootNode;
    }

    public DisguiseManager getDisguiseManager() {
        return disguiseManager;
    }

    public Logger getLogger() {
        return logger;
    }

    public static Text getTextPrefix() {
        return Text.of(prefix); // Returns a copy of the prefix
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        ConfigurationNode config = loadConfig();
        disguiseManager.updateConfig(config);
    }

    @Listener
    public void onGameStoppingServer(GameStoppingServerEvent event) {
        disguiseManager.undisguiseAll();
    }
}
