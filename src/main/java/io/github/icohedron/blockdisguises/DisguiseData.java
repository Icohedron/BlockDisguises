package io.github.icohedron.blockdisguises;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DisguiseData {

    private UUID owner;
    private BlockState blockState;
    private DisguiseEntityData disguiseEntityData;

    private Vector3i lastLocation;
    private Task solidifyTask;

    public DisguiseData(UUID owner, BlockState blockState, DisguiseEntityData disguiseEntityData) {
        this.owner = owner;
        this.blockState = blockState;
        this.disguiseEntityData = disguiseEntityData;

        Optional<Player> playerOptional = Sponge.getServer().getPlayer(owner);
        if (playerOptional.isPresent()) {
            Player player = playerOptional.get();
            this.lastLocation = player.getLocation().getBlockPosition();
        } else {
            this.lastLocation = new Vector3i();
        }

        this.solidifyTask = createSolidifyTask();
    }

    public void resetSolidifyTask() {
        BlockDisguises.getInstance().unsolidify(owner);
        solidifyTask.cancel();
        solidifyTask = createSolidifyTask();
    }

    private Task createSolidifyTask() {
        return Task.builder().execute(() -> BlockDisguises.getInstance().solidify(owner)).async().delay(BlockDisguises.getInstance().getSolidifyDelay(), TimeUnit.SECONDS).submit(BlockDisguises.getInstance());
    }

    public UUID getOwner() {
        return owner;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public Optional<DisguiseEntityData> getDisguiseEntityData() {
        return Optional.ofNullable(disguiseEntityData);
    }

    private void removeDisguiseEntities() {
        if (disguiseEntityData == null) {
            return;
        }

        Sponge.getServer().getWorld(disguiseEntityData.getWorld()).ifPresent(world -> {
            Optional<Entity> armorStand = world.getEntity(disguiseEntityData.getArmorStand());
            armorStand.ifPresent(Entity::remove);

            final UUID armorStandUUID = UUID.fromString(disguiseEntityData.getArmorStand().toString());
            // Allow a buffer time of 1 tick for the entity to be removed. If this task did not complete, it means the server crashed or stopped (or just failed to remove the entity in the 1 tick). If that has occurred, the entity will be removed on the next server start
            Task.builder().execute(() -> BlockDisguises.getInstance().untrack(armorStandUUID)).delayTicks(1).async().submit(BlockDisguises.getInstance());

            Optional<Entity> fallingBlock = world.getEntity(disguiseEntityData.getFallingBlock());
            fallingBlock.ifPresent(Entity::remove);

            final UUID fallingBlockUUID = UUID.fromString(disguiseEntityData.getFallingBlock().toString());
            Task.builder().execute(() -> BlockDisguises.getInstance().untrack(fallingBlockUUID)).delayTicks(1).async().submit(BlockDisguises.getInstance());
        });
    }

    public void setDisguiseEntityData(DisguiseEntityData disguiseEntityData) {
        removeDisguiseEntities();
        this.disguiseEntityData = disguiseEntityData;
    }

    public Vector3i getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Vector3i lastLocation) {
        this.lastLocation = lastLocation;
    }

    public Task getSolidifyTask() {
        return solidifyTask;
    }
}
