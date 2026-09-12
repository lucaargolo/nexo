package dev.lucaargolo.nexo.unit.entity;

import com.google.common.collect.ImmutableSet;
import dev.lucaargolo.nexo.FabricMinecraftRegistryHandler;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.unit.FabricAttachmentData;
import dev.lucaargolo.nexo.unit.FabricItemStorageVault;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class FabricMinecraftEntityUnit<E extends Entity> extends MinecraftEntityUnit<E> {

    public FabricMinecraftEntityUnit(@NotNull NexoMinecraft nexo, @NotNull EntityBase feature, @Nullable Role role, @NotNull E entity) {
        super(nexo, feature, role, entity);
    }

    private @Nullable Storage<ItemVariant> itemStorage() {
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
    public <D> @NotNull EntityUnit setData(@NotNull DataBase<D> data, @Nullable D d) {
        FabricAttachmentData.setData(this.nexo, this.entity, data, d);
        return this;
    }

    @Override
    public @NotNull <U extends Unit<?>> Set<String> vaults(@NotNull Class<U> type) {
        if(type == ItemUnit.class) {
            if (this.itemStorage() != null) {
                return ImmutableSet.<String>builder()
                        .addAll(super.vaults(type))
                        .add("inventory")
                        .build();
            }
        }
        return super.vaults(type);
    }

    @Override
    public @Nullable <U extends Unit<?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        if(type == ItemUnit.class) {
            if (key.equals("inventory")) {
                Vault<U> vault = FabricItemStorageVault.create(this.nexo, type, this.itemStorage());
                return vault != null ? vault : super.vault(type, key);
            }
        }
        return super.vault(type, key);
    }


}
