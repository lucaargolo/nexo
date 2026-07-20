package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MinecraftItemUnit extends ItemUnit<Role> implements MinecraftUnit<ItemStack> {

    private final @NotNull ItemStack stack;

    public MinecraftItemUnit(
            @NotNull ItemBase feature,
            @Nullable Role role,
            @NotNull ItemStack stack
    ) {
        super(feature, role);
        this.stack = stack;
    }

    @Override
    public @NotNull ItemStack get() {
        return stack;
    }

    @Override
    public <D> @Nullable D getData(@NotNull DataBase<D> data) {
        return stack.get(find(data));
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D value) {
        DataComponentType<D> component = find(data);
        if (value == null) {
            stack.remove(component);
        } else {
            stack.set(component, value);
        }
    }

    @SuppressWarnings("unchecked")
    private static <D> @NotNull DataComponentType<D> find(@NotNull DataBase<D> data) {
        return (DataComponentType<D>) MinecraftFeatureType.DATA.convert(data);
    }
}
