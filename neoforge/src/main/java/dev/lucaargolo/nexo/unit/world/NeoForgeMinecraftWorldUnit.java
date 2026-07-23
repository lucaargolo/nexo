package dev.lucaargolo.nexo.unit.world;

import dev.lucaargolo.nexo.NeoForgeNexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.role.Role;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeoForgeMinecraftWorldUnit extends MinecraftWorldUnit<NeoForgeNexoRegistryHandler> {

    public NeoForgeMinecraftWorldUnit(@NotNull NeoForgeNexoRegistryHandler helper, @NotNull WorldBase feature, @Nullable Role role, @NotNull Level level) {
        super(helper, feature, role, level);
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        AttachmentType<D> type = helper.getDataAttachment(data);
        return this.feature.data().contains(data) ? this.level.getData(type) : this.level.getExistingDataOrNull(type);
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        AttachmentType<D> type = helper.getDataAttachment(data);
        if (d == null) {
            this.level.removeData(type);
        } else {
            this.level.setData(type, d);
        }
    }

}
