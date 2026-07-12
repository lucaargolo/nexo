package dev.lucaargolo.nexo;

import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.LazyHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.attachment.AttachmentType;
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
    public <T> Holder<T> registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature) {
        DeferredRegister<T> deferredRegistry = (DeferredRegister<T>) deferredRegistries
                .computeIfAbsent(registry, r -> new HashMap<>())
                .computeIfAbsent(id.getNamespace(), n -> {
                    DeferredRegister<?> r = DeferredRegister.create(registry, id.getNamespace());
                    r.register(this.nexo().modBus());
                    return r;
                });
        return deferredRegistry.register(id.getPath(), feature);
    }

    @Override
    public <T> LazyHolder<T> registerDynamicFeature(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation id, Supplier<T> feature) {
        ResourceKey<T> key = ResourceKey.create(registryKey, id);
        dynamicFeatures.put(key, feature);
        return new LazyHolder<>(this.nexo(), key);
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
        Holder<AttachmentType<?>> holder = registerBuiltinFeature(NeoForgeRegistries.ATTACHMENT_TYPES, id, builder::build);
        dataAttachmentMap.put(data, holder);
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
