package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class NeoForgeItemHandlerVault extends AbstractCollection<ItemUnit<?>> implements Vault<ItemUnit<?>> {

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    final @NotNull IItemHandler handler;

    public NeoForgeItemHandlerVault(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull IItemHandler handler) {
        this.nexo = nexo;
        this.handler = handler;
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
        return remaining.getCount() < source.getCount();
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
            return ItemStack.matches(handler.extractItem(slot, target.getCount(), false), target);
        }
        return false;
    }

    @Override
    public void clear() {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            while (!stack.isEmpty()) {
                ItemStack extracted = handler.extractItem(slot, stack.getCount(), false);
                if (extracted.isEmpty()) {
                    break;
                }
                stack = handler.getStackInSlot(slot);
            }
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
                currentSlot = -1;
            }
        };
    }

}
