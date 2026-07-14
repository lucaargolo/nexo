package dev.lucaargolo.nexo.unit.entity;

import dev.lucaargolo.nexo.FabricNexoRegistryHandler;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class FabricMinecraftEntityUnit extends MinecraftEntityUnit {

    public FabricMinecraftEntityUnit(@NotNull NexoMinecraft nexo, @NotNull EntityBase feature, @NotNull Entity entity) {
        super(nexo, feature, entity);
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        AttachmentType<D> type = FabricNexoRegistryHandler.getDataAttachment(data);
        return this.entity.getAttached(type);
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        AttachmentType<D> type = FabricNexoRegistryHandler.getDataAttachment(data);
        this.entity.setAttached(type, d);
    }
}
