package dev.lucaargolo.nexo.unit.world;

import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.unit.NeoForgeAttachmentData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NeoForgeMinecraftWorldUnit extends MinecraftWorldUnit<NeoForgeNexoMinecraft> {

    public NeoForgeMinecraftWorldUnit(@NotNull NeoForgeNexoMinecraft nexo, @NotNull WorldBase feature, @Nullable Role role, @NotNull Level level) {
        super(nexo, feature, role, level);
    }

    @Override
    public @NotNull List<@NotNull DataBase<?>> data() {
        CompoundTag tag = new CompoundTag();
        CompoundTag attachments = this.level.serializeAttachments(this.level.registryAccess());
        if (attachments != null) {
            tag.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, attachments);
        }
        return NeoForgeAttachmentData.data(this.nexo, this.level, List.of(), tag, MinecraftFeatureType.DATA.convert(this.nexo, DataComponents.CUSTOM_DATA));
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        return NeoForgeAttachmentData.getData(this.nexo, this.feature.initialData(), this.level, data);
    }

    @NotNull
    @Override
    public <D> WorldUnit<Role> setData(@NotNull DataBase<D> data, @Nullable D d) {
        NeoForgeAttachmentData.setData(this.nexo, this.level, data, d);
        return this;
    }

}
