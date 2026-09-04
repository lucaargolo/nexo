package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class NeoForgeItemHandlerVault extends AbstractList<ItemUnit> implements Vault<ItemUnit> {

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    private final @NotNull ItemUnit defaultValue;
    final @NotNull IItemHandler handler;

    public NeoForgeItemHandlerVault(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull IItemHandler handler) {
        this.nexo = nexo;
        this.defaultValue = MinecraftItemUnit.empty(nexo);
        this.handler = handler;
    }

    @Override
    public @NotNull ItemUnit defaultValue() {
        return this.defaultValue;
    }

    @Override
    public @NotNull ItemUnit get(int slot) {
        Objects.checkIndex(slot, this.handler.getSlots());
        ItemStack stack = this.handler.getStackInSlot(slot);
        return stack.isEmpty() ? this.defaultValue() : this.nexo.stackToUnit(stack.copy());
    }

    @Override
    public @NotNull ItemUnit set(int slot, @NotNull ItemUnit item) {
        Objects.checkIndex(slot, this.handler.getSlots());
        if (!(item instanceof MinecraftItemUnit minecraftItem)) {
            throw new IllegalArgumentException("NeoForgeItemHandlerVault only accepts MinecraftItemUnit instances");
        }
        ItemStack stack = minecraftItem.get().copy();
        if (!stack.isEmpty() && (!this.handler.isItemValid(slot, stack) || stack.getCount() > this.handler.getSlotLimit(slot))) {
            throw new IllegalArgumentException("NeoForge item handler rejected item for slot " + slot);
        }
        ItemUnit previous = this.get(slot);
        ItemStack previousStack = this.handler.getStackInSlot(slot).copy();
        if (!previousStack.isEmpty()) {
            int remaining = previousStack.getCount();
            while (remaining > 0) {
                ItemStack extracted = this.handler.extractItem(slot, remaining, false);
                if (extracted.isEmpty()) {
                    throw new IllegalArgumentException("NeoForge item handler rejected item for slot " + slot);
                }
                remaining -= extracted.getCount();
            }
        }
        ItemStack remaining = stack.isEmpty() ? ItemStack.EMPTY : this.handler.insertItem(slot, stack, false);
        if (!remaining.isEmpty()) {
            if (!previousStack.isEmpty()) {
                this.handler.insertItem(slot, previousStack, false);
            }
            throw new IllegalArgumentException("NeoForge item handler rejected item for slot " + slot);
        }
        this.contentsChanged();
        return previous;
    }

    public static @NotNull <U extends Unit<?>> Set<String> vaults(@NotNull Set<String> existing, @NotNull Class<U> type, @Nullable IItemHandler handler) {
        if (handler == null || !MinecraftContainerVault.supports(type)) {
            return existing;
        }
        Set<String> vaults = new HashSet<>(existing);
        vaults.add(MinecraftContainerVault.KEY);
        return Set.copyOf(vaults);
    }

    public static @Nullable <U extends Unit<?>> Vault<U> create(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Class<U> type, @Nullable IItemHandler handler) {
        if (handler == null || !MinecraftContainerVault.supports(type)) {
            return null;
        }
        Class<Vault<U>> clazz = Nexo.type(Vault.class);
        return clazz.cast(new NeoForgeItemHandlerVault(nexo, handler));
    }

    @Override
    public boolean isFull() {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int size() {
        return this.handler.getSlots();
    }

    @Override
    public boolean contains(Object object) {
        if (!(object instanceof MinecraftItemUnit item)) {
            return false;
        }
        ItemStack target = item.get();
        if (target.isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (ItemStack.matches(handler.getStackInSlot(slot), target)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(@NotNull ItemUnit item) {
        if (!(item instanceof MinecraftItemUnit minecraftItem)) {
            throw new IllegalArgumentException("NeoForgeItemHandlerVault only accepts MinecraftItemUnit instances");
        }
        ItemStack source = minecraftItem.get();
        if (source.isEmpty()) {
            return false;
        }
        ItemStack remaining = source.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
        }
        boolean inserted = remaining.getCount() < source.getCount();
        if (inserted) {
            this.contentsChanged();
        }
        return inserted;
    }

    @Override
    public boolean remove(Object object) {
        if (!(object instanceof MinecraftItemUnit item)) {
            return false;
        }
        ItemStack target = item.get();
        if (target.isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!ItemStack.matches(handler.getStackInSlot(slot), target)) {
                continue;
            }
            boolean removed = ItemStack.matches(handler.extractItem(slot, target.getCount(), false), target);
            if (removed) {
                this.contentsChanged();
            }
            return removed;
        }
        return false;
    }

    @Override
    public @NotNull ItemUnit remove(int slot) {
        Objects.checkIndex(slot, this.handler.getSlots());
        ItemUnit previous = this.get(slot);
        ItemStack stack = this.handler.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            int remaining = stack.getCount();
            while (remaining > 0) {
                ItemStack extracted = this.handler.extractItem(slot, remaining, false);
                if (extracted.isEmpty()) {
                    throw new IllegalStateException("NeoForge item handler rejected vault indexed removal");
                }
                remaining -= extracted.getCount();
            }
            this.contentsChanged();
        }
        return previous;
    }

    @Override
    public void clear() {
        boolean changed = false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            while (!stack.isEmpty()) {
                ItemStack extracted = handler.extractItem(slot, stack.getCount(), false);
                if (extracted.isEmpty()) {
                    break;
                }
                changed = true;
                stack = handler.getStackInSlot(slot);
            }
        }
        if (changed) {
            this.contentsChanged();
        }
    }

}
