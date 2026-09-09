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

import java.util.Objects;

public final class MinecraftContainerVault implements Vault.Slotted<ItemUnit> {

    private final @NotNull NexoMinecraft nexo;
    private final @NotNull Container container;
    private final @NotNull ItemUnit empty;

    public MinecraftContainerVault(@NotNull NexoMinecraft nexo, @NotNull Container container) {
        this.nexo = nexo;
        this.container = container;
        this.empty = nexo.stackToUnit(ItemStack.EMPTY);
    }

    @Override
    public @NotNull ItemUnit empty() {
        return this.empty;
    }

    @Override
    public int slots() {
        return this.container.getContainerSize();
    }

    @Override
    public @NotNull ItemUnit get(int slot) {
        Objects.checkIndex(slot, this.slots());
        ItemStack stack = this.container.getItem(slot);
        if (stack.isEmpty()) {
            return this.empty;
        }
        return this.nexo.stackToUnit(stack);
    }

    @Override
    public @NotNull ItemUnit set(int slot, @NotNull ItemUnit value) {
        Objects.checkIndex(slot, this.slots());
        if (!(value instanceof MinecraftItemUnit unit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " only accepts MinecraftItemUnit instances");
        }

        ItemStack stack = unit.get();
        if (!stack.isEmpty()) {
            if (!this.container.canPlaceItem(slot, stack)) {
                throw new IllegalArgumentException(this.getClass().getSimpleName() + " rejected item for slot " + slot);
            }

            int limit = Math.min(this.container.getMaxStackSize(), stack.getMaxStackSize());
            if (stack.getCount() > limit) {
                throw new IllegalArgumentException(this.getClass().getSimpleName() + " rejected item count " + stack.getCount() + " for slot " + slot + " (max " + limit + ")");
            }
        }

        ItemUnit previous = this.get(slot);
        this.container.setItem(slot, stack);
        return previous;
    }

    @Override
    public @NotNull ItemUnit clear(int slot) {
        Objects.checkIndex(slot, this.slots());
        ItemUnit previous = this.get(slot);
        if (!this.container.getItem(slot).isEmpty()) {
            this.container.setItem(slot, ItemStack.EMPTY);
        }
        return previous;
    }

    @Override
    public void clear() {
        for (int slot = 0; slot < this.slots(); slot++) {
            if (!this.container.getItem(slot).isEmpty()) {
                this.container.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    public static @Nullable <U extends Unit<?>> Vault<U> create(@NotNull NexoMinecraft nexo, @NotNull Class<U> type, @Nullable Container container) {
        if(container == null) {
            return null;
        }
        if(type != ItemUnit.class) {
            throw new IllegalArgumentException("Tried to create non ItemUnit MinecraftContainerVault");
        }
        return Nexo.<Vault<U>>type(Vault.class).cast(new MinecraftContainerVault(nexo, container));
    }

}