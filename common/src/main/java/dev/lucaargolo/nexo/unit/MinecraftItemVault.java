package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

abstract class MinecraftItemVault extends AbstractCollection<ItemUnit<?>> implements Vault<ItemUnit<?>> {

    protected final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;

    MinecraftItemVault(@NotNull NexoMinecraft<?, ?, ?, ?> nexo) {
        this.nexo = nexo;
    }

    abstract int slotCount();

    abstract @NotNull ItemStack getItem(int slot);

    abstract void setItem(int slot, @NotNull ItemStack stack);

    abstract boolean isItemValid(int slot, @NotNull ItemStack stack);

    abstract int slotLimit(int slot);

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
        int size = 0;
        for (int slot = 0; slot < this.slotCount(); slot++) {
            if (!this.getItem(slot).isEmpty()) {
                size++;
            }
        }
        return size;
    }

    @Override
    public final boolean contains(@Nullable Object object) {
        if (!(object instanceof MinecraftItemUnit<?> item) || item.get().isEmpty()) {
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
    public final boolean add(@NotNull ItemUnit<?> item) {
        if (!(item instanceof MinecraftItemUnit<?> minecraftItem)) {
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
        if (!(object instanceof MinecraftItemUnit<?> item) || item.get().isEmpty()) {
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

    @Override
    public final @NotNull Iterator<ItemUnit<?>> iterator() {
        return new Iterator<>() {
            private int nextSlot;
            private int currentSlot = -1;

            @Override
            public boolean hasNext() {
                while (this.nextSlot < MinecraftItemVault.this.slotCount() && MinecraftItemVault.this.getItem(this.nextSlot).isEmpty()) {
                    this.nextSlot++;
                }
                return this.nextSlot < MinecraftItemVault.this.slotCount();
            }

            @Override
            public @NotNull ItemUnit<?> next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                this.currentSlot = this.nextSlot++;
                return MinecraftItemVault.this.nexo.stackToUnit(MinecraftItemVault.this.getItem(this.currentSlot));
            }

            @Override
            public void remove() {
                if (this.currentSlot < 0) {
                    throw new IllegalStateException();
                }
                MinecraftItemVault.this.setItem(this.currentSlot, ItemStack.EMPTY);
                MinecraftItemVault.this.contentsChanged();
                this.currentSlot = -1;
            }
        };
    }
}
