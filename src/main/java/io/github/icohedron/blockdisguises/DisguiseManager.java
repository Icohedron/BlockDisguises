package io.github.icohedron.blockdisguises;

import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.block.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DisguiseManager {

    private Map<UUID, Disguise> disguises; // <Player UUID, Player Name>

    // Configuration variables
    private int solidifyDelay; // Amount of delay, in game ticks, before a disguise turns into a solid block
    private double damageDealtToDisguised;
    private boolean disguisedTurnSolid;
    private boolean disguisedUnsolidifyWhenAttacked;
    private boolean disguisedCanAttackOtherEntities;
    private boolean disguisedCanAttackOtherDisguised;

    public DisguiseManager(ConfigurationNode config) {
        disguises = new HashMap<>();
        updateConfig(config);
    }

    public void updateConfig(ConfigurationNode config) {
        solidifyDelay = config.getNode("solidify_delay").getInt(4);
        damageDealtToDisguised = config.getNode("damage_to_disguised").getDouble(5.0);
        disguisedTurnSolid = config.getNode("disguised_turn_solid").getBoolean(true);
        disguisedUnsolidifyWhenAttacked = config.getNode("disguised_unsolidify_when_attacked").getBoolean(true);
        disguisedCanAttackOtherEntities = config.getNode("disguised_can_attack_other_entities").getBoolean(true);
        disguisedCanAttackOtherDisguised = config.getNode("disguised_can_attack_other_disguised").getBoolean(false);
    }

    public void disguise(UUID player, BlockState blockState) {
        disguises.put(player, new Disguise(player, blockState));
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
}
