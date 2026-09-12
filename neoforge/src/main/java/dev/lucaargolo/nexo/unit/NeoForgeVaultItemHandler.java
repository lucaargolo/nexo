package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NeoForgeVaultItemHandler implements IItemHandler {

    private final @NotNull NexoMinecraft nexo;
    private final @NotNull List<Vault.Slotted<ItemUnit>> vaults;
    private final int[] offsets;
    private final int slots;

    public NeoForgeVaultItemHandler(@NotNull NexoMinecraft nexo, @NotNull List<? extends Vault.Slotted<ItemUnit>> vaults) {
        this.nexo = nexo;
        this.vaults = List.copyOf(new ArrayList<>(vaults));
        this.offsets = new int[this.vaults.size() + 1];
        for (int index = 0; index < this.vaults.size(); index++) {
            this.offsets[index + 1] = Math.addExact(this.offsets[index], this.vaults.get(index).slots());
        }
        this.slots = this.offsets[this.offsets.length - 1];
    }

    @Override
    public int getSlots() {
        return this.slots;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        int vaultIndex = this.vaultIndex(slot);
        return this.stack(this.vaults.get(vaultIndex).get(slot - this.offsets[vaultIndex]));
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        int vaultIndex = this.vaultIndex(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Vault.Slotted<ItemUnit> vault = this.vaults.get(vaultIndex);
        int vaultSlot = slot - this.offsets[vaultIndex];
        ItemUnit value = this.nexo.stackToUnit(stack.copy());
        if (!vault.canInsert(vaultSlot, value)) {
            return stack.copy();
        }

        int inserted = checkedAmount("insert", vault.insert(vaultSlot, value, stack.getCount(), simulate), stack.getCount());
        if (!simulate && inserted > 0) {
            vault.changed();
        }
        return this.remainder(stack, inserted);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        int vaultIndex = this.vaultIndex(slot);
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        Vault.Slotted<ItemUnit> vault = this.vaults.get(vaultIndex);
        int vaultSlot = slot - this.offsets[vaultIndex];
        ItemStack stack = this.stack(vault.get(vaultSlot));
        if (stack.isEmpty() || !vault.canExtract(vaultSlot)) {
            return ItemStack.EMPTY;
        }

        int maximum = Math.min(amount, stack.getMaxStackSize());
        int extracted = checkedAmount("extract", vault.extract(vaultSlot, this.nexo.stackToUnit(stack.copy()), maximum, simulate), maximum);
        if (!simulate && extracted > 0) {
            vault.changed();
        }
        if (extracted == 0) {
            return ItemStack.EMPTY;
        }

        ItemStack result = stack.copy();
        result.setCount(extracted);
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        int vaultIndex = this.vaultIndex(slot);
        Vault.Slotted<ItemUnit> vault = this.vaults.get(vaultIndex);
        int vaultSlot = slot - this.offsets[vaultIndex];
        return Math.max(0, vault.maxStackAmount(vaultSlot));
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        int vaultIndex = this.vaultIndex(slot);
        if (stack.isEmpty()) {
            return false;
        }
        Vault.Slotted<ItemUnit> vault = this.vaults.get(vaultIndex);
        return vault.canInsert(slot - this.offsets[vaultIndex], this.nexo.stackToUnit(stack.copy()));
    }

    private int vaultIndex(int slot) {
        Objects.checkIndex(slot, this.slots);
        for (int index = 0; index < this.vaults.size(); index++) {
            if (slot < this.offsets[index + 1]) {
                return index;
            }
        }
        throw new IllegalStateException("Vault slot index was not mapped");
    }

    private @NotNull ItemStack stack(@NotNull ItemUnit value) {
        if (!(value instanceof MinecraftItemUnit unit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " only accepts MinecraftItemUnit instances");
        }
        return unit.get().copy();
    }

    private @NotNull ItemStack remainder(@NotNull ItemStack stack, int inserted) {
        ItemStack remainder = stack.copy();
        remainder.shrink(inserted);
        return remainder;
    }

    private static int checkedAmount(@NotNull String operation, int amount, int maximum) {
        if (amount < 0 || amount > maximum) {
            throw new IllegalStateException("Vault " + operation + " returned " + amount + " for a maximum of " + maximum);
        }
        return amount;
    }
}