package dev.lucaargolo.nexo.feature.block;

import dev.lucaargolo.nexo.api.feature.data.DataBase;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public class DataProperty<D extends Comparable<D>> extends Property<D> {

    private final DataBase.Constrained<D> constrained;

    protected DataProperty(DataBase.Constrained<D> constrained) {
        super(constrained.name(), constrained.valueClass());
        this.constrained = constrained;
    }

    @Override
    public @NotNull Collection<D> getPossibleValues() {
        return constrained.values();
    }

    @Override
    public @NotNull String getName(@NotNull D d) {
        return constrained.toString(d);
    }

    @Override
    public @NotNull Optional<D> getValue(@NotNull String s) {
        return constrained.fromString(s);
    }

    public BlockState setDefault(BlockState state) {
        return state.setValue(this, constrained.initial());
    }

    public BlockState setState(BlockState state, D d) {
        return state.setValue(this, d);
    }

}
