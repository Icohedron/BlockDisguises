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
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.TypeTokens;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@Plugin(id = PluginInfo.ID, name = PluginInfo.NAME, version = PluginInfo.VERSION, description = PluginInfo.DESCRIPTION, authors = {"Icohedron"})
public class BlockDisguises {

    // Future improvements:
    // Disguises persist over server restarts and disconnects. May require additional custom data
    // Turn off player collision with disguised players (and turn them invisible too if this works). May require additional custom data

    public static Key<Value<UUID>> DISGUISE_OWNER;

    private final Text prefix = Text.of(TextColors.GRAY, "[", TextColors.GOLD, "BlockDisguise", TextColors.GRAY, "] ");

    private final String configFileName = "blockdisguises.conf";

    @Inject @ConfigDir(sharedRoot = true) private Path configurationPath;
    @Inject private Logger logger;
    @Inject private PluginContainer container;

    // Configuration Variables

    private DisguiseManager disguiseManager;

    // Handy for having an instance of this plugin available from anywhere
    private static BlockDisguises instance;
    public static BlockDisguises getInstance() {
        assert instance != null;
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
                    loadConfig();
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Reloaded configuration"));
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

        assert rootNode != null;
        return rootNode;
    }

    public DisguiseManager getDisguiseManager() {
        return disguiseManager;
    }

    public Logger getLogger() {
        return logger;
    }

    public Text getTextPrefix() {
        return Text.of(prefix); // Returns a copy of the prefix
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        ConfigurationNode config = loadConfig();
        disguiseManager.updateConfig(config);
    }

//    public void track(UUID entity) {
//        synchronized (trackedEntities) {
//            trackedEntities.add(entity);
//            serializeLeftoverEntities();
//        }
//    }
//
//    public boolean isBeingTracked(UUID entity) {
//        return trackedEntities.contains(entity);
//    }
//
//    public boolean isStray(UUID entity) {
//        for (DisguiseOwnerData disguiseData : uuidDisguiseDataMap.values()) {
//            if (disguiseData.getDisguiseEntityData().isPresent()) {
//                DisguiseEntityData disguiseEntityData = disguiseData.getDisguiseEntityData().get();
//                if (disguiseEntityData.getFallingBlock().equals(entity) || disguiseEntityData.getArmorStand().equals(entity)) {
//                    return false;
//                }
//            }
//        }
//        return true;
//    }
//
//    public void untrack(UUID entity) {
//        synchronized (trackedEntities) {
//            trackedEntities.remove(entity);
//            serializeLeftoverEntities();
//        }
//    }
//
//    public void disguise(Player player, BlockState blockState) {
//        disguised.put(player.getUniqueId(), player.getName());
//        vanishPlayer(player);
//        DisguiseEntityData disguiseEntityData = createDisguiseEntities(player, blockState);
//        uuidDisguiseDataMap.put(player.getUniqueId(), new DisguiseOwnerData(player.getUniqueId(), blockState, disguiseEntityData));
//        if (!disguisedActions.get("turn_solid")) {
//            uuidDisguiseDataMap.get(player.getUniqueId()).getSolidifyTask().cancel();
//        }
//    }
//
//    public void undisguise(Player player) {
//        unsolidify(player);
//        disguised.remove(player.getUniqueId());
//        unvanishPlayer(player);
//        uuidDisguiseDataMap.get(player.getUniqueId()).setDisguiseEntityData(null);
//        uuidDisguiseDataMap.get(player.getUniqueId()).getSolidifyTask().cancel();
//        uuidDisguiseDataMap.remove(player.getUniqueId());
//    }
//
//    public void vanishPlayer(Player player) {
////        InvisibilityData invisibilityData = player.getOrCreate(InvisibilityData.class).get();
////        invisibilityData.set(invisibilityData.ignoresCollisionDetection().set(true));
////        invisibilityData.set(invisibilityData.invisible().set(true));
////        invisibilityData.set(invisibilityData.untargetable().set(true));
////        invisibilityData.set(invisibilityData.vanish().set(false));
////        player.offer(invisibilityData);
//
//        player.offer(Keys.VANISH, true);
//        player.offer(Keys.VANISH_IGNORES_COLLISION, true);
//        player.offer(Keys.VANISH_PREVENTS_TARGETING, true);
//    }
//
//    public void unvanishPlayer(Player player) {
////        InvisibilityData invisibilityData = player.getOrCreate(InvisibilityData.class).get();
////        invisibilityData.set(invisibilityData.ignoresCollisionDetection().set(false));
////        invisibilityData.set(invisibilityData.invisible().set(false));
////        invisibilityData.set(invisibilityData.untargetable().set(false));
////        invisibilityData.set(invisibilityData.vanish().set(false));
////        player.offer(invisibilityData);
//
//        player.offer(Keys.VANISH, false);
//        player.offer(Keys.VANISH_IGNORES_COLLISION, false);
//        player.offer(Keys.VANISH_PREVENTS_TARGETING, false);
//    }
//
//    public DisguiseEntityData createDisguiseEntities(Player player, BlockState blockState) {
//        World world = player.getWorld();
//
//        Entity armorStand = world.createEntity(EntityTypes.ARMOR_STAND, player.getLocation().getPosition());
//        armorStand.offer(Keys.HAS_GRAVITY, false);
//        armorStand.offer(Keys.INVISIBLE, true);
//        armorStand.offer(Keys.ARMOR_STAND_MARKER, true);
//
//        Entity fallingBlock = world.createEntity(EntityTypes.FALLING_BLOCK, player.getLocation().getPosition());
//        fallingBlock.offer(Keys.HAS_GRAVITY, false);
//        fallingBlock.offer(Keys.FALL_TIME, Integer.MAX_VALUE);
//        fallingBlock.offer(Keys.FALLING_BLOCK_STATE, blockState);
//
//        world.spawnEntity(armorStand);
//        world.spawnEntity(fallingBlock);
//
//        armorStand.addPassenger(fallingBlock);
//
//        track(armorStand.getUniqueId());
//        track(fallingBlock.getUniqueId());
//
//        return new DisguiseEntityData(world.getUniqueId(), armorStand.getUniqueId(), fallingBlock.getUniqueId());
//    }
//
//    public synchronized void sendBlockChanges(Player player) {
//        Iterator<Map.Entry<UUID, SolidDisguise>> iterator = uuidSolidDisguiseMap.entrySet().iterator();
//        while (iterator.hasNext()) {
//            Map.Entry entry = iterator.next();
//            UUID key = (UUID) entry.getKey();
//            SolidDisguise value = (SolidDisguise) entry.getValue();
//
//            if (!key.equals(player.getUniqueId()) && value.getWorld().equals(player.getWorld().getUniqueId())) {
//                player.sendBlockChange(value.getLocation(), value.getBlockState());
//            }
//        }
//    }
//
//    public void solidify(UUID player) {
//        Optional<Player> disguisedPlayerOptional = Sponge.getServer().getPlayer(player);
//        if (disguisedPlayerOptional.isPresent()) {
//            solidify(disguisedPlayerOptional.get());
//        }
//    }
//
//    public void solidify(Player player) {
//        if (!disguised.containsKey(player.getUniqueId()) || uuidSolidDisguiseMap.containsKey(player.getUniqueId())) {
//            return;
//        }
//
//        if (!player.getLocation().getBlock().getType().equals(BlockTypes.AIR)) {
//            player.sendMessage(Text.of(prefix, TextColors.RED, "You can not become solid here!"));
//            getDisguiseData(player).get().resetSolidifyTask();
//            return;
//        }
//
//        uuidDisguiseDataMap.get(player.getUniqueId()).setDisguiseEntityData(null);
//        World world = player.getWorld();
//        Vector3i blockPosition = player.getLocation().getBlockPosition();
//
//        BlockState blockState = uuidDisguiseDataMap.get(player.getUniqueId()).getBlockState();
//
//        for (Player p : world.getPlayers()) {
//            if (p.getUniqueId().equals(player.getUniqueId())) {
//                continue;
//            }
//            p.sendBlockChange(blockPosition, blockState);
//        }
//
//        SolidDisguise solidDisguise = new SolidDisguise(player.getUniqueId(), world.getUniqueId(), blockPosition, blockState);
//
//        synchronized (uuidSolidDisguiseMap) {
//            uuidSolidDisguiseMap.put(player.getUniqueId(), solidDisguise);
//        }
//
//        player.sendMessage(Text.of(prefix, TextColors.GREEN, "You are now solid"));
//    }
//
//    public void unsolidify(UUID player) {
//        Optional<Player> disguisedPlayerOptional = Sponge.getServer().getPlayer(player);
//        if (disguisedPlayerOptional.isPresent()) {
//            unsolidify(disguisedPlayerOptional.get());
//        }
//    }
//
//    public void unsolidify(Player player) {
//        if (!disguised.containsKey(player.getUniqueId()) || !uuidSolidDisguiseMap.containsKey(player.getUniqueId())) {
//            return;
//        }
//
//        uuidDisguiseDataMap.get(player.getUniqueId()).setDisguiseEntityData(createDisguiseEntities(player, uuidDisguiseDataMap.get(player.getUniqueId()).getBlockState()));
//        World world = player.getWorld();
//        Vector3i blockPosition = uuidSolidDisguiseMap.get(player.getUniqueId()).getLocation();
//
//        for (Player p : world.getPlayers()) {
//            p.resetBlockChange(blockPosition);
//        }
//
//        synchronized (uuidSolidDisguiseMap) {
//            uuidSolidDisguiseMap.remove(player.getUniqueId());
//        }
//
//        player.sendMessage(Text.of(prefix, TextColors.RED, "You are no longer solid"));
//    }
//
//    public void damagePlayer(UUID target, Player source) {
//        Sponge.getServer().getPlayer(target).ifPresent(player -> {
//            DamageSource damageSource = EntityDamageSource.builder().type(DamageTypes.CUSTOM).entity(source).absolute().bypassesArmor().build();
//            player.damage(damageToDisguised, damageSource);
//            source.playSound(SoundTypes.ENTITY_PLAYER_HURT, player.getLocation().getPosition(), 1.0);
//        });
//    }
//
//    public boolean isDisguised(Player player) {
//        return disguised.containsKey(player.getUniqueId());
//    }
//
//    public boolean isDisguised(UUID player) {
//        return disguised.containsKey(player);
//    }
//
//    public Optional<DisguiseOwnerData> getDisguiseData(Player player) {
//        return getDisguiseData(player.getUniqueId());
//    }
//
//    public Optional<DisguiseOwnerData> getDisguiseData(UUID playerUUID) {
//        try {
//            return Optional.of(uuidDisguiseDataMap.get(playerUUID));
//        } catch (NullPointerException e) {
//            return Optional.empty();
//        }
//    }
//
//    public Optional<DisguiseOwnerData> getDisguiseData(FallingBlock fallingBlock) {
//        for (DisguiseOwnerData disguiseData : uuidDisguiseDataMap.values()) {
//            if (disguiseData.getDisguiseEntityData().isPresent()) {
//                if (disguiseData.getDisguiseEntityData().get().getFallingBlock().equals(fallingBlock.getUniqueId())) {
//                    return Optional.of(disguiseData);
//                }
//            }
//        }
//        return Optional.empty();
//    }
//
//    public Collection<SolidDisguise> getSolidDisguises() {
//        return uuidSolidDisguiseMap.values();
//    }
//
//    public boolean disguisedHasAction(String action) {
//        try {
//            return disguisedActions.get(action);
//        } catch (NullPointerException e) {
//            return true;
//        }
//    }
//
//    public int getSolidifyDelay() {
//        return solidifyDelay;
//    }
}
