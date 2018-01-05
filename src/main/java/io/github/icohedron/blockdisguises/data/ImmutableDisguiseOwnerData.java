package io.github.icohedron.blockdisguises.data;

import io.github.icohedron.blockdisguises.BlockDisguises;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableSingleData;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import java.util.UUID;

public class ImmutableDisguiseOwnerData extends AbstractImmutableSingleData<UUID, ImmutableDisguiseOwnerData, DisguiseOwnerData> implements ImmutableDataManipulator<ImmutableDisguiseOwnerData, DisguiseOwnerData> {

    public ImmutableDisguiseOwnerData(UUID value) {
        super(value, BlockDisguises.DISGUISE_OWNER);
    }

    public ImmutableDisguiseOwnerData() {
        this(null);
    }

    @Override
    protected ImmutableValue<?> getValueGetter() {
        return Sponge.getRegistry().getValueFactory()
                .createValue(BlockDisguises.DISGUISE_OWNER, getValue(), null)
                .asImmutable();
    }

    @Override
    public DisguiseOwnerData asMutable() {
        return new DisguiseOwnerData(getValue());
    }

    @Override
    public int getContentVersion() {
        return DisguiseOwnerDataBuilder.CONTENT_VERSION;
    }

    @Override
    public DataContainer toContainer() {
        DataContainer container = super.toContainer();
        container.set(BlockDisguises.DISGUISE_OWNER.getQuery(), getValue());
        return container;
    }
}
