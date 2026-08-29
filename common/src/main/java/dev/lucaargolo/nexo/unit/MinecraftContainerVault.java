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

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class MinecraftContainerVault extends AbstractCollection<ItemUnit<?>> implements Vault<ItemUnit<?>> {

    public static final String KEY = "inventory";

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    private final @NotNull Container container;

    private MinecraftContainerVault(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Container container) {
        this.nexo = nexo;
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
    public boolean isFull() {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int size() {
        int size = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                size++;
            }
        }
        return size;
    }

    @Override
    public boolean contains(@Nullable Object object) {
        if (!(object instanceof MinecraftItemUnit<?> item)) {
            return false;
        }
        ItemStack target = item.get();
        if (target.isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (ItemStack.matches(container.getItem(slot), target)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(@NotNull ItemUnit<?> item) {
        if (!(item instanceof MinecraftItemUnit<?> minecraftItem)) {
            throw new IllegalArgumentException("MinecraftContainerVault only accepts MinecraftItemUnit instances");
        }
        ItemStack source = minecraftItem.get();
        if (source.isEmpty()) {
            return false;
        }

        int remaining = source.getCount();
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, source) || !container.canPlaceItem(slot, source)) {
                continue;
            }
            int amount = Math.min(remaining, container.getMaxStackSize(current) - current.getCount());
            if (amount <= 0) {
                continue;
            }
            ItemStack updated = current.copy();
            updated.grow(amount);
            container.setItem(slot, updated);
            remaining -= amount;
        }
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            if (!container.getItem(slot).isEmpty() || !container.canPlaceItem(slot, source)) {
                continue;
            }
            int amount = Math.min(remaining, container.getMaxStackSize(source));
            container.setItem(slot, source.copyWithCount(amount));
            remaining -= amount;
        }
        if (remaining == source.getCount()) {
            return false;
        }
        container.setChanged();
        return true;
    }

    @Override
    public boolean remove(@Nullable Object object) {
        if (!(object instanceof MinecraftItemUnit<?> item)) {
            return false;
        }
        ItemStack target = item.get();
        if (target.isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!ItemStack.matches(container.getItem(slot), target)) {
                continue;
            }
            container.removeItemNoUpdate(slot);
            container.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        if (!container.isEmpty()) {
            container.clearContent();
            container.setChanged();
        }
    }

    @Override
    public @NotNull Iterator<ItemUnit<?>> iterator() {
        return new Iterator<>() {
            private int nextSlot;
            private int currentSlot = -1;

            @Override
            public boolean hasNext() {
                while (nextSlot < container.getContainerSize() && container.getItem(nextSlot).isEmpty()) {
                    nextSlot++;
                }
                return nextSlot < container.getContainerSize();
            }

            @Override
            public @NotNull ItemUnit<?> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                currentSlot = nextSlot++;
                return nexo.stackToUnit(container.getItem(currentSlot));
            }

            @Override
            public void remove() {
                if (currentSlot < 0) {
                    throw new IllegalStateException();
                }
                container.removeItemNoUpdate(currentSlot);
                container.setChanged();
                currentSlot = -1;
            }
        };
    }

}
