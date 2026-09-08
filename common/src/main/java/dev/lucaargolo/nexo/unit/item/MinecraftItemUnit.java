package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.data.MinecraftData;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class MinecraftItemUnit extends ItemUnit implements MinecraftUnit<ItemStack> {

    protected final @NotNull NexoMinecraft nexo;
    private final @NotNull ItemStack stack;

    public MinecraftItemUnit(
            @NotNull NexoMinecraft nexo,
            @NotNull ItemBase feature,
            @Nullable Role role,
            @NotNull ItemStack stack
    ) {
        super(nexo, feature, role);
        this.nexo = nexo;
        this.stack = stack;
    }

    @Override
    public @NotNull <U extends Unit<?>> Set<String> vaults(@NotNull Class<U> type) {
        if(type == ItemUnit.class) {
            if (this.vault() != null) {
                return Set.of("inventory");
            }
        }
        return Set.of();
    }

    @Override
    public @Nullable <U extends Unit<?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        Class<Vault<U>> vaultType = Nexo.type(Vault.class);
        if(type == ItemUnit.class) {
            if (key.equals("inventory")) {
                return this.vault() != null ? vaultType.cast(this.vault()) : null;
            }
        }
        return null;
    }


    protected @Nullable Vault<ItemUnit> vault() {
        return null;
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

    public static @NotNull ItemUnit empty(@NotNull NexoMinecraft nexo) {
        ItemBase item = nexo.getFeature(Feature.Type.ITEM, Location.of("minecraft", "air"));
        if(item == null) {
            throw new IllegalStateException("Couldn't find empty item");
        }
        return new MinecraftItemUnit(nexo, item, null, ItemStack.EMPTY) {};
    }

    private static <D> @NotNull DataComponentType<D> find(@NotNull DataBase<D> data) {
        if (data instanceof MinecraftData<D> minecraftData) {
            return minecraftData.component();
        }
        Class<DataComponentType<D>> clazz = Nexo.type(DataComponentType.class);
        return clazz.cast(MinecraftFeatureType.DATA.convert(data));
    }

}
