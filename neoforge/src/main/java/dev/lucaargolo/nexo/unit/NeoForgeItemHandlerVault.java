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

import java.util.AbstractCollection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public final class NeoForgeItemHandlerVault extends AbstractCollection<ItemUnit<?>> implements Vault<ItemUnit<?>> {

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    final @NotNull IItemHandler handler;

    public NeoForgeItemHandlerVault(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull IItemHandler handler) {
        this.nexo = nexo;
        this.handler = handler;
    }

    public static @NotNull <U extends Unit<?, ?>> Set<String> vaults(@NotNull Set<String> existing, @NotNull Class<U> type, @Nullable IItemHandler handler) {
        if (handler == null || !MinecraftContainerVault.supports(type)) {
            return existing;
        }
        Set<String> vaults = new HashSet<>(existing);
        vaults.add(MinecraftContainerVault.KEY);
        return Set.copyOf(vaults);
    }

    public static @Nullable <U extends Unit<?, ?>> Vault<U> create(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Class<U> type, @Nullable IItemHandler handler) {
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
        int size = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) {
                size++;
            }
        }
        return size;
    }

    @Override
    public boolean contains(Object object) {
        if (!(object instanceof MinecraftItemUnit<?> item)) {
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
    public boolean add(@NotNull ItemUnit<?> item) {
        if (!(item instanceof MinecraftItemUnit<?> minecraftItem)) {
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
        if (!(object instanceof MinecraftItemUnit<?> item)) {
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

    @Override
    public @NotNull Iterator<ItemUnit<?>> iterator() {
        return new Iterator<>() {
            private int nextSlot;
            private int currentSlot = -1;
            private int currentAmount;

            @Override
            public boolean hasNext() {
                while (nextSlot < handler.getSlots() && handler.getStackInSlot(nextSlot).isEmpty()) {
                    nextSlot++;
                }
                return nextSlot < handler.getSlots();
            }

            @Override
            public @NotNull ItemUnit<?> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                currentSlot = nextSlot++;
                ItemStack stack = handler.getStackInSlot(currentSlot);
                currentAmount = stack.getCount();
                return nexo.stackToUnit(stack.copy());
            }

            @Override
            public void remove() {
                if (currentSlot < 0) {
                    throw new IllegalStateException();
                }
                handler.extractItem(currentSlot, currentAmount, false);
                NeoForgeItemHandlerVault.this.contentsChanged();
                currentSlot = -1;
            }
        };
    }

}
