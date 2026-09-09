package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.SlottedVault;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class MinecraftVaultContainer implements Container {

    private final @NotNull NexoMinecraft nexo;
    private final @NotNull SlottedVault<ItemUnit> vault;

    public MinecraftVaultContainer(@NotNull NexoMinecraft nexo, @NotNull SlottedVault<ItemUnit> vault) {
        this.nexo = nexo;
        this.vault = vault;
    }

    @Override
    public int getContainerSize() {
        return this.vault.slots();
    }

    @Override
    public @NotNull ItemStack getItem(int pSlot) {
        ItemUnit value = this.vault.get(pSlot);
        if (!(value instanceof MinecraftItemUnit unit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " wrapped Vault with non MinecraftItemUnit instances");
        }
        return unit.get();
    }

    @Override
    public void setItem(int pSlot, @NotNull ItemStack pStack) {
        if(!pStack.isEmpty()) {
            if(this.vault.canAdd()) {
                this.vault.set(pSlot, this.nexo.stackToUnit(pStack));
            }
        }else if(this.vault.canRemove()) {
            this.vault.set(pSlot, this.nexo.stackToUnit(pStack));
        }
    }

    @Override
    public @NotNull ItemStack removeItem(int pSlot, int pAmount) {
        if(!this.vault.canRemove()) {
            return ItemStack.EMPTY;
        }
        ItemUnit value = this.vault.remove(pSlot, pAmount);
        if (!(value instanceof MinecraftItemUnit unit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " wrapped Vault with non MinecraftItemUnit instances");
        }
        return unit.get();
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int pSlot) {
        if(!this.vault.canRemove()) {
            return ItemStack.EMPTY;
        }
        ItemUnit value = this.vault.remove(pSlot);
        if (!(value instanceof MinecraftItemUnit unit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " wrapped Vault with non MinecraftItemUnit instances");
        }
        return unit.get();
    }

    @Override
    public void clearContent() {
        if(this.vault.canRemove()) {
            this.vault.clear();
        }
    }

    @Override
    public boolean isEmpty() {
        return this.vault.isEmpty();
    }

    @Override
    public void setChanged() {

    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return true;
    }

}
