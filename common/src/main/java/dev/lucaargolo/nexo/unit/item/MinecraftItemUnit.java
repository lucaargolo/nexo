package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public abstract class MinecraftItemUnit<C extends Role> extends ItemUnit<C> implements MinecraftUnit<ItemStack> {

    protected final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    private @NotNull ItemStack stack;

    public MinecraftItemUnit(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull ItemBase feature,
            @Nullable C role,
            @NotNull ItemStack stack
    ) {
        super(nexo, feature, role);
        this.nexo = nexo;
        this.stack = stack;
    }

    @Override
    public @NotNull <U extends Unit<?, ?>> Set<String> vaults(@NotNull Class<U> type) {
        return this.itemVault() != null && MinecraftContainerVault.supports(type) ? Set.of(MinecraftContainerVault.KEY) : Set.of();
    }

    @Override
    public @Nullable <U extends Unit<?, ?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        if (!MinecraftContainerVault.KEY.equals(key) || !MinecraftContainerVault.supports(type)) {
            return null;
        }
        Vault<ItemUnit<?>> vault = this.itemVault();
        if (vault == null) {
            return null;
        }
        Class<Vault<U>> clazz = Nexo.type(Vault.class);
        return clazz.cast(vault);
    }

    protected @Nullable Vault<ItemUnit<?>> itemVault() {
        return null;
    }

    protected void setStack(@NotNull ItemStack stack) {
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
            return;
        }
        stack.set(component, value);
    }

    private static <D> @NotNull DataComponentType<D> find(@NotNull DataBase<D> data) {
        Class<DataComponentType<D>> clazz = Nexo.type(DataComponentType.class);
        return clazz.cast(MinecraftFeatureType.DATA.convert(data));
    }
}
