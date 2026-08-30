package dev.lucaargolo.nexo.unit.entity;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.FabricMinecraftRegistryHandler;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.api.unit.Unit;

import dev.lucaargolo.nexo.unit.FabricStorageVault;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
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
        if (!MinecraftContainerVault.supports(type)) {
            return super.vaults(type);
        }
        Set<String> vaults = new HashSet<>(super.vaults(type));
        if (this.entityStorage() != null) {
            vaults.add(MinecraftContainerVault.KEY);
        }
        return Set.copyOf(vaults);
    }

    @Override
    public @Nullable <U extends Unit<?, ?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        if (!MinecraftContainerVault.KEY.equals(key) || !MinecraftContainerVault.supports(type)) {
            return super.vault(type, key);
        }
        Storage<ItemVariant> storage = this.entityStorage();
        if (storage != null) {
            Class<Vault<U>> clazz = Nexo.type(Vault.class);
            return clazz.cast(new FabricStorageVault(this.nexo, storage));
        }
        return super.vault(type, key);
    }

    private @Nullable Storage<ItemVariant> entityStorage() {
        return FabricMinecraftRegistryHandler.ENTITY_ITEM_STORAGE.find(this.entity, null);
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
        return this.feature.data().contains(data) ? this.entity.getAttachedOrCreate(type) : this.entity.getAttached(type);
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
        if (d == null) {
            this.entity.removeAttached(type);
        } else {
            this.entity.setAttached(type, d);
        }
    }


}
