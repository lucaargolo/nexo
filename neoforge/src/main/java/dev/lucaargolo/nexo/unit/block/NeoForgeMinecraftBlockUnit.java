package dev.lucaargolo.nexo.unit.block;

import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import dev.lucaargolo.nexo.unit.NeoForgeItemHandlerVault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class NeoForgeMinecraftBlockUnit<C extends Role> extends MinecraftBlockUnit<NeoForgeNexoMinecraft, C>{

    public NeoForgeMinecraftBlockUnit(@NotNull NeoForgeNexoMinecraft nexo, @NotNull BlockBase feature, @Nullable C role, @Nullable Level level, @Nullable BlockPos position, @NotNull BlockState state, @Nullable BlockEntity entity) {
        super(nexo, feature, role, level, position, state, entity);
    }

    @Override
    public @NotNull <U extends Unit<?, ?>> Set<String> vaults(@NotNull Class<U> type) {
        return this.itemHandler() != null && MinecraftContainerVault.supports(type) ? Set.of(MinecraftContainerVault.KEY) : super.vaults(type);
    }

    @Override
    public @Nullable <U extends Unit<?, ?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        IItemHandler handler = this.itemHandler();
        if (MinecraftContainerVault.KEY.equals(key) && handler != null && MinecraftContainerVault.supports(type)) {
            Class<Vault<U>> clazz = Nexo.type(Vault.class);
            return clazz.cast(new NeoForgeItemHandlerVault(this.nexo, handler));
        }
        return super.vault(type, key);
    }

    private @Nullable IItemHandler itemHandler() {
        if (this.level == null || this.position == null) {
            return null;
        }
        return Capabilities.ItemHandler.BLOCK.getCapability(this.level, this.position, this.state, this.entity, null);
    }

    @Override
    public <D> D getData(@NotNull DataBase<D> data) {
        if (data instanceof DataBase.Constrained<?> constrained && this.feature.data().contains(constrained)) {
            return data.cast(this.getStateData(constrained));
        }else if(this.entity != null) {
            AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
            return this.feature.data().contains(data) ? this.entity.getData(type) : this.entity.getExistingDataOrNull(type);
        }else if(data instanceof DataBase.Constrained<?>) {
            throw new IllegalArgumentException("Tried to get non-initial constrained data " + data + " from non-dynamic MinecraftBlockUnit");
        }else{
            throw new IllegalArgumentException("Tried to ge non-constrained data " + data + " from non-dynamic MinecraftBlockUnit");
        }
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        if (data instanceof DataBase.Constrained<?> constrained && this.feature.data().contains(constrained)) {
            this.state = this.setStateData(constrained, d);
            if(this.level != null && this.position != null) {
                this.level.setBlockAndUpdate(this.position, this.state);
            }
        }else if(this.entity != null) {
            AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
            this.entity.setData(type, d);
        }else if(data instanceof DataBase.Constrained<?>) {
            throw new IllegalArgumentException("Tried to set non-initial constrained data " + data + " to non-dynamic MinecraftBlockUnit");
        }else{
            throw new IllegalArgumentException("Tried to set non-constrained data " + data + " to non-dynamic MinecraftBlockUnit");
        }
    }

}
