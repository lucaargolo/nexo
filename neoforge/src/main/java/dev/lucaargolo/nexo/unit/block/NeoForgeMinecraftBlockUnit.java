package dev.lucaargolo.nexo.unit.block;

import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import dev.lucaargolo.nexo.unit.NeoForgeAttachmentData;
import dev.lucaargolo.nexo.unit.NeoForgeItemHandlerVault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NeoForgeMinecraftBlockUnit<C extends Role> extends MinecraftBlockUnit<NeoForgeNexoMinecraft, C>{

    public NeoForgeMinecraftBlockUnit(@NotNull NeoForgeNexoMinecraft nexo, @NotNull BlockBase feature, @Nullable C role, @Nullable Level level, @Nullable BlockPos position, @NotNull BlockState state, @Nullable BlockEntity entity) {
        this(nexo, feature, role, level, position, state, entity, null);
    }

    public NeoForgeMinecraftBlockUnit(@NotNull NeoForgeNexoMinecraft nexo, @NotNull BlockBase feature, @Nullable C role, @Nullable Level level, @Nullable BlockPos position, @NotNull BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        super(nexo, feature, role, level, position, state, entity, direction);
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
        if (this.level == null || this.position == null) {
            return null;
        }
        return Capabilities.ItemHandler.BLOCK.getCapability(this.level, this.position, this.state, this.entity, this.direction);
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
        return NeoForgeAttachmentData.data(this.nexo, this.entity, componentData, tag, MinecraftFeatureType.DATA.convert(this.nexo, DataComponents.BLOCK_ENTITY_DATA));
    }

    @Override
    public <D> D getData(@NotNull DataBase<D> data) {
        if (data instanceof DataBase.Constrained<?> constrained && this.feature.initialData().contains(constrained)) {
            return data.cast(this.getStateData(constrained));
        } else if (this.entity != null) {
            return NeoForgeAttachmentData.getData(this.nexo, this.feature.initialData(), this.entity, data);
        } else if (data instanceof DataBase.Constrained<?>) {
            throw new IllegalArgumentException("Tried to get non-initial constrained data " + data + " from non-dynamic MinecraftBlockUnit");
        } else {
            throw new IllegalArgumentException("Tried to ge non-constrained data " + data + " from non-dynamic MinecraftBlockUnit");
        }
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        if (data instanceof DataBase.Constrained<?> constrained && this.feature.initialData().contains(constrained)) {
            this.state = this.setStateData(constrained, d);
            if (this.level != null && this.position != null) {
                this.level.setBlockAndUpdate(this.position, this.state);
            }
        } else if (this.entity != null) {
            NeoForgeAttachmentData.setData(this.nexo, this.entity, data, d);
        } else if (data instanceof DataBase.Constrained<?>) {
            throw new IllegalArgumentException("Tried to set non-initial constrained data " + data + " to non-dynamic MinecraftBlockUnit");
        } else {
            throw new IllegalArgumentException("Tried to set non-constrained data " + data + " to non-dynamic MinecraftBlockUnit");
        }
    }

}
