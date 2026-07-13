package dev.lucaargolo.nexo;

import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NeoForgeNexoRegistryHandler extends NexoRegistryHandler<NeoForgeNexoMinecraft> {

    private static final Map<DataBase<?>, Holder<AttachmentType<?>>> dataAttachmentMap = new LinkedHashMap<>();

    private final Map<Registry<?>, Map<String, DeferredRegister<?>>> deferredRegistries = new HashMap<>();

    public NeoForgeNexoRegistryHandler(NeoForgeNexoMinecraft nexo) {
        super(nexo);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, T extends R> NexoHolder<R, T> registerBuiltinFeature(Registry<R> registry, ResourceLocation id, Supplier<T> feature) {
        ResourceKey<R> key = ResourceKey.create(registry.key(), id);
        DeferredRegister<R> deferredRegistry = (DeferredRegister<R>) deferredRegistries
                .computeIfAbsent(registry, r -> new HashMap<>())
                .computeIfAbsent(id.getNamespace(), n -> {
                    DeferredRegister<?> r = DeferredRegister.create(registry, id.getNamespace());
                    r.register(this.nexo().modBus());
                    return r;
                });
        DeferredHolder<R, T> registered = deferredRegistry.register(id.getPath(), feature);
        return new NexoHolder<>(this.nexo(), key, registered);

    }

    @Override
    public <R, T extends R> NexoHolder<R, T> registerDynamicFeature(ResourceKey<? extends Registry<R>> registryKey, ResourceLocation id, Supplier<T> feature, Class<T> type) {
        ResourceKey<R> key = ResourceKey.create(registryKey, id);
        dynamicFeatures.put(key, feature);
        return new NexoHolder<>(this.nexo(), key, type);
    }

    @Override
    public <D> void registerDataAttachment(DataBase<D> data) {
        Location location = data.location();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
        AttachmentType.Builder<D> builder = AttachmentType.builder(data::initial);
        if (data.persistent()) {
            Codec<D> codec = NexoMinecraft.createCodec(data);
            builder.serialize(codec);
            builder.copyOnDeath();
        }
        if (data.synced()) {
            StreamCodec<RegistryFriendlyByteBuf, D> codec = NexoMinecraft.createPacketCodec(data);
            builder.sync(codec);
        }
        NexoHolder<AttachmentType<?>, AttachmentType<?>> holder = registerBuiltinFeature(NeoForgeRegistries.ATTACHMENT_TYPES, id, builder::build);
        dataAttachmentMap.put(data, holder.holder());
    }

    public Supplier<CreativeModeTab> createCreativeTab(ItemCategoryBase category) {
        Location location = category.location();
        Component title = Component.translatable("itemGroup."+location.namespace()+"."+location.path());
        return () -> CreativeModeTab.builder().title(title).build();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <D> AttachmentType<D> getDataAttachment(DataBase<D> data) {
        return (AttachmentType<D>) dataAttachmentMap.get(data).value();
    }

}
