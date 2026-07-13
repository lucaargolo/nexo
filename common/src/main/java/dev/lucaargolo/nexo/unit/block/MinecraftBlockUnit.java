package dev.lucaargolo.nexo.unit.block;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class MinecraftBlockUnit extends BlockUnit implements MinecraftUnit<BlockState> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private BlockState state;

    public MinecraftBlockUnit(@NotNull NexoMinecraft nexo, @NotNull BlockBase feature, @NotNull BlockState state) {
        super(feature);
        this.nexo = nexo;
        this.state = state;
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
    }

    public @NotNull BlockState get() {
        return this.state;
    }

    @Override
    public <D> D getData(@NotNull DataBase<D> data) {
        if(data instanceof DataBase.Constrained<?> constrained) {
            return data.cast(getData(constrained));
        }
        throw new IllegalArgumentException("Tried to get non-constrained data " + data + " from MinecraftBlockUnit");
    }

    private <C extends Comparable<C>> C getData(@NotNull DataBase.Constrained<C> data) {
        Property<C> property = find(data);
        return this.state.getValue(property);
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        if(data instanceof DataBase.Constrained<?> constrained) {
            this.state = setData(constrained, d);
            return;
        }
        throw new IllegalArgumentException("Tried to set non-constrained data " + data + " to MinecraftBlockUnit");
    }


    private <C extends Comparable<C>> BlockState setData(@NotNull DataBase.Constrained<C> data, Object value) {
        Property<C> property = find(data);
        return this.state.setValue(property, data.cast(value));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> Property<T> find(DataBase.Constrained<T> data) {
        for(Property<?> property : this.state.getProperties()) {
            if(!property.getName().equals(data.name())) continue;
            if(!property.getValueClass().equals(data.valueClass())) continue;
            if(different(property.getPossibleValues(), data.values())) continue;
            if(different(serializedValues(property), serializedValues(data))) continue;
            return (Property<T>) property;
        }
        throw new IllegalArgumentException("Couldn't find constrained data " + data + " in MinecraftBlockUnit");
    }

    private static <T extends Comparable<T>> Collection<String> serializedValues(Property<T> property) {
        return property.getPossibleValues().stream().map(property::getName).toList();
    }

    private static <T extends Comparable<T>> Collection<String> serializedValues(DataBase.Constrained<T> property) {
        return property.values().stream().map(property::toString).toList();
    }

    private static boolean different(Collection<?> c1, Collection<?> c2) {
        if (c1.size() != c2.size()) {
            return true;
        }

        Iterator<?> it1 = c1.iterator();
        Iterator<?> it2 = c2.iterator();
        while (it1.hasNext()) {
            if (!Objects.equals(it1.next(), it2.next())) {
                return true;
            }
        }

        return false;
    }

}
