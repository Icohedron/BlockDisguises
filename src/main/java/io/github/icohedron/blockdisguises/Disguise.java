package io.github.icohedron.blockdisguises;

import io.github.icohedron.blockdisguises.data.DisguiseOwnerData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.UUID;

public class Disguise {

    public enum State {
        MOBILE, SOLID
    }

    private State state; // Current state of this disguise
    private UUID owner; // Disguise owner, as a player's UUID
    private BlockState blockState; // The block that this disguise represents

    private long stateTimer; // Timer for transitioning between states

    // UUIDs of the armor stand and falling block making the moving disguise
    private UUID armorStand;
    private UUID fallingBlock;

    // UUID of the world this disguise is in
    private UUID worldID;

    // Creates a disguise for a given online player
    public Disguise(UUID player, BlockState blockState) {
        state = State.MOBILE;
        owner = player;
        this.blockState = blockState;
        createEntites();
    }

    public void createEntites() {
        Optional<Player> playerOptional = Sponge.getServer().getPlayer(owner);
        assert playerOptional.isPresent();

        Player player = playerOptional.get();
        World world = player.getWorld();
        worldID = world.getUniqueId();

        DisguiseOwnerData disguiseOwnerData = new DisguiseOwnerData(owner);
        DataTransactionResult dataTransactionResult;

        Entity armorStandEntity = world.createEntity(EntityTypes.ARMOR_STAND, player.getLocation().getPosition());
        armorStandEntity.offer(Keys.HAS_GRAVITY, false);
        armorStandEntity.offer(Keys.INVISIBLE, true);
        armorStandEntity.offer(Keys.ARMOR_STAND_MARKER, true);

        dataTransactionResult = armorStandEntity.offer(disguiseOwnerData);
        assert dataTransactionResult.isSuccessful();

        Entity fallingBlockEntity = world.createEntity(EntityTypes.FALLING_BLOCK, player.getLocation().getPosition());
        fallingBlockEntity.offer(Keys.HAS_GRAVITY, false);
        fallingBlockEntity.offer(Keys.FALL_TIME, Integer.MAX_VALUE);
        fallingBlockEntity.offer(Keys.FALLING_BLOCK_STATE, blockState);

        dataTransactionResult = fallingBlockEntity.offer(disguiseOwnerData);
        assert dataTransactionResult.isSuccessful();

        world.spawnEntity(armorStandEntity);
        world.spawnEntity(fallingBlockEntity);

        armorStandEntity.addPassenger(fallingBlockEntity);

        armorStand = armorStandEntity.getUniqueId();
        fallingBlock = fallingBlockEntity.getUniqueId();
    }

    private void removeEntities() {
        Optional<World> worldOptional = Sponge.getServer().getWorld(worldID);
        assert worldOptional.isPresent();

        World world = worldOptional.get();

        if (armorStand != null) {
            Optional<Entity> entity = world.getEntity(armorStand);
            entity.ifPresent(Entity::remove);
        }

        if (fallingBlock != null) {
            Optional<Entity> entity = world.getEntity(fallingBlock);
            entity.ifPresent(Entity::remove);
        }
    }

    public void sendBlockChanges() {
        Optional<Player> playerOptional = Sponge.getServer().getPlayer(owner);
        assert playerOptional.isPresent();

        Player player = playerOptional.get();

        for (Player p : Sponge.getServer().getOnlinePlayers()) {
            if (p.getWorld().getUniqueId().equals(worldID)) {
                p.sendBlockChange(player.getLocation().getBlockPosition(), blockState);
            }
        }
    }

    private void resetBlockChanges() {
        Optional<Player> playerOptional = Sponge.getServer().getPlayer(owner);
        assert playerOptional.isPresent();

        Player player = playerOptional.get();

        for (Player p : Sponge.getServer().getOnlinePlayers()) {
            if (p.getWorld().getUniqueId().equals(worldID)) {
                p.resetBlockChange(player.getLocation().getBlockPosition());
            }
        }
    }

    // Disposes of this disguise
    public void dispose() {
        if (state == State.MOBILE) {
            removeEntities();
        } else { // state == State.SOLID
            resetBlockChanges();
        }
    }

    public Optional<User> getOwnerUser() {
        Optional<Player> onlinePlayer = Sponge.getServer().getPlayer(owner);
        if (onlinePlayer.isPresent()) {
            return Optional.of(onlinePlayer.get()); // Due to Optionals not recognizing polymorphism (Player inherits from User)
        }

        Optional<UserStorageService> userStorageService = Sponge.getServiceManager().provide(UserStorageService.class);
        return userStorageService.get().get(owner);
    }

    public Optional<String> getOwnerName() {
        Optional<User> user = getOwnerUser();
        if (user.isPresent()) {
            return Optional.of(user.get().getName());
        }
        return Optional.empty();
    }

    // Checks if a given UUID matches one in use for this disguise
    public boolean isAssociatedWith(UUID uuid) {
        return owner.equals(uuid) || armorStand.equals(uuid) || fallingBlock.equals(uuid) || worldID.equals(uuid);
    }

    public State getState() {
        return state;
    }

    public UUID getOwner() {
        return owner;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public UUID getArmorStand() {
        return armorStand;
    }

    public UUID getFallingBlock() {
        return fallingBlock;
    }

    public UUID getWorld() {
        return worldID;
    }
}
