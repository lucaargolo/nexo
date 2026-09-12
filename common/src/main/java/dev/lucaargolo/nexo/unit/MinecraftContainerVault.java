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
    public int maxStackAmount() {
        return this.container.getMaxStackSize();
    }

    @Override
    public int slots() {
        return this.container.getContainerSize();
    }

    @Override
    public @NotNull ItemUnit get(int slot) {
        Objects.checkIndex(slot, this.slots());
        ItemStack stack = this.container.getItem(slot);
        return stack.isEmpty() ? this.empty : this.nexo.stackToUnit(stack.copy());
    }

    @Override
    public @NotNull ItemUnit set(int slot, @NotNull ItemUnit value) {
        Objects.checkIndex(slot, this.slots());
        if (!(value instanceof MinecraftItemUnit unit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " only accepts MinecraftItemUnit instances");
        }

        ItemStack stack = unit.get().copy();
        if (!stack.isEmpty()) {
            if (!this.canInsert(slot, value)) {
                throw new IllegalArgumentException(this.getClass().getSimpleName() + " rejected item for slot " + slot);
            }
            int limit = this.maxStackAmount(slot, value);
            if (stack.getCount() > limit) {
                throw new IllegalArgumentException(this.getClass().getSimpleName() + " rejected item count " + stack.getCount() + " for slot " + slot + " (max " + limit + ")");
            }
        } else if (!this.container.getItem(slot).isEmpty() && !this.canExtract(slot)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " rejected removal from slot " + slot);
        }

        ItemUnit previous = this.get(slot);
        this.container.setItem(slot, stack);
        this.container.setChanged();
        return previous;
    }

    @Override
    public void changed() {
        this.container.setChanged();
    }

    @Override
    public boolean canInsert() {
        for (int slot = 0; slot < this.slots(); slot++) {
            if (this.canInsert(slot)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canInsert(int slot) {
        Objects.checkIndex(slot, this.slots());
        return this.container.getMaxStackSize() > 0;
    }

    @Override
    public boolean canInsert(@NotNull ItemUnit value) {
        for (int slot = 0; slot < this.slots(); slot++) {
            if (this.canInsert(slot, value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canInsert(int slot, @NotNull ItemUnit value) {
        Objects.checkIndex(slot, this.slots());
        if (!(value instanceof MinecraftItemUnit unit) || unit.get().isEmpty()) {
            return false;
        }
        return this.canInsert(slot) && this.container.canPlaceItem(slot, unit.get());
    }

    @Override
    public int maxStackAmount(int slot, @NotNull ItemUnit value) {
        Objects.checkIndex(slot, this.slots());
        return value instanceof MinecraftItemUnit unit ? Math.min(this.maxStackAmount(slot), unit.get().getMaxStackSize()) : 0;
    }

    @Override
    public boolean canExtract() {
        for (int slot = 0; slot < this.slots(); slot++) {
            if (this.canExtract(slot)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot) {
        Objects.checkIndex(slot, this.slots());
        ItemStack stack = this.container.getItem(slot);
        return !stack.isEmpty() && this.container.canTakeItem(this.container, slot, stack);
    }

    @Override
    public @NotNull ItemUnit clear(int slot) {
        Objects.checkIndex(slot, this.slots());
        if (this.isEmpty(slot) || !this.canExtract(slot)) {
            return this.empty;
        }
        return this.set(slot, this.empty);
    }

    @Override
    public void clear() {
        for (int slot = 0; slot < this.slots(); slot++) {
            this.clear(slot);
        }
    }

    public static @Nullable <U extends Unit<?>> Vault<U> create(@NotNull NexoMinecraft nexo, @NotNull Class<U> type, @Nullable Container container) {
        if (container == null) {
            return null;
        }
        if (type != ItemUnit.class) {
            throw new IllegalArgumentException("Tried to create non ItemUnit MinecraftContainerVault");
        }
        return Nexo.<Vault<U>>type(Vault.class).cast(new MinecraftContainerVault(nexo, container));
    }

}