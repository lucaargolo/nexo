package dev.lucaargolo.nexo.unit.block;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.unit.FabricAttachmentData;
import dev.lucaargolo.nexo.unit.FabricStorageVault;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FabricMinecraftBlockUnit<C extends Role> extends MinecraftBlockUnit<FabricNexoMinecraft, C>{

    public FabricMinecraftBlockUnit(@NotNull FabricNexoMinecraft nexo, @NotNull BlockBase feature, @Nullable C role, @Nullable Level level, @Nullable BlockPos position, @NotNull BlockState state, @Nullable BlockEntity entity) {
        this(nexo, feature, role, level, position, state, entity, null);
    }

    public FabricMinecraftBlockUnit(@NotNull FabricNexoMinecraft nexo, @NotNull BlockBase feature, @Nullable C role, @Nullable Level level, @Nullable BlockPos position, @NotNull BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        super(nexo, feature, role, level, position, state, entity, direction);
    }

    @Override
    public @NotNull <U extends Unit<?, ?>> Set<String> vaults(@NotNull Class<U> type) {
        return FabricStorageVault.vaults(super.vaults(type), type, this.transferStorage());
    }

    @Override
    public @Nullable <U extends Unit<?, ?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        if (!MinecraftContainerVault.KEY.equals(key)) {
            return super.vault(type, key);
        }
        Vault<U> vault = FabricStorageVault.create(this.nexo, type, this.transferStorage());
        return vault == null ? super.vault(type, key) : vault;
    }

    private @Nullable Storage<ItemVariant> transferStorage() {
        if (this.level == null || this.position == null) {
            return null;
        }
        return ItemStorage.SIDED.find(this.level, this.position, this.state, this.entity, this.direction);
    }

    @Override
    public @NotNull List<@NotNull DataBase<?>> data() {
        if (this.entity == null) {
            return List.of();
        }
        List<@NotNull DataBase<?>> componentData = new ArrayList<>();
        this.entity.collectComponents().forEach(component -> componentData.add(MinecraftFeatureType.DATA.convert(this.nexo, component.type())));
        CompoundTag tag = this.entity.saveWithFullMetadata(this.nexo.getRegistryHandler().getRegistry());
        this.entity.removeComponentsFromTag(tag);
        return FabricAttachmentData.data(this.nexo, this.entity, componentData, tag, MinecraftFeatureType.DATA.convert(this.nexo, DataComponents.BLOCK_ENTITY_DATA));
    }

    @Override
    public <D> D getData(@NotNull DataBase<D> data) {
        if (data instanceof DataBase.Constrained<?> constrained && this.feature.initialData().contains(constrained)) {
            return data.cast(this.getStateData(constrained));
        } else if (this.entity != null) {
            return FabricAttachmentData.getData(this.nexo, this.feature.initialData(), this.entity, data);
        } else if (data instanceof DataBase.Constrained<?>) {
            throw new IllegalArgumentException("Tried to get non-initial constrained data " + data + " from non-dynamic MinecraftBlockUnit");
        } else {
            throw new IllegalArgumentException("Tried to ge non-constrained data " + data + " from non-dynamic MinecraftBlockUnit");
        }
    }

    @NotNull
    @Override
    public <D> BlockUnit<C> setData(@NotNull DataBase<D> data, @Nullable D d) {
        if (data instanceof DataBase.Constrained<?> constrained && this.feature.initialData().contains(constrained)) {
            BlockState state = this.setStateData(constrained, d);
            if (this.level != null && this.position != null) {
                this.level.setBlockAndUpdate(this.position, state);
            }
            Class<BlockUnit<C>> type = Nexo.type(this.getClass());
            return type.cast(this.nexo.stateToUnit(state));
        } else if (this.entity != null) {
            FabricAttachmentData.setData(this.nexo, this.entity, data, d);
            return this;
        } else if (data instanceof DataBase.Constrained<?>) {
            throw new IllegalArgumentException("Tried to set non-initial constrained data " + data + " to non-dynamic MinecraftBlockUnit");
        } else {
            throw new IllegalArgumentException("Tried to set non-constrained data " + data + " to non-dynamic MinecraftBlockUnit");
        }
    }

}
