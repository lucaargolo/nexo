package dev.lucaargolo.nexo.unit.entity;

import dev.lucaargolo.nexo.NeoForgeNexoRegistryHandler;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeoForgeMinecraftEntityUnit extends MinecraftEntityUnit {

    public NeoForgeMinecraftEntityUnit(@NotNull NexoMinecraft nexo, @NotNull EntityBase feature, @NotNull Entity entity) {
        super(nexo, feature, entity);
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        AttachmentType<D> type = NeoForgeNexoRegistryHandler.getDataAttachment(data);
        return this.entity.getExistingDataOrNull(type);
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        AttachmentType<D> type = NeoForgeNexoRegistryHandler.getDataAttachment(data);
        if (d == null) {
            this.entity.removeData(type);
        } else {
            this.entity.setData(type, d);
        }
    }
}
