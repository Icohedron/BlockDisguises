package io.github.icohedron.blockdisguises.data;

import io.github.icohedron.blockdisguises.BlockDisguises;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;

import java.util.Optional;
import java.util.UUID;

public class DisguiseOwnerDataBuilder extends AbstractDataBuilder<DisguiseOwnerData> implements DataManipulatorBuilder<DisguiseOwnerData, ImmutableDisguiseOwnerData> {

    public static final int CONTENT_VERSION = 0;

    public DisguiseOwnerDataBuilder() {
        super(DisguiseOwnerData.class, CONTENT_VERSION);
    }

    @Override
    public DisguiseOwnerData create() {
        return new DisguiseOwnerData();
    }

    @Override
    public Optional<DisguiseOwnerData> createFrom(DataHolder dataHolder) {
        return create().fill(dataHolder);
    }

    @Override
    protected Optional<DisguiseOwnerData> buildContent(DataView container) throws InvalidDataException {
        if (container.contains(BlockDisguises.DISGUISE_OWNER)) {
            UUID owner = (UUID) container.get(BlockDisguises.DISGUISE_OWNER.getQuery()).get();
            return Optional.of(new DisguiseOwnerData(owner));
        }
        return Optional.empty();
    }
}
