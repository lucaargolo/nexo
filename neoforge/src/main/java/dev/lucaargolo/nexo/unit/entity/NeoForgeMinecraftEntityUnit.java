package dev.lucaargolo.nexo.unit.entity;

import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.util.Side;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeoForgeMinecraftEntityUnit<C extends Role, E extends Entity> extends MinecraftEntityUnit<NeoForgeNexoMinecraft, C, E> {

    public NeoForgeMinecraftEntityUnit(@NotNull NeoForgeNexoMinecraft nexo, @NotNull EntityBase feature, @Nullable C role, @NotNull E entity) {
        super(nexo, feature, role, entity);
    }

    @Override
    public @NotNull Side side() {
        return entity.level().isClientSide() ? Side.CLIENT : Side.SERVER;
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
        return this.feature.data().contains(data) ? this.entity.getData(type) : this.entity.getExistingDataOrNull(type);
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
        if (d == null) {
            this.entity.removeData(type);
        } else {
            this.entity.setData(type, d);
        }
    }


}
