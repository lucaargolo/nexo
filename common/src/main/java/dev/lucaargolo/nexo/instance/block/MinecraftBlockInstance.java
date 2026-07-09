package dev.lucaargolo.nexo.instance.block;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.block.NexoBlock;
import dev.lucaargolo.nexo.api.feature.data.NexoData;
import dev.lucaargolo.nexo.api.instance.block.BlockInstance;
import dev.lucaargolo.nexo.instance.MinecraftInstance;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public class MinecraftBlockInstance extends BlockInstance implements MinecraftInstance<BlockState> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private BlockState state;

    public MinecraftBlockInstance(@NotNull NexoMinecraft nexo, @NotNull NexoBlock feature, @NotNull BlockState state) {
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public @Nullable <D> D getData(@NotNull NexoData<D> data) {
        if(data instanceof NexoData.Constrained<?> constrained) {
            Property property = find(constrained);
            return (D) this.state.getValue(property);
        }
        throw new IllegalArgumentException("Tried to get non-constrained data " + data + " from BlockInstance");
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <D> void setData(@NotNull NexoData<D> data, @Nullable D d) {
        if(data instanceof NexoData.Constrained<?> constrained) {
            Property property = find(constrained);
            this.state = this.state.setValue(property, (Comparable) d);
            return;
        }
        throw new IllegalArgumentException("Tried to set non-constrained data " + data + " to BlockInstance");
    }

    @NotNull
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Comparable<T>> Property<T> find(NexoData.Constrained<?> data) {
        for(Property<?> property : this.state.getProperties()) {
            if(!property.getName().equals(data.name())) continue;
            if(!property.getValueClass().equals(data.dataType())) continue;
            if(!same(property.getPossibleValues(), data.values())) continue;
            boolean sameSerializer = true;
            for(Comparable<?> o : property.getPossibleValues()) {
                String s1 = ((Property) property).getName(o);
                String s2 = ((NexoData.Constrained) data).serialize(o);
                if(!Objects.equals(s1, s2)) {
                    sameSerializer = false;
                };
            }
            if(!sameSerializer) continue;
            return (Property<T>) property;
        }
        throw new IllegalArgumentException("Couldn't find non-constrained data " + data + " in BlockInstance");
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
