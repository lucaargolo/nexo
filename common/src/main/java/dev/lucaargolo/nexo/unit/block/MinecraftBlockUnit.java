package dev.lucaargolo.nexo.unit.block;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public abstract class MinecraftBlockUnit<R extends NexoRegistryHandler<?>, C extends Role> extends BlockUnit<C> implements MinecraftUnit<BlockState> {

    protected final @NotNull R helper;

    protected final @Nullable Level level;
    protected final @Nullable BlockPos position;
    protected final @Nullable BlockEntity entity;

    protected @NotNull BlockState state;

    public MinecraftBlockUnit(@NotNull R helper, @NotNull BlockBase feature, @Nullable C role, @Nullable Level level, @Nullable BlockPos position, @NotNull BlockState state, @Nullable BlockEntity entity) {
        super(helper.nexo(), feature, role);
        this.helper = helper;
        this.level = level;
        this.position = position;
        this.state = state;
        this.entity = entity;
    }

    public @NotNull BlockState get() {
        return this.state;
    }

    @Override
    public @Nullable WorldUnit<?> world() {
        return this.level != null ? this.helper.nexo().levelToUnit(this.level) : null;
    }

    @Override
    public @Nullable Vector3i position() {
        return this.position != null ? new Vector3i(this.position.getX(), this.position.getY(), this.position.getZ()) : null;
    }

    protected @NotNull <T extends Comparable<T>> T getStateData(@NotNull DataBase.Constrained<T> data) {
        Property<T> property = find(data);
        return this.state.getValue(property);
    }

    protected <T extends Comparable<T>> BlockState setStateData(@NotNull DataBase.Constrained<T> data, Object value) {
        Property<T> property = find(data);
        return this.state.setValue(property, data.cast(value));
    }

    private @NotNull <T extends Comparable<T>> Property<T> find(@NotNull DataBase.Constrained<T> data) {
        for (Property<?> property : this.state.getProperties()) {
            if (!property.getName().equals(data.name())) continue;
            if (!property.getValueClass().equals(data.valueClass())) continue;
            if (different(property.getPossibleValues(), data.values())) continue;
            if (different(serializedValues(property), serializedValues(data))) continue;
            Class<Property<T>> clazz = Nexo.type(Property.class);
            return clazz.cast(property);
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
