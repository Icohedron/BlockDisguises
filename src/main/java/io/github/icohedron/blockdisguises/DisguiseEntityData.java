package io.github.icohedron.blockdisguises;

import java.util.UUID;

public class DisguiseEntityData {

    private UUID world;

    private UUID armorStand;
    private UUID fallingBlock;

    public DisguiseEntityData(UUID world, UUID armorStand, UUID fallingBlock) {
        this.world = world;
        this.armorStand = armorStand;
        this.fallingBlock = fallingBlock;
    }

    public UUID getArmorStand() {
        return armorStand;
    }

    public UUID getFallingBlock() {
        return fallingBlock;
    }

    public UUID getWorld() {
        return world;
    }
}
