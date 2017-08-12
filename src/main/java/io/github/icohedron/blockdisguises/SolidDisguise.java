package io.github.icohedron.blockdisguises;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.block.BlockState;

import java.util.UUID;

public class SolidDisguise {

    private UUID owner;
    private UUID world;
    private Vector3i location;
    private BlockState blockState;

    public SolidDisguise(UUID owner, UUID world, Vector3i location, BlockState blockState) {
        this.owner = owner;
        this.world = world;
        this.location = location;
        this.blockState = blockState;
    }

    public UUID getOwner() {
        return owner;
    }

    public UUID getWorld() {
        return world;
    }

    public Vector3i getLocation() {
        return location;
    }

    public BlockState getBlockState() {
        return blockState;
    }
}
