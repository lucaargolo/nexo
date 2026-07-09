package dev.lucaargolo.nexo.world;

import dev.lucaargolo.nexo.NeoForgeNexoRegistryHandler;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.NexoData;
import dev.lucaargolo.nexo.api.feature.world.NexoWorld;
import dev.lucaargolo.nexo.instance.world.MinecraftWorldInstance;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeoForgeMinecraftWorldInstance extends MinecraftWorldInstance {

    public NeoForgeMinecraftWorldInstance(@NotNull NexoMinecraft nexo, @NotNull NexoWorld feature, @NotNull Level level) {
        super(nexo, feature, level);
    }

    @Override
    public @Nullable <D> D getData(@NotNull NexoData<D> data) {
        AttachmentType<D> type = NeoForgeNexoRegistryHandler.getDataAttachment(data);
        return this.level.getExistingDataOrNull(type);
    }

    @Override
    public <D> void setData(@NotNull NexoData<D> data, @Nullable D d) {
        AttachmentType<D> type = NeoForgeNexoRegistryHandler.getDataAttachment(data);
        if (d == null) {
            this.level.removeData(type);
        }else {
            this.level.setData(type, d);
        }
    }

}
