package dev.lucaargolo.nexo.unit.entity;

import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import dev.lucaargolo.nexo.unit.NeoForgeAttachmentData;
import dev.lucaargolo.nexo.unit.NeoForgeItemHandlerVault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class NeoForgeMinecraftEntityUnit<C extends Role, E extends Entity> extends MinecraftEntityUnit<NeoForgeNexoMinecraft, C, E> {

    public NeoForgeMinecraftEntityUnit(@NotNull NeoForgeNexoMinecraft nexo, @NotNull EntityBase feature, @Nullable C role, @NotNull E entity) {
        super(nexo, feature, role, entity);
    }

    @Override
    public @NotNull <U extends Unit<?, ?>> Set<String> vaults(@NotNull Class<U> type) {
        return NeoForgeItemHandlerVault.vaults(super.vaults(type), type, this.itemHandler());
    }

    @Override
    public @Nullable <U extends Unit<?, ?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        if (!MinecraftContainerVault.KEY.equals(key)) {
            return super.vault(type, key);
        }
        Vault<U> vault = NeoForgeItemHandlerVault.create(this.nexo, type, this.itemHandler());
        return vault == null ? super.vault(type, key) : vault;
    }

    private @Nullable IItemHandler itemHandler() {
        return Capabilities.ItemHandler.ENTITY.getCapability(this.entity, null);
    }

    @Override
    public @NotNull Side side() {
        return entity.level().isClientSide() ? Side.CLIENT : Side.SERVER;
    }

    @Override
    public @NotNull List<@NotNull DataBase<?>> data() {
        CompoundTag tag = this.entity.saveWithoutId(new CompoundTag());
        return NeoForgeAttachmentData.data(this.nexo, this.entity, List.of(), tag, MinecraftFeatureType.DATA.convert(this.nexo, DataComponents.ENTITY_DATA));
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        return NeoForgeAttachmentData.getData(this.nexo, this.feature.initialData(), this.entity, data);
    }

    @Override
    public <D> @NotNull EntityUnit<C> setData(@NotNull DataBase<D> data, @Nullable D d) {
        NeoForgeAttachmentData.setData(this.nexo, this.entity, data, d);
        return this;
    }


}
