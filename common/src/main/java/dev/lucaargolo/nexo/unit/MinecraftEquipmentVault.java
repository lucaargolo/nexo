package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MinecraftEquipmentVault extends MinecraftItemVault {

    private final @NotNull LivingEntity entity;
    private final @NotNull EquipmentSlot slot;

    private MinecraftEquipmentVault(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull LivingEntity entity, @NotNull EquipmentSlot slot) {
        super(nexo);
        this.entity = entity;
        this.slot = slot;
    }

    public static @NotNull String key(@NotNull EquipmentSlot slot) {
        return slot.getName();
    }

    public static boolean supports(@NotNull Class<?> type) {
        return type.isAssignableFrom(MinecraftItemUnit.class);
    }

    public static @Nullable <U extends Unit<?, ?>> Vault<U> create(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @Nullable LivingEntity entity,
            @NotNull EquipmentSlot slot,
            @NotNull Class<U> type
    ) {
        if (entity == null || !supports(type)) {
            return null;
        }
        Class<Vault<U>> clazz = Nexo.type(Vault.class);
        return clazz.cast(new MinecraftEquipmentVault(nexo, entity, slot));
    }

    @Override
    int slotCount() {
        return 1;
    }

    @Override
    @NotNull ItemStack getItem(int slot) {
        return this.entity.getItemBySlot(this.slot);
    }

    @Override
    void setItem(int slot, @NotNull ItemStack stack) {
        this.entity.setItemSlot(this.slot, stack);
    }

    @Override
    boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return !stack.isEmpty();
    }

    @Override
    int slotLimit(int slot) {
        return this.getItem(0).getMaxStackSize();
    }

}
