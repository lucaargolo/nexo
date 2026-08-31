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

import java.util.ArrayList;
import java.util.List;
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
        if (!MinecraftContainerVault.supports(type)) {
            return super.vaults(type);
        }
        return this.itemVault() != null ? Set.of(MinecraftContainerVault.KEY) : super.vaults(type);
    }

    @Override
    public @Nullable <U extends Unit<?, ?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        if (!MinecraftContainerVault.supports(type) || !MinecraftContainerVault.KEY.equals(key)) {
            return super.vault(type, key);
        }
        Vault<ItemUnit<?>> vault = this.itemVault();
        if (vault == null) {
            return super.vault(type, key);
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
