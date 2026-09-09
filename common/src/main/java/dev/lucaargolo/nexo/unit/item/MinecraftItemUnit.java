package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.data.MinecraftData;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class MinecraftItemUnit extends ItemUnit implements MinecraftUnit<ItemStack> {

    protected final @NotNull NexoMinecraft nexo;
    private final @NotNull ItemStack stack;

    public MinecraftItemUnit(@NotNull NexoMinecraft nexo, @NotNull ItemBase feature, @Nullable Role role, @NotNull ItemStack stack) {
        super(nexo, feature, role);
        this.nexo = nexo;
        this.stack = stack;
    }

    @Override
    public @NotNull ItemStack get() {
        return stack;
    }

    @Override
    public int maxAmount() {
        return this.stack.getMaxStackSize();
    }

    @Override
    public int amount() {
        return this.stack.getCount();
    }

    @Override
    public void increment(int amount) {
        this.stack.grow(amount);
    }

    @Override
    public void decrement(int amount) {
        this.stack.shrink(amount);
    }

    @Override
    public @NotNull ItemUnit copy() {
        return this.nexo.stackToUnit(stack.copy());
    }

    @Override
    public @NotNull List<@NotNull DataBase<?>> data() {
        List<@NotNull DataBase<?>> list = new ArrayList<>();
        stack.getComponents().forEach(component -> {
            Object entry = stack.getComponentsPatch().get(component.type());
            if(entry != null) {
                list.add(MinecraftFeatureType.DATA.convert(nexo, component.type()));
            }
        });
        return list;
    }

    @Override
    public <D> @Nullable D getData(@NotNull DataBase<D> data) {
        return stack.get(find(data));
    }

    @NotNull
    @Override
    public <D> ItemUnit setData(@NotNull DataBase<D> data, @Nullable D value) {
        DataComponentType<D> component = find(data);
        if (value == null) {
            stack.remove(component);
            return this;
        }
        stack.set(component, value);
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof MinecraftItemUnit unit) {
            return ItemStack.isSameItemSameComponents(this.stack, unit.stack);
        }else{
            return false;
        }
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.stack);
    }

    private static <D> @NotNull DataComponentType<D> find(@NotNull DataBase<D> data) {
        if (data instanceof MinecraftData<D> minecraftData) {
            return minecraftData.component();
        }
        Class<DataComponentType<D>> clazz = Nexo.type(DataComponentType.class);
        return clazz.cast(MinecraftFeatureType.DATA.convert(data));
    }

}
