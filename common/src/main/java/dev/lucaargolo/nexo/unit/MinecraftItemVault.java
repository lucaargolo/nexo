package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

abstract class MinecraftItemVault extends AbstractList<ItemUnit> implements Vault<ItemUnit> {

    protected final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    private final @NotNull ItemUnit defaultValue;

    MinecraftItemVault(@NotNull NexoMinecraft<?, ?, ?, ?> nexo) {
        this.nexo = nexo;
        this.defaultValue = MinecraftItemUnit.empty(nexo);
    }

    abstract int slotCount();

    abstract @NotNull ItemStack getItem(int slot);

    abstract void setItem(int slot, @NotNull ItemStack stack);

    abstract boolean isItemValid(int slot, @NotNull ItemStack stack);

    abstract int slotLimit(int slot);

    @Override
    public final @NotNull ItemUnit defaultValue() {
        return this.defaultValue;
    }

    @Override
    public final @NotNull ItemUnit get(int slot) {
        Objects.checkIndex(slot, this.slotCount());
        ItemStack stack = this.getItem(slot);
        return stack.isEmpty() ? this.defaultValue() : this.nexo.stackToUnit(stack);
    }

    @Override
    public final @NotNull ItemUnit set(int slot, @NotNull ItemUnit item) {
        Objects.checkIndex(slot, this.slotCount());
        if (!(item instanceof MinecraftItemUnit minecraftItem)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " only accepts MinecraftItemUnit instances");
        }
        ItemStack stack = minecraftItem.get().copy();
        if (!stack.isEmpty() && (!this.isItemValid(slot, stack) || stack.getCount() > Math.min(this.slotLimit(slot), stack.getMaxStackSize()))) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " rejected item for slot " + slot);
        }
        ItemUnit previous = this.get(slot);
        this.setItem(slot, stack);
        this.contentsChanged();
        return previous;
    }

    @Override
    public final boolean isFull() {
        for (int slot = 0; slot < this.slotCount(); slot++) {
            if (this.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final int size() {
        return this.slotCount();
    }

    @Override
    public final boolean contains(@Nullable Object object) {
        if (!(object instanceof MinecraftItemUnit item) || item.get().isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < this.slotCount(); slot++) {
            if (ItemStack.matches(this.getItem(slot), item.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final boolean add(@NotNull ItemUnit item) {
        if (!(item instanceof MinecraftItemUnit minecraftItem)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " only accepts MinecraftItemUnit instances");
        }
        ItemStack source = minecraftItem.get();
        if (source.isEmpty()) {
            return false;
        }
        int remaining = source.getCount();
        for (int slot = 0; slot < this.slotCount() && remaining > 0; slot++) {
            ItemStack current = this.getItem(slot);
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, source)) {
                continue;
            }
            remaining = this.insertItem(slot, source.copyWithCount(remaining), false, false).getCount();
        }
        for (int slot = 0; slot < this.slotCount() && remaining > 0; slot++) {
            if (!this.getItem(slot).isEmpty()) {
                continue;
            }
            remaining = this.insertItem(slot, source.copyWithCount(remaining), false, false).getCount();
        }
        if (remaining == source.getCount()) {
            return false;
        }
        this.contentsChanged();
        return true;
    }

    @Override
    public final boolean remove(@Nullable Object object) {
        if (!(object instanceof MinecraftItemUnit item) || item.get().isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < this.slotCount(); slot++) {
            if (ItemStack.matches(this.getItem(slot), item.get())) {
                this.setItem(slot, ItemStack.EMPTY);
                this.contentsChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    public final @NotNull ItemUnit remove(int slot) {
        Objects.checkIndex(slot, this.slotCount());
        ItemUnit previous = this.get(slot);
        if (!this.getItem(slot).isEmpty()) {
            this.setItem(slot, ItemStack.EMPTY);
            this.contentsChanged();
        }
        return previous;
    }

    @Override
    public final void clear() {
        boolean changed = false;
        for (int slot = 0; slot < this.slotCount(); slot++) {
            if (!this.getItem(slot).isEmpty()) {
                this.setItem(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            this.contentsChanged();
        }
    }

    @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return this.insertItem(slot, stack, simulate, true);
    }

    @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate, boolean notify) {
        if (stack.isEmpty() || !this.isItemValid(slot, stack)) {
            return stack.copy();
        }
        ItemStack current = this.getItem(slot);
        int limit = Math.min(this.slotLimit(slot), stack.getMaxStackSize());
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) {
            return stack.copy();
        }
        int amount = Math.min(stack.getCount(), Math.max(0, limit - current.getCount()));
        if (amount == 0) {
            return stack.copy();
        }
        if (!simulate) {
            this.setItem(slot, current.isEmpty() ? stack.copyWithCount(amount) : current.copyWithCount(current.getCount() + amount));
            if (notify) {
                this.contentsChanged();
            }
        }
        return stack.copyWithCount(stack.getCount() - amount);
    }

    @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return this.extractItem(slot, amount, simulate, true);
    }

    @NotNull ItemStack extractItem(int slot, int amount, boolean simulate, boolean notify) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack current = this.getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int extracted = Math.min(Math.min(amount, current.getCount()), current.getMaxStackSize());
        ItemStack result = current.copyWithCount(extracted);
        if (!simulate) {
            this.setItem(slot, current.copyWithCount(current.getCount() - extracted));
            if (notify) {
                this.contentsChanged();
            }
        }
        return result;
    }

    @NotNull List<ItemStack> snapshot() {
        List<ItemStack> snapshot = new ArrayList<>(this.slotCount());
        for (int slot = 0; slot < this.slotCount(); slot++) {
            snapshot.add(this.getItem(slot).copy());
        }
        return snapshot;
    }

    void restore(@NotNull List<ItemStack> snapshot, boolean notify) {
        if (snapshot.size() != this.slotCount()) {
            throw new IllegalArgumentException("Vault size changed while restoring " + this.getClass().getSimpleName());
        }
        for (int slot = 0; slot < snapshot.size(); slot++) {
            this.setItem(slot, snapshot.get(slot).copy());
        }
        if (notify) {
            this.contentsChanged();
        }
    }

}
