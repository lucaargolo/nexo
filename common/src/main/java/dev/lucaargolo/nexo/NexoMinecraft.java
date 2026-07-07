package dev.lucaargolo.nexo;

import com.google.common.collect.Maps;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.Event;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.NexoBlock;
import dev.lucaargolo.nexo.api.feature.data.NexoData;
import dev.lucaargolo.nexo.api.feature.dimension.NexoDimension;
import dev.lucaargolo.nexo.api.feature.item.NexoItem;
import dev.lucaargolo.nexo.api.feature.item.NexoItemCategory;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.feature.*;
import dev.lucaargolo.nexo.model.NexoModelHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class NexoMinecraft implements Nexo {

    public static final String MOD_ID = "nexo";
    public static final Logger LOGGER = LoggerFactory.getLogger("Nexo");

    private static NexoMinecraft INSTANCE;

    private static final Map<ResourceLocation, Location> ID_CACHE = new ConcurrentHashMap<>();
    private static final Map<Location, Model> MODEL_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<Location, Feature<?>>> FEATURE_REGISTRY = new ConcurrentHashMap<>();

    private final NexoPlatformHelper<?> helper;

    protected final NexoModDiscovery<?> modDiscovery;
    protected final NexoModelHandler<?> modelLoader;

    private final Map<Class<?>, Map<Event.Priority, CopyOnWriteArrayList<Predicate<?>>>> listeners = new ConcurrentHashMap<>();

    public NexoMinecraft() {
        INSTANCE = this;
        this.helper = loadPlatformClass(NexoPlatformHelper.class, this);
        this.modDiscovery = loadPlatformClass(NexoModDiscovery.class, this);
        this.modelLoader = loadPlatformClass(NexoModelHandler.class, this);
    }

    public static NexoMinecraft getInstance() {
        return INSTANCE;
    }

    public static NexoPlatformHelper<?> getHelper() {
        return INSTANCE.helper;
    }

    protected final void init() {
        this.modDiscovery.init();
        this.modelLoader.init();
    }

    public abstract Side getSide();

    public abstract String getPlatform();

    public abstract boolean isModLoaded(String modId);

    @Override
    public @NotNull Logger getLogger() {
        return LOGGER;
    }

    @Override
    public byte @Nullable [] loadResource(@NotNull Location location) {
        // 1. Try Nexo mod (directory or JAR)
        Mod mod = getMod(location.namespace());
        if (mod != null) {
            String resource = location.path();
            Path modPath = mod.path();
            try {
                if (Files.isDirectory(modPath)) {
                    Path file = modPath.resolve(resource);
                    if (Files.isRegularFile(file)) {
                        return Files.readAllBytes(file);
                    }
                    // Fallback: classpath resource (resources dir separate from classes dir in dev)
                    URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(resource);
                    if (resourceUrl != null) {
                        try (InputStream is = resourceUrl.openStream()) {
                            return is.readAllBytes();
                        }
                    }
                } else {
                    try (FileSystem fs = FileSystems.newFileSystem(modPath, (ClassLoader) null)) {
                        Path file = fs.getPath(resource);
                        if (Files.isRegularFile(file)) {
                            return Files.readAllBytes(file);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to read resource {} from mod {}", location, mod.value(), e);
            }
        }

        // 2. Try Minecraft resource manager (any namespace, any resource type)
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
        try {
            Minecraft minecraft = Minecraft.getInstance();
            var optResource = minecraft.getResourceManager().getResource(rl);
            if (optResource.isPresent()) {
                try (InputStream is = optResource.get().open()) {
                    return is.readAllBytes();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read resource {} from Minecraft resource manager", location, e);
        }

        return null;
    }

    @Override
    public @NotNull <T extends Feature<T>> Map<Location, T> getFeatureRegistry(@NotNull Class<T> type) {
        return FEATURE_REGISTRY.getOrDefault(type, Map.of()).entrySet().stream()
                .filter(e -> type.isInstance(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> type.cast(e.getValue())));
    }

    @Override
    public @Nullable <T extends Feature<T>> T getFeature(@NotNull Class<T> type, @NotNull Location location) {
        Object feature = FEATURE_REGISTRY.computeIfAbsent(type, t -> new ConcurrentHashMap<>())
            .computeIfAbsent(location, i -> {
                RegistryAccess access = this.helper.getRegistryAccess();
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
                if(NexoData.class.isAssignableFrom(type)) {
                    return BuiltInRegistries.DATA_COMPONENT_TYPE.getHolder(id).map(MinecraftData::of).orElse(null);
                }else if(NexoBlock.class.isAssignableFrom(type)) {
                    return BuiltInRegistries.BLOCK.getHolder(id).map(MinecraftBlock::new).orElse(null);
                }else if(NexoItem.class.isAssignableFrom(type)) {
                    return BuiltInRegistries.ITEM.getHolder(id).map(MinecraftItem::new).orElse(null);
                }else if(NexoItemCategory.class.isAssignableFrom(type)) {
                    return BuiltInRegistries.CREATIVE_MODE_TAB.getHolder(id).map(MinecraftItemCategory::new).orElse(null);
                }else if(NexoDimension.class.isAssignableFrom(type)) {
                    return access.registry(Registries.LEVEL_STEM).flatMap(r -> r.getHolder(id)).map(MinecraftDimension::new).orElse(null);
                }else{
                    return null;
                }
            });
        return feature != null ? type.cast(feature) : null;
    }

    @Override
    public @NotNull <T extends Feature<T>> T registerFeature(@NotNull Feature<T> feature) {
        Class<? extends T> type = feature.type();
        Location location = feature.location();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
        switch (feature) {
            case NexoData<?> data when NexoData.class.isAssignableFrom(type) -> {
                MinecraftData<?> minecraftData = MinecraftData.register(id, data);
                emit(new FeatureRegisteredEvent(location, minecraftData));
                FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftData);
                return type.cast(minecraftData);
            }
            case NexoBlock block when NexoBlock.class.isAssignableFrom(type) -> {
                MinecraftBlock minecraftBlock = MinecraftBlock.register(id, block);
                emit(new FeatureRegisteredEvent(location, minecraftBlock));
                FEATURE_REGISTRY.computeIfAbsent(type, t -> new ConcurrentHashMap<>()).put(location, minecraftBlock);
                return type.cast(minecraftBlock);
            }
            case NexoItem item when NexoItem.class.isAssignableFrom(type) -> {
                MinecraftItem minecraftItem = MinecraftItem.register(id, item);
                emit(new FeatureRegisteredEvent(location, minecraftItem));
                FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftItem);
                return type.cast(minecraftItem);
            }
            case NexoItemCategory category when NexoItemCategory.class.isAssignableFrom(type) -> {
                MinecraftItemCategory minecraftCategory = MinecraftItemCategory.register(id, category);
                emit(new FeatureRegisteredEvent(location, minecraftCategory));
                FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftCategory);
                return type.cast(minecraftCategory);
            }
            case NexoDimension dimension when NexoDimension.class.isAssignableFrom(type) -> {
                MinecraftDimension minecraftDimension = MinecraftDimension.register(id, dimension);
                emit(new FeatureRegisteredEvent(location, minecraftDimension));
                FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftDimension);
                return type.cast(minecraftDimension);
            }
            default -> throw new IllegalStateException(String.format("Cannot register %s as %s", feature.getClass(), feature.type()));
        }
    }

    @Override
    public <E extends Event<T>, T> void on(@NotNull Class<E> eventType, @NotNull Event.Priority priority, @NotNull Predicate<E> listener) {
        listeners.computeIfAbsent(eventType, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(priority, k -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    @Override
    public <E extends Event<T>, T> void off(@NotNull Class<E> eventType, @NotNull Predicate<E> listener) {
        Map<Event.Priority, CopyOnWriteArrayList<Predicate<?>>> priorityMap = listeners.get(eventType);
        if (priorityMap != null) {
            for (CopyOnWriteArrayList<Predicate<?>> predicates : priorityMap.values()) {
                predicates.remove(listener);
            }
        }
    }

    @Override
    public <E extends Event<T>, T> @Nullable T emit(@NotNull E event) {
        Map<Event.Priority, CopyOnWriteArrayList<Predicate<?>>> priorityMap = listeners.get(event.getClass());
        boolean cancel = false;
        if (priorityMap != null) {
            for (Event.Priority priority : Event.Priority.values()) {
                List<Predicate<?>> predicates = priorityMap.get(priority);
                if (predicates != null) {
                    for (Predicate<?> predicate : predicates) {
                        @SuppressWarnings("unchecked")
                        Predicate<E> typedPredicate = (Predicate<E>) predicate;
                        cancel = cancel || !typedPredicate.test(event);
                    }
                }
            }
        }
        if(event.cancelable() && cancel) {
            return null;
        }else{
            return event.value();
        }
    }

    @Override
    public @Nullable Model getModel(@NotNull Location location) {
        return MODEL_CACHE.computeIfAbsent(location, (l) -> {
            Model model = Model.load(this, location);
            if (model != null) return model;

            if (getMod(location.namespace()) != null) {
                return null;
            }

            Location mcLocation = Location.of(location.namespace(), "models/" + location.path());
            byte[] data = loadResource(mcLocation);
            if (data != null) {
                return Model.load(this, mcLocation, data);
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public <M> M getMinecraftFeature(Feature<?> feature) {
        if (feature instanceof MinecraftBlock mb) return (M) mb.getHolder().value();
        if (feature instanceof MinecraftItem mi) return (M) mi.getHolder().value();
        if (feature instanceof MinecraftData<?> md) return (M) md.getHolder().value();
        if (feature instanceof MinecraftItemCategory mic) return (M) mic.getHolder().value();
        throw new IllegalArgumentException("Not a Minecraft feature: " + feature.getClass());
    }

    public <T> T loadPlatformClass(Class<T> clazz, Object... parameters) {
        return loadPlatformClass(null, clazz, parameters);
    }

    @SuppressWarnings("unchecked")
    public <T> T loadPlatformClass(String mod, Class<T> clazz, Object... parameters) {
        String originalName = clazz.getName();
        Class<?>[] parameterTypes = new Class<?>[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parameterTypes[i] = parameters[i].getClass();
        }

        String commonClassPrefix = mod == null ? this.getPlatform() : this.isModLoaded(mod) ? this.getPlatform() : "Empty";
        String commonClassName = originalName.substring(0, originalName.lastIndexOf('.')) + "." + commonClassPrefix + originalName.substring(originalName.lastIndexOf('.') + 1);
        String clientClassPrefix = "Client" + commonClassPrefix;
        String clientClassName = originalName.substring(0, originalName.lastIndexOf('.')) + "." + clientClassPrefix + originalName.substring(originalName.lastIndexOf('.') + 1);

        if(this.getSide().isClient()) {
            try {
                Class<? extends T> clientPlatformClass = (Class<? extends T>) clazz.getClassLoader().loadClass(clientClassName);
                return clientPlatformClass.getConstructor(parameterTypes).newInstance(parameters);
            } catch (Exception ignored) {}
        }
        try {
            Class<? extends T> commonPlatformClass = (Class<? extends T>) clazz.getClassLoader().loadClass(commonClassName);
            return commonPlatformClass.getConstructor(parameterTypes).newInstance(parameters);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static Location id(ResourceLocation location) {
        return ID_CACHE.computeIfAbsent(location, k -> Location.of(k.getNamespace(), k.getPath()));
    }

    public static Location id(ResourceKey<?> key) {
        return id(key.location());
    }

    public static <D> Codec<D> createCodec(NexoData<D> data) {
        return Codec.STRING.xmap(
                str -> data.deserialize(JsonParser.parseString(str)),
                obj -> data.serialize(obj).toString()
        );
    }

    public static <D> StreamCodec<RegistryFriendlyByteBuf, D> createPacketCodec(NexoData<D> data) {
        return new StreamCodec<>() {
            @Override
            public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull D value) {
                ByteBuffer buffer = data.write(value);
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                buf.writeVarInt(bytes.length);
                buf.writeBytes(bytes);
            }

            @Override
            public @NotNull D decode(@NotNull RegistryFriendlyByteBuf buf) {
                int length = buf.readVarInt();
                byte[] bytes = new byte[length];
                buf.readBytes(bytes);
                return data.read(ByteBuffer.wrap(bytes));
            }
        };
    }
}
