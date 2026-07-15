package dev.lucaargolo.nexo.unit.world;

import dev.lucaargolo.nexo.FabricNexoRegistryHandler;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class FabricMinecraftWorldUnit extends MinecraftWorldUnit {

    public FabricMinecraftWorldUnit(@NotNull NexoMinecraft nexo, @NotNull WorldBase feature, @Nullable Role role, @NotNull Level level) {
        super(nexo, feature, role, level);
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        AttachmentType<D> type = FabricNexoRegistryHandler.getDataAttachment(data);
        return this.level.getAttached(type);
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        AttachmentType<D> type = FabricNexoRegistryHandler.getDataAttachment(data);
        this.level.setAttached(type, d);
    }

}
