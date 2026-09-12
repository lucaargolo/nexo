package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MinecraftEquipmentVault implements Vault.Slotted<ItemUnit> {

    private final @NotNull NexoMinecraft nexo;
    private final @NotNull LivingEntity entity;
    private final @NotNull EquipmentSlot slot;
    private final @NotNull ItemUnit empty;

    public MinecraftEquipmentVault(@NotNull NexoMinecraft nexo, @NotNull LivingEntity entity, @NotNull EquipmentSlot slot) {
        this.nexo = nexo;
        this.entity = entity;
        this.slot = slot;
        this.empty = nexo.stackToUnit(ItemStack.EMPTY);
    }

    @Override
    public @NotNull ItemUnit empty() {
        return this.empty;
    }

    @Override
    public boolean canExtract() {
        return this.canExtract(0);
    }

    @Override
    public int maxStackAmount() {
        return 64;
    }

    @Override
    public int slots() {
        return 1;
    }

    @Override
    public @NotNull ItemUnit get(int slot) {
        Objects.checkIndex(slot, this.slots());
        ItemStack stack = this.entity.getItemBySlot(this.slot);
        if (stack.isEmpty()) {
            return this.empty;
        }
        return this.nexo.stackToUnit(stack.copy());
    }

    @Override
    public @NotNull ItemUnit set(int slot, @NotNull ItemUnit value) {
        Objects.checkIndex(slot, this.slots());
        if (!(value instanceof MinecraftItemUnit unit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " only accepts MinecraftItemUnit instances");
        }

        ItemStack stack = unit.get().copy();
        if (!stack.isEmpty() && !this.canInsert(slot, value)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " rejected item for slot " + slot);
        }

        ItemUnit previous = this.get(slot);
        this.entity.setItemSlot(this.slot, stack);
        return previous;
    }

    @Override
    public boolean canInsert(int slot) {
        Objects.checkIndex(slot, this.slots());
        return true;
    }

    @Override
    public boolean canInsert(@NotNull ItemUnit value) {
        return this.canInsert(0, value);
    }

    @Override
    public boolean canInsert(int slot, @NotNull ItemUnit value) {
        Objects.checkIndex(slot, this.slots());
        if (!(value instanceof MinecraftItemUnit unit)) {
            return false;
        }
        ItemStack stack = unit.get();
        return !stack.isEmpty()
                && stack.getCount() <= stack.getMaxStackSize()
                && this.slot == this.entity.getEquipmentSlotForItem(stack);
    }

    @Override
    public boolean canExtract(int slot) {
        Objects.checkIndex(slot, this.slots());
        return !this.entity.getItemBySlot(this.slot).isEmpty();
    }

    @Override
    public int maxStackAmount(int slot, @NotNull ItemUnit value) {
        Objects.checkIndex(slot, this.slots());
        return value instanceof MinecraftItemUnit unit ? Math.min(this.maxStackAmount(slot), unit.get().getMaxStackSize()) : 0;
    }

    public static @NotNull <U extends Unit<?>> Vault<U> create(@NotNull NexoMinecraft nexo, @NotNull Class<U> type, @NotNull LivingEntity entity, @NotNull EquipmentSlot slot) {
        if(type != ItemUnit.class) {
            throw new IllegalArgumentException("Tried to create non ItemUnit MinecraftEquipmentVault");
        }
        return Nexo.<Vault<U>>type(Vault.class).cast(new MinecraftEquipmentVault(nexo, entity, slot));
    }

}
