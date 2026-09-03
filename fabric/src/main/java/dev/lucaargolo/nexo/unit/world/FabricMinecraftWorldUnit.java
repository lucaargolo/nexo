package dev.lucaargolo.nexo.unit.world;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.unit.FabricAttachmentData;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class FabricMinecraftWorldUnit extends MinecraftWorldUnit<FabricNexoMinecraft> {

    public FabricMinecraftWorldUnit(@NotNull FabricNexoMinecraft nexo, @NotNull WorldBase feature, @Nullable Role role, @NotNull Level level) {
        super(nexo, feature, role, level);
    }

    @Override
    public @NotNull List<@NotNull DataBase<?>> data() {
        AttachmentTargetImpl target = (AttachmentTargetImpl) this.level;
        CompoundTag tag = new CompoundTag();
        target.fabric_writeAttachmentsToNbt(tag, this.level.registryAccess());
        return FabricAttachmentData.data(this.nexo, this.level, List.of(), tag, MinecraftFeatureType.DATA.convert(this.nexo, DataComponents.CUSTOM_DATA));
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        return FabricAttachmentData.getData(this.nexo, this.feature.initialData(), this.level, data);
    }

    @NotNull
    @Override
    public <D> WorldUnit setData(@NotNull DataBase<D> data, @Nullable D d) {
        FabricAttachmentData.setData(this.nexo, this.level, data, d);
        return this;
    }

}
