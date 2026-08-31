package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class NeoForgeAttachmentData {

    private NeoForgeAttachmentData() {
    }

    public static @NotNull List<@NotNull DataBase<?>> data(@NotNull NeoForgeNexoMinecraft nexo, @NotNull IAttachmentHolder target, @NotNull List<@NotNull DataBase<?>> additionalData, @NotNull CompoundTag tag, @NotNull DataBase<?> serializedData) {
        List<@NotNull DataBase<?>> list = nexo.getRegistryHandler().getAttachedData(target);
        CompoundTag attachmentTag = tag.contains(AttachmentHolder.ATTACHMENTS_NBT_KEY) ? tag.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY) : null;
        if (attachmentTag != null) {
            for (DataBase<?> data : list) {
                AttachmentType<?> type = nexo.getRegistryHandler().getDataAttachment(data);
                ResourceLocation id = NeoForgeRegistries.ATTACHMENT_TYPES.getKey(type);
                if (id != null) {
                    attachmentTag.remove(id.toString());
                }
            }
            if (attachmentTag.isEmpty()) {
                tag.remove(AttachmentHolder.ATTACHMENTS_NBT_KEY);
            }
        }
        list.addAll(additionalData);
        if (!tag.isEmpty()) {
            list.add(serializedData);
        }
        return list;
    }

    public static @Nullable <D> D getData(@NotNull NeoForgeNexoMinecraft nexo, @NotNull List<@NotNull DataBase<?>> initialData, @NotNull IAttachmentHolder target, @NotNull DataBase<D> data) {
        AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
        return initialData.contains(data) ? target.getData(type) : target.getExistingDataOrNull(type);
    }

    public static <D> void setData(@NotNull NeoForgeNexoMinecraft nexo, @NotNull IAttachmentHolder target, @NotNull DataBase<D> data, @Nullable D value) {
        AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
        if (value == null) {
            target.removeData(type);
        } else {
            target.setData(type, value);
        }
    }

}
