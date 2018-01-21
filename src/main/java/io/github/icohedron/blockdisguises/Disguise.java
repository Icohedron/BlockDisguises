package io.github.icohedron.blockdisguises;

import com.flowpowered.math.vector.Vector3i;
import io.github.icohedron.blockdisguises.data.DisguiseOwnerData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.UUID;

public class Disguise {

    // Possible feature: xp bar counts down when the player stops moving so they know when they turn solid

    public enum State {
        MOBILE, SOLID, NONE
    } // MOBILE - when the player is moving, SOLID - when the player is not moving, NONE - the player disconnected

    private DisguiseManager disguiseManager;

    private State state; // Current state of this disguise
    private UUID owner; // Disguise owner, as a player's UUID
    private BlockState blockState; // The block that this disguise represents

    private Task solidifyTask; // Task for solidifying the player after a few moments

    // UUIDs of the armor stand and falling block making the moving disguise
    private UUID armorStand;
    private UUID fallingBlock;

    // UUID of the world this disguise is in
    private Location<World> lastLocation; // Last known location of this disguise

    // Creates a disguise for a given online player
    public Disguise(Player player, BlockState blockState) {
        disguiseManager = BlockDisguises.getInstance().getDisguiseManager();

        state = State.MOBILE;
        owner = player.getUniqueId();
        this.blockState = blockState;
        lastLocation = player.getLocation();

        createEntities(player);
        createSolidifyTask();
    }

    public void createEntities(Player player) {
        assert player.getUniqueId().equals(owner);

        World world = lastLocation.getExtent();
        assert world != null;

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
        World world = lastLocation.getExtent();
        if (world == null) {
            return; // Entites will be removed by the chunkload event in the near future
        }

        if (armorStand != null) {
            Optional<Entity> entity = world.getEntity(armorStand);
            entity.ifPresent(Entity::remove);
        }

        if (fallingBlock != null) {
            Optional<Entity> entity = world.getEntity(fallingBlock);
            entity.ifPresent(Entity::remove);
        }
    }

    public void sendBlockChange(Player player) {
        if (state != State.SOLID) {
            return; // No block change to send
        }

        // Prevents the player from being pushed away by their own block
        if (player.getUniqueId().equals(owner)) {
            return;
        }

        if (player.getWorld().getUniqueId().equals(getWorld())) {

            Vector3i playerBlockPosition = player.getLocation().getBlockPosition();
            Vector3i thisBlockPosition = lastLocation.getBlockPosition();

            if (playerBlockPosition.distance(thisBlockPosition) <= player.getViewDistance() * 16) {
                player.sendBlockChange(lastLocation.getBlockPosition(), blockState);
            }
        }
    }

    public void sendBlockChangeOptimally(Player player, Location<World> from, Location<World> to) {
        if (state != State.SOLID) {
            return; // No block change to send
        }

        // Prevents the player from being pushed away by their own block
        if (player.getUniqueId().equals(owner)) {
            return;
        }

        if (from.getExtent().getUniqueId().equals(getWorld())) {

            Vector3i thisBlockPosition = lastLocation.getBlockPosition();
            Vector3i fromBlockPosition = from.getBlockPosition();
            Vector3i toBlockPosition = to.getBlockPosition();

            int viewDistance = player.getViewDistance() * 16; // View distance in blocks

            float distFrom = fromBlockPosition.distance(thisBlockPosition);
            float distTo = toBlockPosition.distance(thisBlockPosition);

            if (distFrom > viewDistance && distTo <= viewDistance) {
                player.sendBlockChange(lastLocation.getBlockPosition(), blockState);
            }
        }
    }

    public void sendBlockChanges() {
        if (state != State.SOLID) {
            return; // No block change to send
        }

        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            // Prevents the player from being pushed away by their own block
            if (player.getUniqueId().equals(owner)) {
                return;
            }

            if (player.getWorld().getUniqueId().equals(getWorld())) {

                Vector3i playerBlockPosition = player.getLocation().getBlockPosition();
                Vector3i thisBlockPosition = lastLocation.getBlockPosition();

                if (playerBlockPosition.distance(thisBlockPosition) <= player.getViewDistance() * 16) {
                    player.sendBlockChange(lastLocation.getBlockPosition(), blockState);
                }
            }
        }
    }

    private void resetBlockChanges(Vector3i blockPosition) {
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            if (player.getWorld().getUniqueId().equals(getWorld())) {
                Vector3i playerBlockPosition = player.getLocation().getBlockPosition();

                if (playerBlockPosition.distance(blockPosition) <= player.getViewDistance() * 16) {
                    player.resetBlockChange(blockPosition);
                }
            }
        }
    }

    private void resetBlockChanges() {
        resetBlockChanges(lastLocation.getBlockPosition());
    }

    private void cancelSolidifyTask() {
        if (solidifyTask != null) {
            solidifyTask.cancel();
        }
    }

    private void createSolidifyTask() {
        solidifyTask = Task.builder().delayTicks(disguiseManager.getSolidifyDelay()).execute(this::setSolid).submit(BlockDisguises.getInstance());
    }

    private void resetSolidifyTask() {
        cancelSolidifyTask();
        createSolidifyTask();
    }

    private void setMobile(Player player) {
        assert player.getUniqueId().equals(owner);
        if (state == State.MOBILE) {
            return; // Already in MOBILE state
        }

        resetBlockChanges();
        state = State.MOBILE;
        createEntities(player);
        resetSolidifyTask();
    }

    private void setSolid() {
        if (state == State.SOLID) {
            return; // Already in SOLID state
        }

        Optional<User> user = getOwnerUser();
        assert user.isPresent() && user.get().getPlayer().isPresent();
        Player player = user.get().getPlayer().get();

        // The player must be in an air block to be set solid
        if (!lastLocation.getExtent().getBlock(lastLocation.getBlockPosition()).getType().equals(BlockTypes.AIR)) {
            resetSolidifyTask();
            player.sendMessage(Text.of(BlockDisguises.getInstance().getTextPrefix(), TextColors.RED, "You may not turn solid here!"));
            return;
        }

        player.sendMessage(Text.of(BlockDisguises.getInstance().getTextPrefix(), TextColors.YELLOW, "You have become solid!"));

        removeEntities();
        state = State.SOLID;
//        sendBlockChanges();
    }

    private void setNone() {
        removeEntities();
        resetBlockChanges();
        cancelSolidifyTask();
        state = State.NONE;
    }

    public void moveCallback(MoveEntityEvent event, Player player) { // Call this function when the disguise owner moves.
        assert player.getUniqueId().equals(owner);

        Location<World> from = event.getFromTransform().getLocation();
        Location<World> to = event.getToTransform().getLocation();

        if (!from.getExtent().getUniqueId().equals(to.getExtent().getUniqueId())) {
            disguiseManager.undisguise(owner);
            player.sendMessage(Text.of(BlockDisguises.getInstance().getTextPrefix(), TextColors.RED, "Undisguised due to changing worlds/dimensions!"));
            return;
        }

        boolean blockLocationChanged = !from.getExtent().getUniqueId().equals(to.getExtent().getUniqueId()) // If the worlds between positions are not the same
                || !from.getBlockPosition().equals(to.getBlockPosition());              // or if the block location of the player changed

        if (state == State.SOLID) {
            if (blockLocationChanged) {
                resetBlockChanges(from.getBlockPosition());
                setMobile(player);
                player.sendMessage(Text.of(BlockDisguises.getInstance().getTextPrefix(), TextColors.RED, "You are no longer solid!"));
            }
        } else if (state == State.MOBILE) {
            if (blockLocationChanged) {
                resetSolidifyTask();
            }

            Optional<Entity> armorStandEntity = from.getExtent().getEntity(armorStand);
            assert armorStandEntity.isPresent();

            Task.builder().execute(() ->
                armorStandEntity.get().setLocation(to)
            ).submit(BlockDisguises.getInstance());
        }

        lastLocation = to;
    }


    public void clientJoinCallback(ClientConnectionEvent.Join event, Player player) {
        assert player.getUniqueId().equals(owner);
        setMobile(player);
    }

    public void clientDisconnectCallback(ClientConnectionEvent.Disconnect event, Player player) {
        assert player.getUniqueId().equals(owner);
        setNone();
    }

    // Disposes of this disguise
    public void dispose() {
        removeEntities();
        cancelSolidifyTask();
        resetBlockChanges();
    }

    public Optional<User> getOwnerUser() {
        Optional<Player> onlinePlayer = Sponge.getServer().getPlayer(owner);
        if (onlinePlayer.isPresent()) {
            return Optional.of(onlinePlayer.get()); // Due to Optionals not recognizing polymorphism (Player inherits from User)
        }

        Optional<UserStorageService> userStorageService = Sponge.getServiceManager().provide(UserStorageService.class);
        assert userStorageService.isPresent();
        return userStorageService.get().get(owner);
    }

    public Optional<String> getOwnerName() {
        Optional<User> user = getOwnerUser();
        return user.map(User::getName);
    }

    // Checks if a given UUID matches one in use for this disguise
    public boolean isAssociatedWith(UUID uuid) {
        return owner.equals(uuid) || armorStand.equals(uuid) || fallingBlock.equals(uuid) || getWorld().equals(uuid);
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

    public Location<World> getLocation() {
        return lastLocation.copy();
    }

    public UUID getWorld() {
        return lastLocation.getExtent().getUniqueId();
    }
}
