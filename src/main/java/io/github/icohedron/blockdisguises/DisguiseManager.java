package io.github.icohedron.blockdisguises;

import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DisguiseManager {

    private Map<UUID, Disguise> disguises; // <Player UUID, Player Name>

    // Configuration variables
    private int solidifyDelay; // Amount of delay, in game ticks, before a disguise turns into a solid block

    public DisguiseManager(ConfigurationNode config) {
        disguises = new HashMap<>();
        updateConfig(config);
    }

    public void updateConfig(ConfigurationNode config) {
        solidifyDelay = config.getNode("solidify_delay").getInt(60);
    }

    public void disguise(Player player, BlockState blockState) {
        disguises.put(player.getUniqueId(), new Disguise(player, blockState));
    }

    public void undisguise(UUID player) {
        Disguise disguise = disguises.remove(player);
        assert disguise != null;
        disguise.dispose();
    }

    public void undisguiseAll() {
        for (UUID disguised : disguises.keySet()) {
            undisguise(disguised);
        }
    }

    public void sendBlockChanges(Player player) {
        disguises.entrySet().forEach(uuidDisguiseEntry -> uuidDisguiseEntry.getValue().sendBlockChange(player));
    }

    // public void sendBlockChangesOptimally(Player player, Location<World> from, Location<World> to) {
    //     disguises.entrySet().forEach(uuidDisguiseEntry -> uuidDisguiseEntry.getValue().sendBlockChangeOptimally(player, from, to));
    // }

    public boolean isDisguised(UUID uuid) {
        return disguises.containsKey(uuid);
    }

    public Disguise getDisguise(UUID uuid) {
        return disguises.get(uuid);
    }

    public int numDisguised() {
        return disguises.size();
    }

    public Set<UUID> getAllDisguised() {
        return disguises.keySet();
    }

    public int getSolidifyDelay() {
        return solidifyDelay;
    }
}
