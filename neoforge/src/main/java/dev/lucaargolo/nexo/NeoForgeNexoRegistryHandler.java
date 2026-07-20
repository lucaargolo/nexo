package dev.lucaargolo.nexo;

import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class NeoForgeNexoRegistryHandler extends NexoRegistryHandler<NeoForgeNexoMinecraft> {

    private static final Map<DataBase<?>, Object> dataAttachmentMap = new LinkedHashMap<>();

    private final Map<Registry<?>, Map<String, DeferredRegister<?>>> deferredRegistries = new HashMap<>();

    public NeoForgeNexoRegistryHandler(NeoForgeNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public <T> void registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature) {
        DeferredRegister<T> deferredRegistry = getOrCreateDeferredRegister(registry, id.getNamespace());
        deferredRegistry.register(id.getPath(), feature);
    }

    @Override
    public <D> void registerDataAttachment(DataBase<D> data) {
        ResourceLocation id = NexoMinecraft.rl(data.location());
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
        DeferredRegister<AttachmentType<?>> deferredRegistry = getOrCreateDeferredRegister(NeoForgeRegistries.ATTACHMENT_TYPES, id.getNamespace());
        Holder<AttachmentType<?>> holder = deferredRegistry.register(id.getPath(), builder::build);
        dataAttachmentMap.put(data, holder);
    }

    public CreativeModeTab craftCreativeTab(ItemCategoryBase category) {
        Location location = category.location();
        Component title = Component.translatable("itemGroup."+location.namespace()+"."+location.path());
        return CreativeModeTab.builder().title(title).displayItems((parameters, output) -> {
            MinecraftItemCategory.ITEM_MAP.getOrDefault(category, List.of()).forEach(item -> {
                output.accept(MinecraftFeatureType.ITEM.convert(item));
            });
        }).build();
    }

    @Override
    protected RegistryAccess getLocalRegistry() {
        return null;
    }

    @SuppressWarnings("unchecked")
    private <R> DeferredRegister<R> getOrCreateDeferredRegister(Registry<R> registry, String namespace) {
        return (DeferredRegister<R>) deferredRegistries.computeIfAbsent(registry, r -> new HashMap<>())
            .computeIfAbsent(namespace, n -> {
                DeferredRegister<R> r = DeferredRegister.create(registry, namespace);
                r.register(this.nexo().modBus());
                return r;
            });
    }

    @SuppressWarnings("unchecked")
    public static <D> @NotNull AttachmentType<D> getDataAttachment(@NotNull DataBase<D> data) {
        return (AttachmentType<D>) ((Holder<AttachmentType<?>>) dataAttachmentMap.get(data)).value();
    }

}
