package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MinecraftContainerVault extends MinecraftItemVault {

    public static final String KEY = "inventory";

    private final @NotNull Container container;

    private MinecraftContainerVault(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Container container) {
        super(nexo);
        this.container = container;
    }

    public static boolean supports(@NotNull Class<?> type) {
        return type.isAssignableFrom(MinecraftItemUnit.class);
    }

    public static @Nullable <U extends Unit<?, ?>> Vault<U> create(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @Nullable Container container,
            @NotNull Class<U> type
    ) {
        if (container == null || !supports(type)) {
            return null;
        }
        Class<Vault<U>> clazz = Nexo.type(Vault.class);
        return clazz.cast(new MinecraftContainerVault(nexo, container));
    }

    @Override
    int slotCount() {
        return this.container.getContainerSize();
    }

    @Override
    @NotNull ItemStack getItem(int slot) {
        return this.container.getItem(slot);
    }

    @Override
    void setItem(int slot, @NotNull ItemStack stack) {
        this.container.setItem(slot, stack);
    }

    @Override
    boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return this.container.canPlaceItem(slot, stack);
    }

    @Override
    int slotLimit(int slot) {
        return this.container.getMaxStackSize(this.container.getItem(slot));
    }

    @Override
    public void contentsChanged() {
        super.contentsChanged();
        this.container.setChanged();
    }

}
