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
import org.jetbrains.annotations.Nullable;

public final class MinecraftEquipmentVault extends MinecraftItemVault {

    private final @NotNull LivingEntity entity;
    private final @NotNull EquipmentSlot slot;

    private MinecraftEquipmentVault(@NotNull NexoMinecraft nexo, @NotNull LivingEntity entity, @NotNull EquipmentSlot slot) {
        super(nexo);
        this.entity = entity;
        this.slot = slot;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return this.entity.getItemBySlot(this.slot);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        this.entity.setItemSlot(this.slot, stack);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return !stack.isEmpty();
    }

    @Override
    public int slotLimit(int slot) {
        return this.getItem(0).getMaxStackSize();
    }

    public static @Nullable MinecraftEquipmentVault create(@NotNull NexoMinecraft nexo, @Nullable LivingEntity entity, @NotNull EquipmentSlot slot) {
        if (entity == null) {
            return null;
        }
        return new MinecraftEquipmentVault(nexo, entity, slot);
    }


}
