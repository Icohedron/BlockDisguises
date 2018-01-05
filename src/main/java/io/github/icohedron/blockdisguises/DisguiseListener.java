package io.github.icohedron.blockdisguises;

import io.github.icohedron.blockdisguises.data.DisguiseOwnerData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.FallingBlock;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.arrow.Arrow;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

import java.util.*;

public class DisguiseListener {

    private BlockDisguises blockDisguises;
    private DisguiseManager disguiseManager;
    private Map<UUID, DamageSource> lastDamageSource;

    public DisguiseListener() {
        blockDisguises = BlockDisguises.getInstance();
        disguiseManager = blockDisguises.getDisguiseManager();
        lastDamageSource = new HashMap<>();
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
//        if (event.getTargetEntity() instanceof Player) {
//            Player player = (Player) event.getTargetEntity();
//
//            if (blockDisguises.isDisguised(player)) {
//                blockDisguises.undisguise(player);
//
//                if (lastDamageSource.containsKey(player.getUniqueId())) {
//                    if (lastDamageSource.get(player.getUniqueId()) instanceof EntityDamageSource) {
//                        EntityDamageSource damageSource = (EntityDamageSource) lastDamageSource.get(player.getUniqueId());
//                        if (damageSource.getSource() instanceof Player && damageSource.getType().equals(DamageTypes.CUSTOM)) {
//                            event.clearMessage();
//                        }
//                    }
//                }
//            }
//        }
//        } else if (blockDisguises.isMarkedForRemoval(event.getTargetEntity().getUniqueId())) {
//            blockDisguises.confirmRemoval(event.getTargetEntity().getUniqueId());
//        }
    }

    @Listener
    public void onDamageEntity(DamageEntityEvent event) {
//        if (event.willCauseDeath() && event.getTargetEntity() instanceof Player) {
//
//            Player victim = (Player) event.getTargetEntity();
//            lastDamageSource.put(victim.getUniqueId(), event.getCause().first(DamageSource.class).get());
//
//            if (blockDisguises.isDisguised(victim)) {
//
//                Optional<EntityDamageSource> entityDamageSource = event.getCause().first(EntityDamageSource.class);
//                if (entityDamageSource.isPresent()) {
//
//                    EntityDamageSource damageSource = entityDamageSource.get();
//                    if (damageSource.getSource() instanceof Player) {
//
//                        if (entityDamageSource.get().getType().equals(DamageTypes.CUSTOM)) {
//                            Player source = (Player) damageSource.getSource();
//                            String[] deathMessage = new String[] {
//                                    " was slain by ",
//                                    " was slaughtered by ",
//                                    " faced the wrath of ",
//                                    " was killed by ",
//                                    " got finished off by "
//                            };
//                            Sponge.getServer().getBroadcastChannel().send(Text.of(victim.getName(), deathMessage[new Random().nextInt(deathMessage.length)], source.getName()));
//                        }
//
//                    }
//
//                }
//
//            }
//
//        }
    }

    @Listener
    public void onClientJoin(ClientConnectionEvent.Join event, @First Player player) {
//        if (blockDisguises.isDisguised(player)) {
//            blockDisguises.vanishPlayer(player);
//            blockDisguises.getDisguiseData(player).ifPresent(disguiseData -> {
//                disguiseData.setDisguiseEntityData(blockDisguises.createDisguiseEntities(player, disguiseData.getBlockState()));
//                if (blockDisguises.disguisedHasAction("turn_solid")) {
//                    disguiseData.resetSolidifyTask();
//                }
//            });
//        }
//
//        blockDisguises.sendBlockChanges(player);
    }

    @Listener
    public void onClientDisconnect(ClientConnectionEvent.Disconnect event, @First Player player) {
//        if (blockDisguises.isDisguised(player)) {
//            blockDisguises.unsolidify(player);
//            blockDisguises.unvanishPlayer(player);
//            blockDisguises.getDisguiseData(player).ifPresent(disguiseData -> {
//                disguiseData.setDisguiseEntityData(null);
//                disguiseData.getSolidifyTask().cancel();
//            });
//        }
    }

    @Listener
    public void onMoveEvent(MoveEntityEvent event, @First Player player) {
//        blockDisguises.sendBlockChanges(player);
//
//        blockDisguises.getDisguiseData(player).ifPresent(disguiseData -> {
//
//            disguiseData.getDisguiseEntityData().ifPresent(disguiseEntityData -> {
//                World world = player.getWorld();
//
//                if (!world.getUniqueId().equals(disguiseEntityData.getWorld())) {
//                    blockDisguises.undisguise(player);
//                    return;
//                }
//
//                Optional<Entity> armorStand = world.getEntity(disguiseEntityData.getArmorStand());
//                armorStand.ifPresent(entity -> entity.setLocation(player.getLocation()));
//            });
//
//            if (blockDisguises.disguisedHasAction("turn_solid")) {
//                if (!disguiseData.getLastLocation().equals(player.getLocation().getBlockPosition())) {
//                    disguiseData.resetSolidifyTask();
//                    disguiseData.setLastLocation(player.getLocation().getBlockPosition());
//                }
//            }
//
//        });
    }

    @Listener
    public void onInteractEntity(InteractEntityEvent.Primary event, @First Player player) {
//
//        // Prevent dealing damage to the disguised player directly. Not really necessary since hiders are vanished, but why not leave this here anyway.
//        if (event.getTargetEntity() instanceof Player) {
//            Player target = (Player) event.getTargetEntity();
//            if (blockDisguises.isDisguised(target)) {
//                event.setCancelled(true);
//                return;
//            }
//        }
//
//        // Damage the disguised player if their block disguise entity was hit
//        if (event.getTargetEntity() instanceof FallingBlock) {
//            if (blockDisguises.isDisguised(player) && !blockDisguises.disguisedHasAction("attack_other_disguised")) {
//                event.setCancelled(true);
//                return;
//            }
//
//            FallingBlock fallingBlock = (FallingBlock) event.getTargetEntity();
//            blockDisguises.getDisguiseData(fallingBlock).ifPresent(disguiseData -> {
//                // Shouldn't be able to damage yourself by punching your own disguise!
//                if (disguiseData.getOwner().equals(player.getUniqueId())) {
//                    event.setCancelled(true);
//                    return;
//                }
//
//                blockDisguises.damagePlayer(disguiseData.getOwner(), player);
//            });
//
//        } else if (blockDisguises.isDisguised(player)) {
//            if (!blockDisguises.disguisedHasAction("attack_other_entities")) {
//                event.setCancelled(true);
//                return;
//            }
//        }
    }

    @Listener
    public void onInteractBlock(InteractBlockEvent.Primary event, @First Player player) {
//        for (SolidDisguise solidDisguise : blockDisguises.getSolidDisguises()) {
//
//            if (event.getTargetBlock().getWorldUniqueId().equals(solidDisguise.getWorld())) {
//
//                if (solidDisguise.getLocation().equals(event.getTargetBlock().getPosition())) {
//                    if (blockDisguises.isDisguised(player) && !blockDisguises.disguisedHasAction("attack_other_disguised")) {
//                        // Do nothing
//                    } else {
//                        if (blockDisguises.disguisedHasAction("unsolidify_when_attacked")) {
//                            blockDisguises.unsolidify(solidDisguise.getOwner());
//                        }
//                        blockDisguises.damagePlayer(solidDisguise.getOwner(), player);
//                    }
//
//                    event.setCancelled(true);
//                    return;
//                }
//
//            }
//
//        }
    }

    @Listener(order = Order.POST)
    public void onPostInteractBlock(InteractBlockEvent.Primary event, @First Player player) {
//        blockDisguises.sendBlockChanges(player);
    }

    @Listener
    public void onEntityCollision(CollideEntityEvent event) {
//        Arrow arrow = null;
//        FallingBlock fallingBlock = null;
//
//        for (Entity entity : event.getEntities()) {
//            if (entity instanceof Player) { // Do not collide with disguised players
//                Player player = (Player) entity;
//                if (blockDisguises.isDisguised(player)) {
//                    event.setCancelled(true);
//                    return;
//                }
//            } else if (entity instanceof Arrow) {
//                arrow = (Arrow) entity;
//            } else if (entity instanceof FallingBlock) {
//                fallingBlock = (FallingBlock) entity;
//            }
//        }
//
//        if (arrow != null && fallingBlock != null) {
//            if (arrow.getCreator().isPresent()) {
//
//                Optional<Player> playerOptional = Sponge.getServer().getPlayer(arrow.getCreator().get());
//                if (playerOptional.isPresent()) {
//                    Player player = playerOptional.get();
//                    final Arrow arrowFinal = arrow;
//
//                    blockDisguises.getDisguiseData(fallingBlock).ifPresent(disguiseData -> {
//                        // Shouldn't be able to damage yourself by shooting your own disguise!
//                        if (disguiseData.getOwner().equals(player.getUniqueId())) {
//                            event.setCancelled(true);
//                            return;
//                        }
//
//                        blockDisguises.damagePlayer(disguiseData.getOwner(), player);
//                        player.playSound(SoundTypes.ENTITY_ARROW_HIT_PLAYER, player.getLocation().getPosition(), 1.0);
//                        arrowFinal.remove();
//                    });
//
//                }
//
//            }
//        }
    }
}
