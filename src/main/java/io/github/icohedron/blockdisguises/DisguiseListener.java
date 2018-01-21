package io.github.icohedron.blockdisguises;

import com.flowpowered.math.vector.Vector3i;
import io.github.icohedron.blockdisguises.data.DisguiseOwnerData;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.entity.*;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DisguiseListener {

    private BlockDisguises blockDisguises;
    private DisguiseManager disguiseManager;
    private Map<UUID, DamageSource> lastDamageSource;

    public DisguiseListener() {
        blockDisguises = BlockDisguises.getInstance();
        disguiseManager = blockDisguises.getDisguiseManager();
        lastDamageSource = new HashMap<>();
    }

    private boolean isKillable(Player player) {
        Value<GameMode> gameModeValue = player.gameMode();
        if (gameModeValue.exists()) {
            GameMode gameMode = gameModeValue.get();
            if (gameMode.equals(GameModes.SURVIVAL) || gameMode.equals(GameModes.ADVENTURE)) {
                return true;
            }
        }
        return false;
    }

    @Listener
    public void onSpawnEntityChunkLoadEvent(SpawnEntityEvent.ChunkLoad event) {

        // Remove any entities that were created by this plugin and are no longer in use
        List<Entity> entities = event.getEntities();
        for (Entity e : entities) {

            // Check if the entity has a DisguiseOwnerData data manipulator
            Optional<DisguiseOwnerData> disguiseOwnerDataOptional = e.get(DisguiseOwnerData.class);
            if (disguiseOwnerDataOptional.isPresent()) {

                // If so, remove this entity if the disguise owner isn't disguised
                DisguiseOwnerData disguiseOwnerData = disguiseOwnerDataOptional.get();
                UUID disguiseOwner = disguiseOwnerData.get(BlockDisguises.DISGUISE_OWNER).get();

                // Remove the entity if it is no longer in use
                if (!disguiseManager.isDisguised(disguiseOwner)) { // No corresponding disguise associated with this entity
                    e.remove();
                } else {
                    Disguise disguise = disguiseManager.getDisguise(disguiseOwner);
                    if (!disguise.isAssociatedWith(e.getUniqueId())) { // This is an entity no longer associated with this disguise
                        e.remove();
                    }
                }
            }

        }

    }

    @Listener
    public void onEntityDestruct(DestructEntityEvent.Death event) {
        if (event.getTargetEntity() instanceof Player) {
            Player player = (Player) event.getTargetEntity();

            if (disguiseManager.isDisguised(player.getUniqueId())) {
                disguiseManager.undisguise(player.getUniqueId());
            }
         }
    }

    @Listener
    public void onClientJoin(ClientConnectionEvent.Join event, @First Player player) {
        UUID uuid = player.getUniqueId();
        if (disguiseManager.isDisguised(uuid)) {
            Disguise disguise = disguiseManager.getDisguise(uuid);
            disguise.clientJoinCallback(event, player);
        }

//        disguiseManager.sendBlockChanges(player);
    }

    @Listener
    public void onClientDisconnect(ClientConnectionEvent.Disconnect event, @First Player player) {
        UUID uuid = player.getUniqueId();
        if (disguiseManager.isDisguised(uuid)) {
            Disguise disguise = disguiseManager.getDisguise(uuid);
            disguise.clientDisconnectCallback(event, player);
        }
    }

    @Listener
    public void onMoveEvent(MoveEntityEvent event, @First Player player) {
        // disguiseManager.sendBlockChanges(player);
//        disguiseManager.sendBlockChangesOptimally(player, event.getFromTransform().getLocation(), event.getToTransform().getLocation());

        UUID uuid = player.getUniqueId();
        if (disguiseManager.isDisguised(uuid)) {
            Disguise disguise = disguiseManager.getDisguise(uuid);
            disguise.moveCallback(event, player);
        }
    }

    @Listener
    public void onDamageTaken(DamageEntityEvent event) {
        if (event.getTargetEntity() instanceof Player) {
            Player player = (Player) event.getTargetEntity();
            if (disguiseManager.isDisguised(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onInteractEntity(InteractEntityEvent event, @First Player player) {
        if (disguiseManager.isDisguised(player.getUniqueId())) {
            return;
        }

        Entity entity = event.getTargetEntity();
        if (entity instanceof Player && disguiseManager.isDisguised(entity.getUniqueId())) {
            Player targetPlayer = (Player) entity;

            if (!isKillable(targetPlayer)) {
                return;
            }

            BlockRay<World> blockRay = BlockRay.from(player).distanceLimit(5).build();
            while (blockRay.hasNext()) {
                BlockRayHit<World> blockRayHit = blockRay.next();
                Vector3i blockPos = blockRayHit.getBlockPosition();

                Disguise disguise = disguiseManager.getDisguise(targetPlayer.getUniqueId());
                Location<World> disguiseLoc = disguise.getLocation();
                if (    disguise.getState() == Disguise.State.SOLID
                        && disguiseLoc.getBlockPosition().equals(blockPos)
                        && disguiseLoc.getExtent().getUniqueId().equals(player.getWorld().getUniqueId())) {

                    targetPlayer.offer(Keys.HEALTH, 0.0);
                    player.playSound(SoundTypes.ENTITY_PLAYER_HURT, targetPlayer.getLocation().getPosition(), 1.0);
                    return;
                }
            }

        } else {

            Optional<UUID> disguiseOwner = entity.get(BlockDisguises.DISGUISE_OWNER);
            if (disguiseOwner.isPresent()) {

                if (disguiseManager.isDisguised(disguiseOwner.get())) {

                    Optional<User> user = disguiseManager.getDisguise(disguiseOwner.get()).getOwnerUser();
                    if (user.isPresent()) {
                        Optional<Player> owner = user.get().getPlayer();
                        if (owner.isPresent()) {
                            if (!isKillable(owner.get())) {
                                return;
                            }
                            owner.get().offer(Keys.HEALTH, 0.0);
                            player.playSound(SoundTypes.ENTITY_PLAYER_HURT, owner.get().getLocation().getPosition(), 1.0);
                        }
                    }

                } else {
                    entity.remove();
                }
            }

        }

    }

    @Listener
    public void onInteractBlock(InteractBlockEvent event, @First Player player) {

        if (disguiseManager.isDisguised(player.getUniqueId())) {
            return;
        }

        if (!event.getTargetBlock().getLocation().isPresent()) {
            return;
        }

        Vector3i blockPos = event.getTargetBlock().getPosition();

        for (UUID disguised : disguiseManager.getAllDisguised()) {

            Disguise disguise = disguiseManager.getDisguise(disguised);
            Location<World> disguiseLoc = disguise.getLocation();
            if (    disguise.getState() == Disguise.State.SOLID
                    && disguiseLoc.getBlockPosition().equals(blockPos)
                    && disguiseLoc.getExtent().getUniqueId().equals(event.getTargetBlock().getWorldUniqueId())) {

//                disguise.sendBlockChange(player);

                if (!disguiseManager.isDisguised(player.getUniqueId())) {
                    assert disguise.getOwnerUser().get().getPlayer().isPresent();
                    Player disguiseOwner = disguise.getOwnerUser().get().getPlayer().get();

                    if (!isKillable(disguiseOwner)) {
                        return;
                    }

                    disguiseOwner.offer(Keys.HEALTH, 0.0);
                    player.playSound(SoundTypes.ENTITY_PLAYER_HURT, player.getLocation().getPosition(), 1.0);
                }

                break;
            }

        }
    }
}
