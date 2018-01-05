package io.github.icohedron.blockdisguises.data;

import io.github.icohedron.blockdisguises.BlockDisguises;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractSingleData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.Optional;
import java.util.UUID;

// This data is just for tracking which entities were created by this plugin and removing them if unused
public class DisguiseOwnerData extends AbstractSingleData<UUID, DisguiseOwnerData, ImmutableDisguiseOwnerData> implements DataManipulator<DisguiseOwnerData, ImmutableDisguiseOwnerData> {

    public DisguiseOwnerData(UUID value) {
        super(value, BlockDisguises.DISGUISE_OWNER);
    }

    public DisguiseOwnerData() {
        this(null);
    }

    @Override
    protected Value<UUID> getValueGetter() {
        return Sponge.getRegistry().getValueFactory()
                .createValue(BlockDisguises.DISGUISE_OWNER, getValue(), UUID.randomUUID());
    }

    @Override
    public Optional<DisguiseOwnerData> fill(DataHolder dataHolder, MergeFunction overlap) {
        DisguiseOwnerData merged = overlap.merge(this, dataHolder.get(DisguiseOwnerData.class).orElse(null));
        setValue(merged.getValue());
        return Optional.of(this);
    }

    @Override
    public Optional<DisguiseOwnerData> from(DataContainer container) {
        if (container.contains(BlockDisguises.DISGUISE_OWNER)) {
            UUID owner = (UUID) container.get(BlockDisguises.DISGUISE_OWNER.getQuery()).get();
            return Optional.of(setValue(owner));
        }
        return Optional.empty();
    }

    @Override
    public DisguiseOwnerData copy() {
        return new DisguiseOwnerData(getValue());
    }

    @Override
    public ImmutableDisguiseOwnerData asImmutable() {
        return new ImmutableDisguiseOwnerData(getValue());
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
