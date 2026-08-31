package dev.lucaargolo.nexo.unit.entity;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.FabricMinecraftRegistryHandler;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.unit.FabricAttachmentData;
import dev.lucaargolo.nexo.unit.FabricStorageVault;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class FabricMinecraftEntityUnit<C extends Role, E extends Entity> extends MinecraftEntityUnit<FabricNexoMinecraft, C, E> {

    public FabricMinecraftEntityUnit(@NotNull FabricNexoMinecraft nexo, @NotNull EntityBase feature, @Nullable C role, @NotNull E entity) {
        super(nexo, feature, role, entity);
    }

    @Override
    public @NotNull Side side() {
        return entity.level().isClientSide() ? Side.CLIENT : Side.SERVER;
    }

    @Override
    public @NotNull <U extends Unit<?, ?>> Set<String> vaults(@NotNull Class<U> type) {
        return FabricStorageVault.vaults(super.vaults(type), type, this.entityStorage());
    }

    @Override
    public @Nullable <U extends Unit<?, ?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        if (!MinecraftContainerVault.KEY.equals(key)) {
            return super.vault(type, key);
        }
        Vault<U> vault = FabricStorageVault.create(this.nexo, type, this.entityStorage());
        return vault == null ? super.vault(type, key) : vault;
    }

    private @Nullable Storage<ItemVariant> entityStorage() {
        return FabricMinecraftRegistryHandler.ENTITY_ITEM_STORAGE.find(this.entity, null);
    }

    @Override
    public @NotNull List<@NotNull DataBase<?>> data() {
        CompoundTag tag = this.entity.saveWithoutId(new CompoundTag());
        return FabricAttachmentData.data(this.nexo, this.entity, List.of(), tag, MinecraftFeatureType.DATA.convert(this.nexo, DataComponents.ENTITY_DATA));
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        return FabricAttachmentData.getData(this.nexo, this.feature.initialData(), this.entity, data);
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        FabricAttachmentData.setData(this.nexo, this.entity, data, d);
    }


}
