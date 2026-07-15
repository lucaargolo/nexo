package dev.lucaargolo.nexo.unit.world;

import dev.lucaargolo.nexo.NeoForgeNexoRegistryHandler;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeoForgeMinecraftWorldUnit extends MinecraftWorldUnit {

    public NeoForgeMinecraftWorldUnit(@NotNull NexoMinecraft nexo, @NotNull WorldBase feature, @Nullable Role role, @NotNull Level level) {
        super(nexo, feature, role, level);
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        AttachmentType<D> type = NeoForgeNexoRegistryHandler.getDataAttachment(data);
        return this.level.getExistingDataOrNull(type);
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        AttachmentType<D> type = NeoForgeNexoRegistryHandler.getDataAttachment(data);
        if (d == null) {
            this.level.removeData(type);
        } else {
            this.level.setData(type, d);
        }
    }

}
