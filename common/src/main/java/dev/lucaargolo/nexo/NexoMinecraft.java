package dev.lucaargolo.nexo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.event.IEvent;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.feature.block.IBlock;
import dev.lucaargolo.nexo.api.feature.data.IData;
import dev.lucaargolo.nexo.api.feature.item.IItem;
import dev.lucaargolo.nexo.api.feature.item.IItemCategory;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.feature.*;
import dev.lucaargolo.nexo.model.NexoModelHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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

public abstract class NexoMinecraft implements Nexo {

    public static final String MOD_ID = "nexo";
    public static final Logger LOGGER = LoggerFactory.getLogger("Nexo");

    private static NexoMinecraft INSTANCE;

    private static final Map<ResourceLocation, Location> ID_CACHE = new ConcurrentHashMap<>();
    private static final Map<Location, Model> MODEL_CACHE = new ConcurrentHashMap<>();

    protected static final Map<IFeature.Type, Map<Location, IFeature>> FEATURE_REGISTRY = new ConcurrentHashMap<>();

    private final NexoPlatformHelper helper;

    protected final NexoModDiscovery modDiscovery;
    protected final NexoModelHandler modelLoader;

    private final Map<Class<?>, Map<IEvent.Priority, CopyOnWriteArrayList<Predicate<?>>>> listeners = new ConcurrentHashMap<>();

    public NexoMinecraft() {
        INSTANCE = this;
        this.helper = loadPlatformClass(NexoPlatformHelper.class);
        this.modDiscovery = loadPlatformClass(NexoModDiscovery.class);
        this.modelLoader = loadPlatformClass(NexoModelHandler.class);
        this.on(FeatureRegisteredEvent.class, event -> {
            IFeature feature = event.value();
            Map<Location, IFeature> cache = FEATURE_REGISTRY.computeIfAbsent(feature.type(), t -> new ConcurrentHashMap<>());
            cache.put(feature.location(), feature);
            return true;
        });
    }

    public static NexoMinecraft getInstance() {
        return INSTANCE;
    }

    public static NexoPlatformHelper getHelper() {
        return INSTANCE.helper;
    }

    protected final void init() {
        this.registerFeature(IData.COUNT);
        this.modDiscovery.init(this);
        this.modelLoader.init(this);
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
    public @NotNull Map<Location, IFeature> getFeatureRegistry(@NotNull IFeature.Type type) {
        return ImmutableMap.copyOf(FEATURE_REGISTRY.getOrDefault(type, Map.of()));
    }

    @Override
    public @Nullable IFeature getFeature(@NotNull IFeature.Type type, @NotNull Location location) {
        return FEATURE_REGISTRY.computeIfAbsent(type, t -> new ConcurrentHashMap<>())
            .computeIfAbsent(location, i -> {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
                Registry<?> registry = switch (type) {
                    case BLOCK -> BuiltInRegistries.BLOCK;
                    case DATA -> BuiltInRegistries.DATA_COMPONENT_TYPE;
                    case ITEM -> BuiltInRegistries.ITEM;
                    case ITEM_CATEGORY -> BuiltInRegistries.CREATIVE_MODE_TAB;
                };
                return registry.getHolder(id).map(holder -> switch (type) {
                    case BLOCK -> new MinecraftBlock((Holder<Block>) holder, null);
                    case DATA -> new MinecraftData(holder, null);
                    case ITEM -> new MinecraftItem((Holder<Item>) holder, null);
                    case ITEM_CATEGORY -> new MinecraftItemCategory((Holder<CreativeModeTab>) holder, null);
                }).orElse(null);
            });
    }

    @Override
    public @NotNull IFeature registerFeature(@NotNull IFeature feature) {
        IFeature.Type type = feature.type();
        Location location = feature.location();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
        switch (type) {
            case DATA:
                if(feature instanceof IData<?> data) {
                    MinecraftData<?> minecraftData = emit(new FeatureRegisteredEvent<>(location, MinecraftData.register(this, id, data)));
                    FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftData);
                    return minecraftData;
                }
            case BLOCK:
                if (feature instanceof IBlock block) {
                    MinecraftBlock minecraftBlock = emit(new FeatureRegisteredEvent<>(location, MinecraftBlock.register(this, id, block)));
                    FEATURE_REGISTRY.computeIfAbsent(type, t -> new ConcurrentHashMap<>()).put(location, minecraftBlock);
                    return minecraftBlock;
                }
                break;
            case ITEM:
                if(feature instanceof IItem item) {
                    MinecraftItem minecraftItem = emit(new FeatureRegisteredEvent<>(location, MinecraftItem.register(this, id, item)));
                    FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftItem);
                    return minecraftItem;
                }
                break;
            case ITEM_CATEGORY:
                if(feature instanceof IItemCategory category) {
                    MinecraftItemCategory minecraftCategory = emit(new FeatureRegisteredEvent<>(location, MinecraftItemCategory.register(this, id, category)));
                    FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftCategory);
                    return minecraftCategory;
                }
        }
        throw new IllegalStateException(String.format("Cannot register %s as %s", feature.getClass(), feature.type()));
    }

    @Override
    public <E extends IEvent<T>, T> void on(@NotNull Class<E> eventType, @NotNull IEvent.Priority priority, @NotNull Predicate<E> listener) {
        listeners.computeIfAbsent(eventType, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(priority, k -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    @Override
    public <E extends IEvent<T>, T> void off(@NotNull Class<E> eventType, @NotNull Predicate<E> listener) {
        Map<IEvent.Priority, CopyOnWriteArrayList<Predicate<?>>> priorityMap = listeners.get(eventType);
        if (priorityMap != null) {
            for (CopyOnWriteArrayList<Predicate<?>> predicates : priorityMap.values()) {
                predicates.remove(listener);
            }
        }
    }

    @Override
    public <E extends IEvent<T>, T> @Nullable T emit(@NotNull E event) {
        Map<IEvent.Priority, CopyOnWriteArrayList<Predicate<?>>> priorityMap = listeners.get(event.getClass());
        boolean cancel = false;
        if (priorityMap != null) {
            for (IEvent.Priority priority : IEvent.Priority.values()) {
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

    public @NotNull <T> T getMinecraftFeature(IFeature feature) {
        @SuppressWarnings("unchecked")
        MinecraftFeature<T, ?> mcFeature = (MinecraftFeature<T, ?>) feature;
        return mcFeature.getHolder().value();
    }

    public <T> T loadPlatformClass(Class<T> clazz, Object... parameters) {
        return loadPlatformClass(null, clazz, parameters);
    }

    public <T> T loadPlatformClass(String mod, Class<T> clazz, Object... parameters) {
        String originalName = clazz.getName();
        String clazzPrefix = mod == null ? this.getPlatform() : this.isModLoaded(mod) ? this.getPlatform() : "Empty";
        String clazzName = originalName.substring(0, originalName.lastIndexOf('.')) + "." + clazzPrefix + originalName.substring(originalName.lastIndexOf('.') + 1);
        Class<?>[] parameterTypes = new Class<?>[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parameterTypes[i] = parameters[i].getClass();
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends T> platformClazz = (Class<? extends T>) clazz.getClassLoader().loadClass(clazzName);
            return platformClazz.getConstructor(parameterTypes).newInstance(parameters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Location id(ResourceLocation location) {
        return ID_CACHE.computeIfAbsent(location, k -> Location.of(k.getNamespace(), k.getPath()));
    }

    public static <D> Codec<D> createCodec(IData<D> data) {
        return Codec.STRING.xmap(
                str -> data.deserialize(JsonParser.parseString(str)),
                obj -> data.serialize(obj).toString()
        );
    }

    public static <D> StreamCodec<RegistryFriendlyByteBuf, D> createPacketCodec(IData<D> data) {
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
