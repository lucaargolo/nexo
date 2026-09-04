package dev.lucaargolo.nexo.unit.entity;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.mixin.AbstractHorseAccessor;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import dev.lucaargolo.nexo.unit.MinecraftEquipmentVault;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public abstract class MinecraftEntityUnit<N extends NexoMinecraft<N, ?, ?, ?>, E extends Entity> extends EntityUnit implements MinecraftUnit<E> {

    @NotNull
    protected final N nexo;
    @NotNull
    protected final E entity;

    public MinecraftEntityUnit(@NotNull N nexo, @NotNull EntityBase feature, @Nullable Role role, @NotNull E entity) {
        super(nexo, feature, role);
        this.nexo = nexo;
        this.entity = entity;
    }

    @Override
    public @NotNull E get() {
        return this.entity;
    }

    @Override
    public @Nullable WorldUnit world() {
        return nexo.levelToUnit(this.entity.level());
    }

    @Override
    public @NotNull <U extends Unit<?>> Set<String> vaults(@NotNull Class<U> type) {
        if (!MinecraftContainerVault.supports(type)) {
            return super.vaults(type);
        }
        Set<String> vaults = new HashSet<>(super.vaults(type));
        if (this.container() != null) {
            vaults.add(MinecraftContainerVault.KEY);
        }
        if (this.entity instanceof LivingEntity) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                vaults.add(MinecraftEquipmentVault.key(slot));
            }
        }
        return Set.copyOf(vaults);
    }

    @Override
    public @Nullable <U extends Unit<?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        if (!MinecraftContainerVault.supports(type)) {
            return super.vault(type, key);
        }
        if (this.entity instanceof LivingEntity livingEntity) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (MinecraftEquipmentVault.key(slot).equals(key)) {
                    return MinecraftEquipmentVault.create(this.nexo, livingEntity, slot, type);
                }
            }
        }
        Container container = this.container();
        return MinecraftContainerVault.KEY.equals(key) && container != null ? MinecraftContainerVault.create(this.nexo, container, type) : super.vault(type, key);
    }

    private @Nullable Container container() {
        if (this.entity instanceof Player player) {
            return player.getInventory();
        }
        if (this.entity instanceof Container container) {
            return container;
        }
        return this.entity instanceof AbstractHorse horse ? ((AbstractHorseAccessor) horse).nexo$getInventory() : null;
    }

}
