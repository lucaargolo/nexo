package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public final class FabricAttachmentData {

    private FabricAttachmentData() {
    }

    public static @NotNull List<@NotNull DataBase<?>> data(@NotNull FabricNexoMinecraft nexo, @NotNull AttachmentTarget target, @NotNull List<@NotNull DataBase<?>> additionalData, @NotNull CompoundTag tag, @NotNull DataBase<?> serializedData) {
        List<@NotNull DataBase<?>> list = new ArrayList<>();
        CompoundTag attachmentTag = tag.contains(AttachmentTarget.NBT_ATTACHMENT_KEY) ? tag.getCompound(AttachmentTarget.NBT_ATTACHMENT_KEY) : null;
        Map<AttachmentType<?>, ?> attachments = ((AttachmentTargetImpl) target).fabric_getAttachments();
        if (attachments != null) {
            for (AttachmentType<?> type : attachments.keySet()) {
                DataBase<?> data = nexo.getRegistryHandler().getAttachmentData(type);
                if (data != null) {
                    list.add(data);
                    if (attachmentTag != null) {
                        attachmentTag.remove(type.identifier().toString());
                    }
                }
            }
        }
        if (attachmentTag != null && attachmentTag.isEmpty()) {
            tag.remove(AttachmentTarget.NBT_ATTACHMENT_KEY);
        }
        list.addAll(additionalData);
        if (!tag.isEmpty()) {
            list.add(serializedData);
        }
        return list;
    }

    public static @Nullable <D> D getData(@NotNull FabricNexoMinecraft nexo, @NotNull List<@NotNull DataBase<?>> initialData, @NotNull AttachmentTarget target, @NotNull DataBase<D> data) {
        AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
        return initialData.contains(data) ? target.getAttachedOrCreate(type) : target.getAttached(type);
    }

    public static <D> void setData(@NotNull FabricNexoMinecraft nexo, @NotNull AttachmentTarget target, @NotNull DataBase<D> data, @Nullable D value) {
        AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
        target.setAttached(type, value);
    }

}
