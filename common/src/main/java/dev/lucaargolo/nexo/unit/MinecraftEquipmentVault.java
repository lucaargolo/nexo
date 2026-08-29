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

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class MinecraftEquipmentVault extends AbstractCollection<ItemUnit<?>> implements Vault<ItemUnit<?>> {

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    private final @NotNull LivingEntity entity;
    private final @NotNull EquipmentSlot slot;

    private MinecraftEquipmentVault(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull LivingEntity entity, @NotNull EquipmentSlot slot) {
        this.nexo = nexo;
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
    public boolean isFull() {
        return !entity.getItemBySlot(slot).isEmpty();
    }

    @Override
    public int size() {
        return entity.getItemBySlot(slot).isEmpty() ? 0 : 1;
    }

    @Override
    public boolean contains(@Nullable Object object) {
        if (!(object instanceof MinecraftItemUnit<?> item)) {
            return false;
        }
        ItemStack target = item.get();
        return !target.isEmpty() && ItemStack.matches(entity.getItemBySlot(slot), target);
    }

    @Override
    public boolean add(@NotNull ItemUnit<?> item) {
        if (!(item instanceof MinecraftItemUnit<?> minecraftItem)) {
            throw new IllegalArgumentException("MinecraftEquipmentVault only accepts MinecraftItemUnit instances");
        }
        if (!entity.getItemBySlot(slot).isEmpty() || minecraftItem.get().isEmpty()) {
            return false;
        }
        entity.setItemSlot(slot, minecraftItem.get().copy());
        return true;
    }

    @Override
    public boolean remove(@Nullable Object object) {
        if (!contains(object)) {
            return false;
        }
        entity.setItemSlot(slot, ItemStack.EMPTY);
        return true;
    }

    @Override
    public void clear() {
        if (!entity.getItemBySlot(slot).isEmpty()) {
            entity.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public @NotNull Iterator<ItemUnit<?>> iterator() {
        return new Iterator<>() {
            private boolean returned;

            @Override
            public boolean hasNext() {
                return !returned && !entity.getItemBySlot(slot).isEmpty();
            }

            @Override
            public @NotNull ItemUnit<?> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                returned = true;
                return nexo.stackToUnit(entity.getItemBySlot(slot));
            }

            @Override
            public void remove() {
                if (!returned) {
                    throw new IllegalStateException();
                }
                entity.setItemSlot(slot, ItemStack.EMPTY);
                returned = false;
            }
        };
    }

}
