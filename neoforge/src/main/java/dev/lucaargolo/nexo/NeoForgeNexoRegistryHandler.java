package dev.lucaargolo.nexo;

import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.event.DynamicRegistrySetupEvent;
import dev.lucaargolo.nexo.event.WorldDimensionsBakeEvent;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import dev.lucaargolo.nexo.util.DynamicRegistryView;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.callback.AddCallback;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NeoForgeNexoRegistryHandler extends NexoRegistryHandler<NeoForgeNexoMinecraft> {

    private final Map<Registry<?>, Map<String, DeferredRegister<?>>> deferredRegistries = new HashMap<>();
    private final Map<DataBase<?>, Holder<AttachmentType<?>>> dataAttachmentMap = new LinkedHashMap<>();

    public NeoForgeNexoRegistryHandler(NeoForgeNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        MinecraftFeatureType.all().forEach(this::addBuiltinRegistryListener);
        NeoForge.EVENT_BUS.addListener(DynamicRegistrySetupEvent.class, event -> {
            MinecraftFeatureType.all().forEach(type -> this.addDynamicRegistryListener(event.view(), type));
            dynamicRegistrars.forEach((key, registrar) -> {
                event.view().getOptional(key.registryKey()).ifPresent(registrar);
            });
        });
        NeoForge.EVENT_BUS.addListener(WorldDimensionsBakeEvent.class, event -> {
            event.dimensions().forEach((key, stem) -> {
                Holder<LevelStem> holder = event.registry().getHolderOrThrow(key);
                MinecraftFeatureType.WORLD.index(this, holder);
            });
        });
    }

    @Override
    public <T> Holder<T> registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature) {
        DeferredRegister<T> deferredRegistry = getOrCreateDeferredRegister(registry, id.getNamespace());
        return deferredRegistry.register(id.getPath(), feature);
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

    private <M> void addBuiltinRegistryListener(MinecraftFeatureType<?, ?, M> type) {
        RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registry(type.registry()).ifPresent(registry -> {
            Consumer<Holder<M>> consumer = (holder) -> {
                this.nexo().emit(new FeatureRegisteredEvent(NexoMinecraft.id(holder), type.index(this, holder)));
            };
            registry.addCallback((AddCallback<M>) (r, raw, id, value) -> {
                consumer.accept(r.getHolder(raw).orElseThrow());
            });
            registry.holders().toList().forEach(consumer);
        });
    }

    private <M> void addDynamicRegistryListener(DynamicRegistryView view, MinecraftFeatureType<?, ?, M> type) {
        view.registerEntryAdded(type.registry(), (r, raw, id, value) -> {
            Holder.Reference<M> holder = view.getOptional(type.registry()).flatMap(registry -> registry.getHolder(raw)).orElseThrow();
            this.nexo().emit(new FeatureRegisteredEvent(NexoMinecraft.id(id), type.index(this, holder)));
            dynamicHolders.put(holder.key(), holder);
        });
    }


    private <R> DeferredRegister<R> getOrCreateDeferredRegister(Registry<R> registry, String namespace) {
        DeferredRegister<?> deferredRegister = deferredRegistries
            .computeIfAbsent(registry, r -> new HashMap<>())
            .computeIfAbsent(namespace, n -> {
                DeferredRegister<R> r = DeferredRegister.create(registry, namespace);
                r.register(this.nexo().modBus());
                return r;
            });
        Class<DeferredRegister<R>> clazz = Nexo.type(DeferredRegister.class);
        return clazz.cast(deferredRegister);
    }


    public <D> @NotNull AttachmentType<D> getDataAttachment(@NotNull DataBase<D> data) {
        Class<AttachmentType<D>> clazz = Nexo.type(AttachmentType.class);
        return clazz.cast(dataAttachmentMap.get(data).value());
    }

}
