package io.github.icohedron.blockdisguises;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.FallingBlock;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Plugin(id = "blockdisguises", name = "Block Disguises", version = "1.0.0-S5.1-SNAPSHOT-1",
        description = "Disguise as a block!", authors = {"Icohedron"})
public class BlockDisguises {

    // Known bug: When using Nucleus: If the player has the permission nucleus.vanish.persist, and the server crashes, then the player will keep vanish upon reconnection to the server if they were disguised before the crash.
    // Known bug: Sometimes .tmp files will be made in the configuration directory. You can delete those

    private final Text prefix = Text.of(TextColors.GRAY, "[", TextColors.GOLD, "BlockDisguise", TextColors.GRAY, "] ");

    private final String configFileName = "blockdisguises.conf";
    private final String trackedEntitiesFileName = "trackedEntities.dat";

    @Inject @ConfigDir(sharedRoot = false) private Path configurationPath;
    @Inject private Logger logger;

    private ConfigurationLoader<CommentedConfigurationNode> trackedEntitiesConfig;
    private ConfigurationNode trackedEntitiesNode;
    private Set<UUID> trackedEntities;

    private int solidifyDelay; // Amount of delay, in seconds, before a disguise turns into a solid block
    private double damageToDisguised; // Amount of damage dealt to disguised players when hit
    private Map<String, Boolean> disguisedActions;

    private Map<UUID, String> disguised; // <Player UUID, Player name>
    private Map<UUID, DisguiseData> uuidDisguiseDataMap; // <Player UUID, DisguiseData>
    private Map<UUID, SolidDisguise> uuidSolidDisguiseMap; // <Player UUID, SolidDisguise>

    private static BlockDisguises instance;

    public static BlockDisguises getInstance() {
        if (instance == null) {
            throw new RuntimeException("No instance of Block Disguise is available");
        }
        return instance;
    }

    @Listener
    public void onConstruct(GameConstructionEvent event) {
        instance = this;
    }

    @Listener
    public void onInitialization(GameInitializationEvent event) {
        disguised = new HashMap<>();
        uuidDisguiseDataMap = new HashMap<>();
        uuidSolidDisguiseMap = new HashMap<>();
        trackedEntities = new HashSet<>();

        initializeCommands();
        loadConfig();
        loadTrackedEntitiesFile();
        deserializeLeftoverEntities();
        Sponge.getEventManager().registerListeners(this, new DisguiseListener());
        logger.info("Finished initialization");
    }

    private void initializeCommands() {

        CommandSpec disguise = CommandSpec.builder()
                .description(Text.of("Disguise as a block"))
                .permission("blockdisguises.command.disguise")
                .arguments( GenericArguments.onlyOne(GenericArguments.string(Text.of("blocktype"))),
                        GenericArguments.onlyOne(GenericArguments.playerOrSource(Text.of("player"))))
                .executor((src, args) -> {
                    Player player = args.<Player>getOne("player").get();

                    if (disguised.containsKey(player.getUniqueId())) {
                        if (src == player) {
                            src.sendMessage(Text.of(prefix, TextColors.RED, "You are already disguised"));
                        } else {
                            src.sendMessage(Text.of(prefix, TextColors.RED, "'" + player.getName() + "' is already disguised"));
                        }
                        return CommandResult.empty();
                    }

                    String blockTypeString = args.<String>getOne("blocktype").get();
                    Optional<BlockType> blockType = Sponge.getRegistry().getType(BlockType.class, blockTypeString);
                    if (!blockType.isPresent()) {
                        src.sendMessage(Text.of(prefix, TextColors.RED, "'" + blockTypeString + "' is not a valid block type"));
                        return CommandResult.empty();
                    }

                    disguise(player, blockType.get().getDefaultState());

                    String start = player.getName() + " is";
                    if (src == player) {
                        start = "You are";
                    }
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, start + " now disguised as " + blockType.get().getDefaultState().getType().getName()));
                    return CommandResult.success();
                })
                .build();

        CommandSpec undisguise = CommandSpec.builder()
                .description(Text.of("Remove block disguise"))
                .permission("blockdisguises.command.undisguise")
                .arguments(GenericArguments.onlyOne(GenericArguments.playerOrSource(Text.of("player"))))
                .executor((src, args) -> {
                    Player player = args.<Player>getOne("player").get();

                    if (!disguised.containsKey(player.getUniqueId())) {
                        if (src == player) {
                            src.sendMessage(Text.of(prefix, TextColors.RED, "You are not currently disguised"));
                        } else {
                            src.sendMessage(Text.of(prefix, TextColors.RED, "'" + player.getName() + "' is not currently disguised"));
                        }
                        return CommandResult.empty();
                    }

                    undisguise(player);

                    String start = player.getName() + " is";
                    if (src == player) {
                        start = "You are";
                    }
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, start + " no longer disguised as a block"));
                    return CommandResult.success();
                })
                .build();

        CommandSpec undisguiseAll = CommandSpec.builder()
                .description(Text.of("Undisguise all players"))
                .permission("blockdisguises.command.undisguiseall")
                .executor((src, args) -> {
                    for (UUID uuid : disguised.keySet()) {
                        Optional<Player> player = Sponge.getServer().getPlayer(uuid);
                        if (player.isPresent()) {
                            undisguise(player.get());
                        } else {
                            unsolidify(uuid);
                            disguised.remove(uuid);
                            if (uuidDisguiseDataMap.containsKey(uuid)) {
                                uuidDisguiseDataMap.get(uuid).setDisguiseEntityData(null);
                                uuidDisguiseDataMap.get(uuid).getSolidifyTask().cancel();
                                uuidDisguiseDataMap.remove(uuid);
                            }
                        }
                    }
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Undisguised all players"));
                    return CommandResult.success();
                })
                .build();

        CommandSpec list = CommandSpec.builder()
                .description(Text.of("List all disguised players"))
                .permission("blockdisguises.command.list")
                .executor((src, args) -> {
                    List<Text> contents = new ArrayList<>(disguised.size());
                    for (Map.Entry<UUID, String> entry : disguised.entrySet()) {

                        Optional<DisguiseData> disguiseData = getDisguiseData(entry.getKey());
                        if (disguiseData.isPresent()) {
                            contents.add(Text.of(entry.getValue(), TextColors.GRAY, " -> Disguised as ", TextColors.YELLOW, disguiseData.get().getBlockState().getType().getName()));
                        } else {
                            contents.add(Text.of(entry.getValue()));
                        }

                    }
                    PaginationList.builder()
                            .title(Text.of(TextColors.DARK_GREEN, "Disguised players"))
                            .contents(contents)
                            .padding(Text.of(TextColors.DARK_GREEN, "="))
                            .build().sendTo(src);
                    return CommandResult.success();
                })
                .build();

        CommandSpec reload = CommandSpec.builder()
                .description(Text.of("Reload the configuration"))
                .permission("blockdisguises.command.reload")
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

    private void loadConfig() {
        File configFile = new File(configurationPath.toFile(), configFileName);
        ConfigurationLoader<CommentedConfigurationNode> configurationLoader = HoconConfigurationLoader.builder().setFile(configFile).build();
        ConfigurationNode rootNode;

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            logger.info("Configuration file '" + configFile.getPath() + "' not found. Creating default configuration");
            try {
                Sponge.getAssetManager().getAsset(this, configFileName).get().copyToFile(configFile.toPath());
            } catch (IOException e) {
                logger.error("Failed to load default configuration file!");
                e.printStackTrace();
            }
        }

        try {
            rootNode = configurationLoader.load();

            solidifyDelay = rootNode.getNode("solidify_delay").getInt(4);
            damageToDisguised = rootNode.getNode("damage_to_disguised").getDouble(5.0);

            disguisedActions = new HashMap<>();
            rootNode.getNode("allowed_actions_while_disguised").getChildrenMap().forEach(
                    (key, value) -> disguisedActions.put((String) key, value.getBoolean(true)));

        } catch (IOException e) {
            logger.error("An error has occurred while reading the configuration file: ");
            e.printStackTrace();
        }
    }

    private void loadTrackedEntitiesFile() {
        File trackedEntitiesFile = new File(configurationPath.toFile(), trackedEntitiesFileName);
        trackedEntitiesConfig = HoconConfigurationLoader.builder().setFile(trackedEntitiesFile).build();

        if (!trackedEntitiesFile.exists()) {
            trackedEntitiesFile.getParentFile().mkdirs();
            try {
                trackedEntitiesFile.createNewFile();
            } catch (IOException e) {
                logger.error("Failed to create '" + trackedEntitiesFile.getPath() + "': ");
                e.printStackTrace();
            }
        }

        try {
            trackedEntitiesNode = trackedEntitiesConfig.load();
        } catch (Exception e) {
            logger.error("An error has occurred while reading the " + trackedEntitiesFileName + " file: ");
            e.printStackTrace();
        }
    }

    private void serializeLeftoverEntities() {
        try {
            List<UUID> list = new LinkedList<>(trackedEntities); // Serialize a list rather than a set. Sets have some issues with serialization
            trackedEntitiesNode.getNode("trackedEntities").setValue(new TypeToken<List<UUID>>() {}, list);
            trackedEntitiesConfig.save(trackedEntitiesNode);
        } catch (ObjectMappingException | IOException e) {
            if (e instanceof NoSuchFileException || e instanceof AccessDeniedException || e instanceof FileAlreadyExistsException) {
                return; // Exceptions caused from a .tmp file that Windows creates for whatever reason while saving trackedEntities.dat
                // The name of the temporary file is something like 1502484168654trackedEntities.dat.tmp -- The numbers may be different. No idea why it's made, but it's there and it causes exceptions that don't seem to affect any functionality
            }
            e.printStackTrace();
        }
    }

    private void deserializeLeftoverEntities() {
        try {
            List<UUID> list = trackedEntitiesNode.getNode("trackedEntities").getValue(new TypeToken<List<UUID>>() {});
            if (list != null) {
                trackedEntities = new HashSet<>(list);
            }
            trackedEntitiesNode.removeChild("trackedEntities");
            trackedEntitiesConfig.save(trackedEntitiesNode);
        } catch (ObjectMappingException | IOException e) {
            e.printStackTrace();
        }
    }

    public void track(UUID entity) {
        synchronized (trackedEntities) {
            trackedEntities.add(entity);
        }
        serializeLeftoverEntities();
    }

    public boolean isBeingTracked(UUID entity) {
        return trackedEntities.contains(entity);
    }

    public boolean isStray(UUID entity) {
        for (DisguiseData disguiseData : uuidDisguiseDataMap.values()) {
            if (disguiseData.getDisguiseEntityData().isPresent()) {
                DisguiseEntityData disguiseEntityData = disguiseData.getDisguiseEntityData().get();
                if (disguiseEntityData.getFallingBlock().equals(entity) || disguiseEntityData.getArmorStand().equals(entity)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void untrack(UUID entity) {
        synchronized (trackedEntities) {
            trackedEntities.remove(entity);
        }
        serializeLeftoverEntities();
    }

    public void disguise(Player player, BlockState blockState) {
        disguised.put(player.getUniqueId(), player.getName());
        vanishPlayer(player);
        DisguiseEntityData disguiseEntityData = createDisguiseEntities(player, blockState);
        uuidDisguiseDataMap.put(player.getUniqueId(), new DisguiseData(player.getUniqueId(), blockState, disguiseEntityData));
        if (!disguisedActions.get("turn_solid")) {
            uuidDisguiseDataMap.get(player.getUniqueId()).getSolidifyTask().cancel();
        }
    }

    public void undisguise(Player player) {
        unsolidify(player);
        disguised.remove(player.getUniqueId());
        unvanishPlayer(player);
        uuidDisguiseDataMap.get(player.getUniqueId()).setDisguiseEntityData(null);
        uuidDisguiseDataMap.get(player.getUniqueId()).getSolidifyTask().cancel();
        uuidDisguiseDataMap.remove(player.getUniqueId());
    }

    public void vanishPlayer(Player player) {
//        InvisibilityData invisibilityData = player.getOrCreate(InvisibilityData.class).get();
//        invisibilityData.set(invisibilityData.ignoresCollisionDetection().set(true));
//        invisibilityData.set(invisibilityData.invisible().set(true));
//        invisibilityData.set(invisibilityData.untargetable().set(true));
//        invisibilityData.set(invisibilityData.vanish().set(false));
//        player.offer(invisibilityData);

        player.offer(Keys.VANISH, true);
        player.offer(Keys.VANISH_IGNORES_COLLISION, true);
        player.offer(Keys.VANISH_PREVENTS_TARGETING, true);
    }

    public void unvanishPlayer(Player player) {
//        InvisibilityData invisibilityData = player.getOrCreate(InvisibilityData.class).get();
//        invisibilityData.set(invisibilityData.ignoresCollisionDetection().set(false));
//        invisibilityData.set(invisibilityData.invisible().set(false));
//        invisibilityData.set(invisibilityData.untargetable().set(false));
//        invisibilityData.set(invisibilityData.vanish().set(false));
//        player.offer(invisibilityData);

        player.offer(Keys.VANISH, false);
        player.offer(Keys.VANISH_IGNORES_COLLISION, false);
        player.offer(Keys.VANISH_PREVENTS_TARGETING, false);
    }

    public DisguiseEntityData createDisguiseEntities(Player player, BlockState blockState) {
        World world = player.getWorld();

        Entity armorStand = world.createEntity(EntityTypes.ARMOR_STAND, player.getLocation().getPosition());
        armorStand.offer(Keys.HAS_GRAVITY, false);
        armorStand.offer(Keys.INVISIBLE, true);
        armorStand.offer(Keys.ARMOR_STAND_MARKER, true);

        Entity fallingBlock = world.createEntity(EntityTypes.FALLING_BLOCK, player.getLocation().getPosition());
        fallingBlock.offer(Keys.HAS_GRAVITY, false);
        fallingBlock.offer(Keys.FALL_TIME, Integer.MAX_VALUE);
        fallingBlock.offer(Keys.FALLING_BLOCK_STATE, blockState);

        SpawnCause spawnCause = SpawnCause.builder().type(SpawnTypes.PLUGIN).build();
        world.spawnEntity(armorStand, Cause.source(spawnCause).build());
        world.spawnEntity(fallingBlock, Cause.source(spawnCause).build());

        armorStand.addPassenger(fallingBlock);

        track(armorStand.getUniqueId());
        track(fallingBlock.getUniqueId());

        return new DisguiseEntityData(world.getUniqueId(), armorStand.getUniqueId(), fallingBlock.getUniqueId());
    }

    public synchronized void sendBlockChanges(Player player) {
        Iterator<Map.Entry<UUID, SolidDisguise>> iterator = uuidSolidDisguiseMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = iterator.next();
            UUID key = (UUID) entry.getKey();
            SolidDisguise value = (SolidDisguise) entry.getValue();

            if (!key.equals(player.getUniqueId()) && value.getWorld().equals(player.getWorld().getUniqueId())) {
                player.sendBlockChange(value.getLocation(), value.getBlockState());
            }
        }
    }

    public void solidify(UUID player) {
        Optional<Player> disguisedPlayerOptional = Sponge.getServer().getPlayer(player);
        if (disguisedPlayerOptional.isPresent()) {
            solidify(disguisedPlayerOptional.get());
        }
    }

    public void solidify(Player player) {
        if (!disguised.containsKey(player.getUniqueId()) || uuidSolidDisguiseMap.containsKey(player.getUniqueId())) {
            return;
        }

        if (!player.getLocation().getBlock().getType().equals(BlockTypes.AIR)) {
            player.sendMessage(Text.of(prefix, TextColors.RED, "You can not become solid here!"));
            getDisguiseData(player).get().resetSolidifyTask();
            return;
        }

        uuidDisguiseDataMap.get(player.getUniqueId()).setDisguiseEntityData(null);
        World world = player.getWorld();
        Vector3i blockPosition = player.getLocation().getBlockPosition();

        BlockState blockState = uuidDisguiseDataMap.get(player.getUniqueId()).getBlockState();

        for (Player p : world.getPlayers()) {
            if (p.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            p.sendBlockChange(blockPosition, blockState);
        }

        SolidDisguise solidDisguise = new SolidDisguise(player.getUniqueId(), world.getUniqueId(), blockPosition, blockState);

        synchronized (uuidSolidDisguiseMap) {
            uuidSolidDisguiseMap.put(player.getUniqueId(), solidDisguise);
        }

        player.sendMessage(Text.of(prefix, TextColors.GREEN, "You are now solid"));
    }

    public void unsolidify(UUID player) {
        Optional<Player> disguisedPlayerOptional = Sponge.getServer().getPlayer(player);
        if (disguisedPlayerOptional.isPresent()) {
            unsolidify(disguisedPlayerOptional.get());
        }
    }

    public void unsolidify(Player player) {
        if (!disguised.containsKey(player.getUniqueId()) || !uuidSolidDisguiseMap.containsKey(player.getUniqueId())) {
            return;
        }

        uuidDisguiseDataMap.get(player.getUniqueId()).setDisguiseEntityData(createDisguiseEntities(player, uuidDisguiseDataMap.get(player.getUniqueId()).getBlockState()));
        World world = player.getWorld();
        Vector3i blockPosition = uuidSolidDisguiseMap.get(player.getUniqueId()).getLocation();

        for (Player p : world.getPlayers()) {
            p.resetBlockChange(blockPosition);
        }

        synchronized (uuidSolidDisguiseMap) {
            uuidSolidDisguiseMap.remove(player.getUniqueId());
        }

        player.sendMessage(Text.of(prefix, TextColors.RED, "You are no longer solid"));
    }

    public void damagePlayer(UUID target, Player source) {
        Sponge.getServer().getPlayer(target).ifPresent(player -> {
            DamageSource damageSource = EntityDamageSource.builder().type(DamageTypes.CUSTOM).entity(source).absolute().bypassesArmor().build();
            player.damage(damageToDisguised, damageSource);
            source.playSound(SoundTypes.ENTITY_PLAYER_HURT, player.getLocation().getPosition(), 1.0);
        });
    }

    public boolean isDisguised(Player player) {
        return disguised.containsKey(player.getUniqueId());
    }

    public Optional<DisguiseData> getDisguiseData(Player player) {
        return getDisguiseData(player.getUniqueId());
    }

    public Optional<DisguiseData> getDisguiseData(UUID playerUUID) {
        try {
            return Optional.of(uuidDisguiseDataMap.get(playerUUID));
        } catch (NullPointerException e) {
            return Optional.empty();
        }
    }

    public Optional<DisguiseData> getDisguiseData(FallingBlock fallingBlock) {
        for (DisguiseData disguiseData : uuidDisguiseDataMap.values()) {
            if (disguiseData.getDisguiseEntityData().isPresent()) {
                if (disguiseData.getDisguiseEntityData().get().getFallingBlock().equals(fallingBlock.getUniqueId())) {
                    return Optional.of(disguiseData);
                }
            }
        }
        return Optional.empty();
    }

    public Collection<SolidDisguise> getSolidDisguises() {
        return uuidSolidDisguiseMap.values();
    }

    public boolean disguisedHasAction(String action) {
        try {
            return disguisedActions.get(action);
        } catch (NullPointerException e) {
            return true;
        }
    }

    public int getSolidifyDelay() {
        return solidifyDelay;
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        loadConfig();
    }
}
