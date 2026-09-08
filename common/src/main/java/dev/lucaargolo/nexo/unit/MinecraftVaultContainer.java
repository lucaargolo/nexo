package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MinecraftVaultContainer implements Container {

    private final @NotNull NexoMinecraft nexo;
    private final @NotNull Vault<ItemUnit> vault;

    public MinecraftVaultContainer(@NotNull NexoMinecraft nexo, @NotNull Vault<ItemUnit> vault) {
        this.nexo = nexo;
        this.vault = vault;
    }

    @Override
    public int getContainerSize() {
        return this.vault.size();
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < this.getContainerSize(); slot++) {
            if (!this.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (slot < 0 || slot >= this.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        ItemUnit item = this.vault.get(slot);
        return item instanceof MinecraftItemUnit minecraftItem ? minecraftItem.get() : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= this.getContainerSize() || amount <= 0 || !this.vault.canRemove()) {
            return ItemStack.EMPTY;
        }
        if (this.vault instanceof MinecraftItemVault minecraftVault) {
            return minecraftVault.extractItem(slot, amount, false);
        }
        ItemStack current = this.getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int extracted = Math.min(amount, current.getCount());
        ItemStack result = current.copyWithCount(extracted);
        this.setItem(slot, current.copyWithCount(current.getCount() - extracted));
        return result;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        Objects.checkIndex(slot, this.getContainerSize());
        if (!this.vault.canRemove()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = this.getItem(slot);
        if (this.vault instanceof MinecraftItemVault minecraftVault) {
            return minecraftVault.extractItem(slot, current.getCount(), false, false);
        }
        if (current.isEmpty()) {
            this.vault.remove(slot);
            return ItemStack.EMPTY;
        }
        this.vault.set(slot, this.vault.defaultValue());
        return current;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        Objects.checkIndex(slot, this.getContainerSize());
        ItemStack value = stack.copy();
        value.limitSize(this.getMaxStackSize(value));
        this.vault.set(slot, value.isEmpty() ? this.vault.defaultValue() : this.nexo.stackToUnit(value));
    }

    @Override
    public void setChanged() {
        this.vault.contentsChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= this.getContainerSize() || stack.isEmpty() || !this.vault.canAdd()) {
            return false;
        }
        if (this.vault instanceof MinecraftItemVault minecraftVault) {
            return minecraftVault.isItemValid(slot, stack);
        }
        ItemStack current = this.getItem(slot);
        return current.isEmpty() ? !this.vault.isFull() : ItemStack.isSameItemSameComponents(current, stack);
    }

    @Override
    public boolean canTakeItem(@NotNull Container container, int slot, @NotNull ItemStack stack) {
        return slot >= 0 && slot < this.getContainerSize() && this.vault.canRemove();
    }

    @Override
    public void clearContent() {
        if (this.vault.canRemove()) {
            this.vault.clear();
        }
    }

}
