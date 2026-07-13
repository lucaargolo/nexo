package dev.lucaargolo.nexo;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.NexoException;
import dev.lucaargolo.nexo.api.event.Event;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.feature.block.MinecraftBlock;
import dev.lucaargolo.nexo.feature.data.MinecraftData;
import dev.lucaargolo.nexo.feature.item.MinecraftItem;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import dev.lucaargolo.nexo.feature.world.MinecraftWorld;
import dev.lucaargolo.nexo.unit.block.MinecraftBlockUnit;
import dev.lucaargolo.nexo.unit.world.MinecraftWorldUnit;
import dev.lucaargolo.nexo.model.NexoModelHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.LevelStem;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class NexoMinecraft implements Nexo {

    public static final String MOD_ID = "nexo";
    public static final Logger LOGGER = LoggerFactory.getLogger("Nexo");

    private static final Map<ResourceLocation, Location> ID_CACHE = new ConcurrentHashMap<>();
    private static final Map<Location, Model> MODEL_CACHE = new ConcurrentHashMap<>();

    protected final NexoModDiscoveryHandler<?> discoveryHandler;
    protected final NexoRegistryHandler<?> registryHandler;
    protected final NexoModelHandler<?> modelHandler;

    private final Map<Class<?>, Map<Event.Priority, CopyOnWriteArrayList<Predicate<?>>>> listeners = new ConcurrentHashMap<>();

    public NexoMinecraft() {
        this.discoveryHandler = loadPlatformClass(NexoModDiscoveryHandler.class, this);
        this.registryHandler = loadPlatformClass(NexoRegistryHandler.class, this);
        this.modelHandler = loadPlatformClass(NexoModelHandler.class, this);
    }

    protected final void init() {
        this.discoveryHandler.init();
        this.modelHandler.init();
    }

    public abstract Side getSide();

    public abstract String getPlatform();

    public abstract boolean isModLoaded(String modId);

    public abstract MinecraftServer getServer();

    public RegistryAccess getRegistry() {
        return this.registryHandler.getRegistry();
    }

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
    public @Nullable <T extends Feature<T>> T getFeature(@NotNull Feature.Type<T> type, @NotNull Location location) {
        return FeatureType.type(type).lookup(this.registryHandler, location).map(type::cast).orElse(null);
    }

    @Override
    public @NotNull <T extends Feature<T>> T registerFeature(@NotNull T feature) {
        Location location = feature.location();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
        for(Feature.Type<?> type : Feature.Type.values()) {
            FeatureType<?, ?> t = FeatureType.type(type);
            if(t.tryRegister(this.registryHandler, id, feature).isPresent()) {
                return feature;
            }
        }
        throw new IllegalStateException(String.format("Cannot register %s", feature.getClass()));
    }

    @NotNull
    public BlockUnit block(@NotNull BlockState state) {
        BlockBase block = this.getFeature(Feature.Type.BLOCK, NexoMinecraft.id(state.getBlockHolder().unwrapKey().orElseThrow()));
        assert block != null;
        return new MinecraftBlockUnit(this, block, state);
    }

    @NotNull
    public WorldUnit world(@NotNull Level level) {
        WorldBase world = this.getFeature(Feature.Type.WORLD, NexoMinecraft.id(level.dimension()));
        assert world != null;
        return this.loadPlatformClass(MinecraftWorldUnit.class, this, world, level);
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

    public <T> T loadPlatformClass(Class<T> clazz, Object... parameters) {
        return loadPlatformClass(null, clazz, parameters);
    }

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
                Class<? extends T> clientPlatformClass = clazz.getClassLoader().loadClass(clientClassName).asSubclass(clazz);
                return clientPlatformClass.getConstructor(parameterTypes).newInstance(parameters);
            } catch (Exception ignored) {}
        }
        try {
            Class<? extends T> commonPlatformClass = clazz.getClassLoader().loadClass(commonClassName).asSubclass(clazz);
            return commonPlatformClass.getConstructor(parameterTypes).newInstance(parameters);
        } catch (Exception exception) {
            throw new NexoException("Failed to load platform class for " + clazz.getName(), exception);
        }
    }

    public static Location id(ResourceLocation location) {
        return ID_CACHE.computeIfAbsent(location, k -> Location.of(k.getNamespace(), k.getPath()));
    }

    public static Location id(ResourceKey<?> key) {
        return id(key.location());
    }

    public static <D> Codec<D> createCodec(DataBase<D> data) {
        return Codec.STRING.xmap(
                str -> data.deserialize(JsonParser.parseString(str)),
                obj -> data.serialize(obj).toString()
        );
    }

    public static <D> StreamCodec<RegistryFriendlyByteBuf, D> createPacketCodec(DataBase<D> data) {
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

    @FunctionalInterface
    private interface FeatureRegistrar<T extends Feature<T>> {
        T register(NexoRegistryHandler<?> helper, ResourceLocation id, T feature);
    }

    @FunctionalInterface
    private interface FeatureLookup<T extends Feature<T>, M> {
        T lookup(NexoRegistryHandler<?> helper, Location location);
    }

    @FunctionalInterface
    private interface FeatureCraftar<T extends Feature<T>, M> {
        M craft(T feature);
    }

    private record FeatureType<T extends Feature<T>, M>(Class<T> type, FeatureRegistrar<T> registrar, FeatureLookup<T, M> nexar, FeatureCraftar<T, M> craftar) {

        private static final FeatureType<BlockBase, Block> BLOCK = new FeatureType<>(BlockBase.class, MinecraftBlock::register, MinecraftBlock::lookup, MinecraftBlock::craft);
        private static final FeatureType<ItemBase, Item> ITEM = new FeatureType<>(ItemBase.class, MinecraftItem::register, MinecraftItem::lookup, MinecraftItem::craft);
        private static final FeatureType<ItemCategoryBase, CreativeModeTab> ITEM_CATEGORY = new FeatureType<>(ItemCategoryBase.class, MinecraftItemCategory::register, MinecraftItemCategory::lookup, MinecraftItemCategory::craft);
        private static final FeatureType<WorldBase, LevelStem> WORLD = new FeatureType<>(WorldBase.class, MinecraftWorld::register, MinecraftWorld::lookup, MinecraftWorld::craft);

        private Optional<T> lookup(NexoRegistryHandler<?> helper, Location id) {
            return Optional.ofNullable(nexar.lookup(helper, id));
        }

        private Optional<T> tryRegister(NexoRegistryHandler<?> helper, ResourceLocation id, Feature<?> feature) {
            if(type.isInstance(feature)) {
                return Optional.of(registrar.register(helper, id, type.cast(feature)));
            }else{
                return Optional.empty();
            }
        }

        @NotNull
        private static FeatureType<?, ?> type(Feature.Type<?> type) {
            if (type == Feature.Type.BLOCK) {
                return BLOCK;
            }else if (type == Feature.Type.ITEM) {
                return ITEM;
            }else if (type == Feature.Type.ITEM_CATEGORY) {
                return ITEM_CATEGORY;
            }else if (type == Feature.Type.WORLD) {
                return WORLD;
            }
            throw new UnsupportedOperationException("Unsupported feature type: " + type);
        }

    }








}
