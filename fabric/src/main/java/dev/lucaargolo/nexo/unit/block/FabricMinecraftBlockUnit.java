package dev.lucaargolo.nexo.unit.block;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.unit.FabricTransferVault;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class FabricMinecraftBlockUnit<C extends Role> extends MinecraftBlockUnit<FabricNexoMinecraft, C>{

    public FabricMinecraftBlockUnit(@NotNull FabricNexoMinecraft nexo, @NotNull BlockBase feature, @Nullable C role, @Nullable Level level, @Nullable BlockPos position, @NotNull BlockState state, @Nullable BlockEntity entity) {
        super(nexo, feature, role, level, position, state, entity);
    }

    @Override
    public @NotNull <U extends Unit<?, ?>> Set<String> vaults(@NotNull Class<U> type) {
        return this.transferStorage() != null && MinecraftContainerVault.supports(type) ? Set.of(MinecraftContainerVault.KEY) : super.vaults(type);
    }

    @Override
    public @Nullable <U extends Unit<?, ?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        Storage<ItemVariant> storage = this.transferStorage();
        if (MinecraftContainerVault.KEY.equals(key) && storage != null && MinecraftContainerVault.supports(type)) {
            Class<Vault<U>> clazz = Nexo.type(Vault.class);
            return clazz.cast(new FabricTransferVault(this.nexo, storage));
        }
        return super.vault(type, key);
    }

    private @Nullable Storage<ItemVariant> transferStorage() {
        if (this.level == null || this.position == null) {
            return null;
        }
        return ItemStorage.SIDED.find(this.level, this.position, this.state, this.entity, null);
    }

    @Override
    public <D> D getData(@NotNull DataBase<D> data) {
        if (data instanceof DataBase.Constrained<?> constrained && this.feature.data().contains(constrained)) {
            return data.cast(this.getStateData(constrained));
        } else if (this.entity != null) {
            AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
            return this.feature.data().contains(data) ? this.entity.getAttachedOrCreate(type) : this.entity.getAttached(type);
        } else if (data instanceof DataBase.Constrained<?>) {
            throw new IllegalArgumentException("Tried to get non-initial constrained data " + data + " from non-dynamic MinecraftBlockUnit");
        } else {
            throw new IllegalArgumentException("Tried to ge non-constrained data " + data + " from non-dynamic MinecraftBlockUnit");
        }
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        if (data instanceof DataBase.Constrained<?> constrained && this.feature.data().contains(constrained)) {
            this.state = this.setStateData(constrained, d);
            if (this.level != null && this.position != null) {
                this.level.setBlockAndUpdate(this.position, this.state);
            }
        } else if (this.entity != null) {
            AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
            this.entity.setAttached(type, d);
        } else if (data instanceof DataBase.Constrained<?>) {
            throw new IllegalArgumentException("Tried to set non-initial constrained data " + data + " to non-dynamic MinecraftBlockUnit");
        } else {
            throw new IllegalArgumentException("Tried to set non-constrained data " + data + " to non-dynamic MinecraftBlockUnit");
        }
    }

}
