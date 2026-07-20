package dev.lucaargolo.nexo.unit.world;

import dev.lucaargolo.nexo.FabricNexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.role.Role;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class FabricMinecraftWorldUnit extends MinecraftWorldUnit<FabricNexoRegistryHandler> {

    public FabricMinecraftWorldUnit(@NotNull FabricNexoRegistryHandler helper, @NotNull WorldBase feature, @Nullable Role role, @NotNull Level level) {
        super(helper, feature, role, level);
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        AttachmentType<D> type = helper.getDataAttachment(data);
        return this.level.getAttached(type);
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        AttachmentType<D> type = helper.getDataAttachment(data);
        this.level.setAttached(type, d);
    }

}
