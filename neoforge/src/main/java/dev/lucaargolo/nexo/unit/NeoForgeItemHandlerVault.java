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

import java.util.Objects;

public final class NeoForgeItemHandlerVault implements Vault.Slotted<ItemUnit> {

    private final @NotNull NexoMinecraft nexo;
    private final @NotNull IItemHandler handler;
    private final @NotNull ItemUnit empty;

    public NeoForgeItemHandlerVault(@NotNull NexoMinecraft nexo, @NotNull IItemHandler handler) {
        this.nexo = nexo;
        this.handler = handler;
        this.empty = nexo.stackToUnit(ItemStack.EMPTY);
    }

    @Override
    public @NotNull ItemUnit empty() {
        return this.empty;
    }

    @Override
    public int slots() {
        return this.handler.getSlots();
    }

    @Override
    public @NotNull ItemUnit get(int slot) {
        Objects.checkIndex(slot, this.slots());
        ItemStack stack = this.handler.getStackInSlot(slot);
        return stack.isEmpty() ? this.empty : this.nexo.stackToUnit(stack.copy());
    }

    @Override
    public @NotNull ItemUnit set(int slot, @NotNull ItemUnit value) {
        Objects.checkIndex(slot, this.slots());

        ItemStack previous = this.handler.getStackInSlot(slot).copy();
        ItemStack replacement = this.stack(value);
        if (!previous.isEmpty()) {
            ItemStack extracted = this.handler.extractItem(slot, previous.getCount(), false);
            if (extracted.getCount() != previous.getCount()) {
                this.restore(slot, previous);
                throw new IllegalStateException("Could not clear NeoForge item handler slot " + slot);
            }
        }

        if (!replacement.isEmpty()) {
            ItemStack remaining = this.handler.insertItem(slot, replacement, false);
            if (!remaining.isEmpty()) {
                this.restore(slot, previous);
                throw new IllegalStateException("Could not set NeoForge item handler slot " + slot);
            }
        }

        return previous.isEmpty() ? this.empty : this.nexo.stackToUnit(previous);
    }

    @Override
    public boolean canAdd() {
        for (int slot = 0; slot < this.slots(); slot++) {
            if (this.canAdd(slot)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canAdd(int slot) {
        Objects.checkIndex(slot, this.slots());
        return this.handler.getSlotLimit(slot) > 0;
    }

    @Override
    public boolean canAdd(int slot, @NotNull ItemUnit value) {
        Objects.checkIndex(slot, this.slots());
        ItemStack stack = this.stack(value);
        if (stack.isEmpty() || !this.canAdd(slot)) {
            return false;
        }
        return this.handler.insertItem(slot, stack, true).getCount() < stack.getCount();
    }

    @Override
    public boolean canRemove() {
        for (int slot = 0; slot < this.slots(); slot++) {
            if (this.canRemove(slot)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canRemove(int slot) {
        Objects.checkIndex(slot, this.slots());
        ItemStack stack = this.handler.getStackInSlot(slot);
        return !stack.isEmpty() && !this.handler.extractItem(slot, 1, true).isEmpty();
    }

    private @NotNull ItemStack stack(@NotNull ItemUnit value) {
        if (!(value instanceof MinecraftItemUnit unit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " only accepts MinecraftItemUnit instances");
        }
        return unit.get().copy();
    }

    private void restore(int slot, @NotNull ItemStack previous) {
        ItemStack current = this.handler.getStackInSlot(slot);
        if (!current.isEmpty()) {
            ItemStack extracted = this.handler.extractItem(slot, current.getCount(), false);
            if (extracted.getCount() != current.getCount()) {
                throw new IllegalStateException("Could not restore NeoForge item handler slot " + slot);
            }
        }

        if (!previous.isEmpty()) {
            ItemStack remaining = this.handler.insertItem(slot, previous.copy(), false);
            if (!remaining.isEmpty()) {
                throw new IllegalStateException("Could not restore NeoForge item handler slot " + slot);
            }
        }
    }

    public static @Nullable <U extends Unit<?>> Vault<U> create(@NotNull NexoMinecraft nexo, @NotNull Class<U> type, @Nullable IItemHandler handler) {
        if (handler == null) {
            return null;
        }
        if (type != ItemUnit.class) {
            throw new IllegalArgumentException("Tried to create non ItemUnit NeoForgeItemHandlerVault");
        }
        return Nexo.<Vault<U>>type(Vault.class).cast(new NeoForgeItemHandlerVault(nexo, handler));
    }

}
