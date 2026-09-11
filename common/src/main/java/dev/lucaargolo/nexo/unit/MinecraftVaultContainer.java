package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MinecraftVaultContainer implements Container {

    private final @NotNull NexoMinecraft nexo;
    private final @NotNull Vault.Slotted<ItemUnit> vault;

    public MinecraftVaultContainer(@NotNull NexoMinecraft nexo, @NotNull Vault.Slotted<ItemUnit> vault) {
        this.nexo = nexo;
        this.vault = vault;
    }

    public @NotNull Vault.Slotted<ItemUnit> vault() {
        return this.vault;
    }

    @Override
    public int getContainerSize() {
        return this.vault.slots();
    }

    @Override
    public @NotNull ItemStack getItem(int pSlot) {
        return this.stack(this.vault.get(pSlot));
    }

    @Override
    public void setItem(int pSlot, @NotNull ItemStack pStack) {
        Objects.checkIndex(pSlot, this.getContainerSize());
        if (pStack.isEmpty()) {
            this.vault.clear(pSlot);
            return;
        }

        ItemUnit value = this.nexo.stackToUnit(pStack.copy());
        if (this.vault.canAdd(pSlot, value)) {
            this.vault.set(pSlot, value);
        }
    }

    @Override
    public @NotNull ItemStack removeItem(int pSlot, int pAmount) {
        Objects.checkIndex(pSlot, this.getContainerSize());
        if (pAmount <= 0) {
            return ItemStack.EMPTY;
        }
        return this.stack(this.vault.remove(pSlot, pAmount));
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int pSlot) {
        Objects.checkIndex(pSlot, this.getContainerSize());
        return this.stack(this.vault.remove(pSlot));
    }

    @Override
    public void clearContent() {
        boolean changed = false;
        for (int slot = 0; slot < this.getContainerSize(); slot++) {
            if (!this.vault.isEmpty(slot) && this.vault.canRemove(slot)) {
                this.vault.clear(slot);
                changed = true;
            }
        }
        if (changed) {
            this.setChanged();
        }
    }

    @Override
    public boolean isEmpty() {
        return this.vault.isEmpty();
    }

    @Override
    public boolean canPlaceItem(int pSlot, @NotNull ItemStack pStack) {
        Objects.checkIndex(pSlot, this.getContainerSize());
        return !pStack.isEmpty() && this.vault.canAdd(pSlot, this.nexo.stackToUnit(pStack.copy()));
    }

    @Override
    public boolean canTakeItem(@NotNull Container pTarget, int pSlot, @NotNull ItemStack pStack) {
        Objects.checkIndex(pSlot, this.getContainerSize());
        return this.vault.canRemove(pSlot);
    }

    @Override
    public void setChanged() {
        this.vault.changed();
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return true;
    }

    public static final class Slot extends net.minecraft.world.inventory.Slot {

        private final @NotNull MinecraftVaultContainer container;
        private final int vaultSlot;

        public Slot(@NotNull MinecraftVaultContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.container = container;
            this.vaultSlot = slot;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return !stack.isEmpty() && this.container.vault.canAdd(this.vaultSlot, this.container.nexo.stackToUnit(stack.copy()));
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return this.container.vault.canRemove(this.vaultSlot);
        }
    }

    private @NotNull ItemStack stack(@NotNull ItemUnit value) {
        if (!(value instanceof MinecraftItemUnit unit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " wrapped Vault with non MinecraftItemUnit instances");
        }
        return unit.get().copy();
    }
}