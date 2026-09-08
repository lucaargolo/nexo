package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MinecraftContainerVault extends MinecraftItemVault {

    private final @NotNull Container container;

    private MinecraftContainerVault(@NotNull NexoMinecraft nexo, @NotNull Container container) {
        super(nexo);
        this.container = container;
    }

    @Override
    public int size() {
        return this.container.getContainerSize();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return this.container.getItem(slot);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        this.container.setItem(slot, stack);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return this.container.canPlaceItem(slot, stack);
    }

    @Override
    public int slotLimit(int slot) {
        return this.container.getMaxStackSize();
    }

    @Override
    public void contentsChanged() {
        super.contentsChanged();
        this.container.setChanged();
    }

    public static @Nullable MinecraftContainerVault create(@NotNull NexoMinecraft nexo, @Nullable Container container) {
        if (container == null) {
            return null;
        }
        return new MinecraftContainerVault(nexo, container);
    }

}
