package dev.lucaargolo.nexo.unit.block;

import dev.lucaargolo.nexo.NeoForgeNexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.role.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeoForgeMinecraftBlockUnit<C extends Role> extends MinecraftBlockUnit<NeoForgeNexoRegistryHandler, C>{

    public NeoForgeMinecraftBlockUnit(@NotNull NeoForgeNexoRegistryHandler helper, @NotNull BlockBase feature, @Nullable C role, @Nullable Level level, @Nullable BlockPos position, @NotNull BlockState state, @Nullable BlockEntity entity) {
        super(helper, feature, role, level, position, state, entity);
    }

    @Override
    public <D> D getData(@NotNull DataBase<D> data) {
        if (data instanceof DataBase.Constrained<?> constrained && this.feature.data().contains(constrained)) {
            return data.cast(this.getStateData(constrained));
        }else if(this.entity != null) {
            AttachmentType<D> type = helper.getDataAttachment(data);
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
            AttachmentType<D> type = helper.getDataAttachment(data);
            this.entity.setData(type, d);
        }else if(data instanceof DataBase.Constrained<?>) {
            throw new IllegalArgumentException("Tried to set non-initial constrained data " + data + " to non-dynamic MinecraftBlockUnit");
        }else{
            throw new IllegalArgumentException("Tried to set non-constrained data " + data + " to non-dynamic MinecraftBlockUnit");
        }
    }

}
