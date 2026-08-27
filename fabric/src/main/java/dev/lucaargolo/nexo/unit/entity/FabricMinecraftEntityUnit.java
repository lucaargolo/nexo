package dev.lucaargolo.nexo.unit.entity;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.util.Side;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
