package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class NeoForgeVaultItemHandler implements IItemHandler {

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    private final @NotNull List<Vault<ItemUnit>> vaults;
    private @NotNull List<Slot> slots;

    private NeoForgeVaultItemHandler(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull List<Vault<ItemUnit>> vaults) {
        this.nexo = nexo;
        this.vaults = vaults;
        this.slots = this.createSlots();
    }

    public static @Nullable IItemHandler create(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull Unit<?> unit,
            @NotNull Map<String, ? extends Function<?, ? extends @Nullable Vault<ItemUnit>>> vaultFactories
    ) {
        List<Vault<ItemUnit>> vaults = new ArrayList<>(vaultFactories.size());
        Class<Function<Unit<?>, ? extends @Nullable Vault<ItemUnit>>> type = Nexo.type(Function.class);
        for (Function<?, ? extends @Nullable Vault<ItemUnit>> factory : vaultFactories.values()) {
            @Nullable Vault<ItemUnit> vault = type.cast(factory).apply(unit);
            if (vault != null) {
                vaults.add(vault);
            }
        }
        return vaults.isEmpty() ? null : new NeoForgeVaultItemHandler(nexo, List.copyOf(vaults));
    }

    @Override
    public int getSlots() {
        this.refreshSlots();
        return this.slots.size();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        this.refreshSlots();
        Slot selected = this.slot(slot);
        if (selected.handler != null) {
            return selected.handler.getStackInSlot(selected.physicalSlot).copy();
        }
        if (selected.physicalVault != null) {
            return selected.physicalVault.getItem(selected.physicalSlot).copy();
        }
        return selected.item == null ? ItemStack.EMPTY : selected.item.get().copy();
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        this.refreshSlots();
        Slot selected = this.slot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (selected.handler != null) {
            return selected.handler.insertItem(selected.physicalSlot, stack, simulate);
        }
        if (!selected.vault.canAdd()) {
            return stack.copy();
        }
        if (selected.physicalVault != null) {
            return selected.physicalVault.insertItem(selected.physicalSlot, stack, simulate);
        }
        if (selected.item != null && !ItemStack.isSameItemSameComponents(selected.item.get(), stack)) {
            return stack.copy();
        }
        if (selected.item == null && selected.vault.isFull()) {
            return stack.copy();
        }
        if (selected.logicalSlot >= 0) {
            ItemStack current = selected.item == null ? ItemStack.EMPTY : selected.item.get();
            int amount = Math.min(stack.getCount(), Math.max(0, this.getSlotLimit(slot) - current.getCount()));
            if (amount <= 0) {
                return stack.copy();
            }
            if (!simulate) {
                ItemStack stored = stack.copyWithCount(current.getCount() + amount);
                selected.vault.set(selected.logicalSlot, this.nexo.stackToUnit(stored));
                this.refreshSlots();
            }
            return stack.copyWithCount(stack.getCount() - amount);
        }
        List<ItemStack> before = simulate ? this.snapshot(selected.vault) : null;
        try {
            int stored = this.count(selected.vault, stack);
            boolean changed = selected.vault.add(this.nexo.stackToUnit(stack.copy()));
            int inserted = changed ? Math.min(stack.getCount(), Math.max(0, this.count(selected.vault, stack) - stored)) : 0;
            if (!simulate && inserted > 0) {
                this.refreshSlots();
            }
            return inserted == 0 ? stack.copy() : stack.copyWithCount(stack.getCount() - inserted);
        } finally {
            if (before != null) {
                this.restore(selected.vault, before);
            }
        }
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        this.refreshSlots();
        Slot selected = this.slot(slot);
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        if (selected.handler != null) {
            return selected.handler.extractItem(selected.physicalSlot, amount, simulate);
        }
        if (!selected.vault.canRemove()) {
            return ItemStack.EMPTY;
        }
        if (selected.physicalVault != null) {
            return selected.physicalVault.extractItem(selected.physicalSlot, amount, simulate);
        }
        if (selected.item == null || selected.item.get().isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (simulate) {
            ItemStack source = selected.item.get();
            int extracted = Math.min(Math.min(amount, source.getCount()), source.getMaxStackSize());
            return source.copyWithCount(extracted);
        }
        List<ItemStack> before = this.snapshot(selected.vault);
        try {
            ItemStack result = this.extract(selected.vault, selected.item, amount, selected.logicalSlot);
            if (!result.isEmpty()) {
                this.refreshSlots();
            }
            return result;
        } catch (RuntimeException exception) {
            this.restore(selected.vault, before);
            this.refreshSlots();
            throw exception;
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        this.refreshSlots();
        Slot selected = this.slot(slot);
        if (selected.handler != null) {
            return selected.handler.getSlotLimit(selected.physicalSlot);
        }
        if (selected.physicalVault != null) {
            return selected.physicalVault.slotLimit(selected.physicalSlot);
        }
        return selected.item == null ? Item.DEFAULT_MAX_STACK_SIZE : selected.item.get().getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        this.refreshSlots();
        Slot selected = this.slot(slot);
        if (selected.handler != null) {
            return selected.handler.isItemValid(selected.physicalSlot, stack);
        }
        if (selected.physicalVault != null) {
            return selected.vault.canAdd() && !stack.isEmpty() && selected.physicalVault.isItemValid(selected.physicalSlot, stack);
        }
        if (selected.item == null) {
            return selected.vault.canAdd() && !selected.vault.isFull() && !stack.isEmpty();
        }
        return selected.vault.canAdd() && !stack.isEmpty() && ItemStack.isSameItemSameComponents(selected.item.get(), stack);
    }

    private int count(@NotNull Vault<ItemUnit> vault, @NotNull ItemStack stack) {
        int count = 0;
        for (ItemUnit item : vault) {
            if (item instanceof MinecraftItemUnit minecraftItem && ItemStack.isSameItemSameComponents(minecraftItem.get(), stack)) {
                count += minecraftItem.get().getCount();
            }
        }
        return count;
    }

    private @NotNull List<ItemStack> snapshot(@NotNull Vault<ItemUnit> vault) {
        List<ItemStack> snapshot = new ArrayList<>(vault.size());
        for (ItemUnit item : vault) {
            snapshot.add(item instanceof MinecraftItemUnit minecraftItem ? minecraftItem.get().copy() : ItemStack.EMPTY);
        }
        return snapshot;
    }

    private void restore(@NotNull Vault<ItemUnit> vault, @NotNull List<ItemStack> snapshot) {
        vault.clear();
        for (int index = 0; index < snapshot.size(); index++) {
            ItemStack stack = snapshot.get(index);
            vault.set(index, stack.isEmpty() ? vault.defaultValue() : this.nexo.stackToUnit(stack.copy()));
        }
    }

    private @NotNull List<Slot> createSlots() {
        List<Slot> slots = new ArrayList<>();
        for (Vault<ItemUnit> vault : this.vaults) {
            if (vault instanceof NeoForgeItemHandlerVault handlerVault) {
                IItemHandler handler = handlerVault.handler;
                for (int physicalSlot = 0; physicalSlot < handler.getSlots(); physicalSlot++) {
                    slots.add(new Slot(vault, handler, null, null, physicalSlot, -1));
                }
                continue;
            }
            if (vault instanceof MinecraftItemVault minecraftVault) {
                for (int physicalSlot = 0; physicalSlot < minecraftVault.slotCount(); physicalSlot++) {
                    slots.add(new Slot(vault, null, minecraftVault, null, physicalSlot, -1));
                }
                continue;
            }
            for (int logicalSlot = 0; logicalSlot < vault.size(); logicalSlot++) {
                ItemUnit item = vault.get(logicalSlot);
                MinecraftItemUnit minecraftItem = item instanceof MinecraftItemUnit candidate && !candidate.get().isEmpty() ? candidate : null;
                slots.add(new Slot(vault, null, null, minecraftItem, -1, logicalSlot));
            }
        }
        return List.copyOf(slots);
    }

    private void refreshSlots() {
        this.slots = this.createSlots();
    }

    private @NotNull Slot slot(int index) {
        if (index < 0 || index >= this.slots.size()) {
            throw new IndexOutOfBoundsException("Slot " + index + " out of bounds: [0, " + this.slots.size() + ")");
        }
        return this.slots.get(index);
    }

    private @NotNull ItemStack extract(@NotNull Vault<ItemUnit> vault, @NotNull MinecraftItemUnit target, int amount, int logicalSlot) {
        ItemUnit item = vault.get(logicalSlot);
        if (!(item instanceof MinecraftItemUnit minecraftItem) || !ItemStack.isSameItemSameComponents(minecraftItem.get(), target.get())) {
            return ItemStack.EMPTY;
        }
        ItemStack source = minecraftItem.get();
        int extracted = Math.min(Math.min(amount, source.getCount()), source.getMaxStackSize());
        ItemStack result = source.copyWithCount(extracted);
        vault.remove(logicalSlot);
        if (extracted < source.getCount()) {
            vault.set(logicalSlot, this.nexo.stackToUnit(source.copyWithCount(source.getCount() - extracted)));
        }
        return result;
    }

    private record Slot(
            @NotNull Vault<ItemUnit> vault,
            @Nullable IItemHandler handler,
            @Nullable MinecraftItemVault physicalVault,
            @Nullable MinecraftItemUnit item,
            int physicalSlot,
            int logicalSlot
    ) {
    }

}
