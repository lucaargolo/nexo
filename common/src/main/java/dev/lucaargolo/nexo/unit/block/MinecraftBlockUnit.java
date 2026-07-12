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

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

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
    @SuppressWarnings("unchecked")
    public <D> D getData(@NotNull DataBase<D> data) {
        if(data instanceof DataBase.Constrained<?> constrained) {
            Property<?> property = find(constrained);
            return (D) this.state.getValue(property);
        }
        throw new IllegalArgumentException("Tried to get non-constrained data " + data + " from MinecraftBlockUnit");
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        if(data instanceof DataBase.Constrained<?> constrained) {
            Property<?> property = find(constrained);
            this.state = this.state.setValue(property, (Comparable) d);
            return;
        }
        throw new IllegalArgumentException("Tried to set non-constrained data " + data + " to MinecraftBlockUnit");
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> Property<T> find(DataBase.Constrained<?> data) {
        for(Property<?> property : this.state.getProperties()) {
            if(!property.getName().equals(data.name())) continue;
            if(!property.getValueClass().equals(data.dataType())) continue;
            if(!same(property.getPossibleValues(), data.values())) continue;
            if(!match(property, data)) continue;
            return (Property<T>) property;
        }
        throw new IllegalArgumentException("Couldn't find constrained data " + data + " in MinecraftBlockUnit");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> boolean match(Property<?> property, DataBase.Constrained<?> data) {
        Property<T> typedProperty = (Property<T>) property;
        DataBase.Constrained<T> typedData = (DataBase.Constrained<T>) data;
        for(T o : typedProperty.getPossibleValues()) {
            if(!Objects.equals(typedProperty.getName(o), typedData.serialize(o))) {
                return false;
            }
        }
        return true;
    }

    private static boolean same(Collection<?> c1, Collection<?> c2) {
        if (c1.size() != c2.size()) {
            return false;
        }

        Iterator<?> it1 = c1.iterator();
        Iterator<?> it2 = c2.iterator();
        while (it1.hasNext()) {
            if (!Objects.equals(it1.next(), it2.next())) {
                return false;
            }
        }

        return true;
    }

}
