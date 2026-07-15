package dev.lucaargolo.nexo;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.NexoException;
import dev.lucaargolo.nexo.api.event.Event;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.role.PlayerRole;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.entity.MinecraftEntity;
import dev.lucaargolo.nexo.model.NexoModelHandler;
import dev.lucaargolo.nexo.unit.block.MinecraftBlockUnit;
import dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit;
import dev.lucaargolo.nexo.unit.world.MinecraftWorldUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
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
        this.modelHandler.init();
        this.discoveryHandler.init();
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
        Feature<?> feature = MinecraftFeatureType.of(type).lookup(this.registryHandler, location);
        return type.cast(feature);
    }

    @Override
    public @NotNull <T extends Feature<T>> T registerFeature(@NotNull T feature) {
        Location location = feature.location();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
        for (Feature.Type<?> type : Feature.Type.values()) {
            MinecraftFeatureType<?> t = MinecraftFeatureType.of(type);
            if (t.isInstance(feature)) {
                t.register(this.registryHandler, id, feature);
                this.emit(new FeatureRegisteredEvent(location, feature));
                return feature;
            }
        }
        throw new IllegalStateException(String.format("Cannot register %s", feature.getClass()));
    }

@NotNull
    public BlockUnit<?> stateToUnit(@NotNull BlockState state) {
        BlockBase block = this.getFeature(Feature.Type.BLOCK, NexoMinecraft.id(state.getBlockHolder().unwrapKey().orElseThrow()));
        assert block != null;
        return new MinecraftBlockUnit(this, block, block.role(), state);
    }

    @NotNull
    public WorldUnit<?> levelToUnit(@NotNull Level level) {
        WorldBase world = this.getFeature(Feature.Type.WORLD, NexoMinecraft.id(level.dimension()));
        assert world != null;
        return this.loadPlatformClass(MinecraftWorldUnit.class, this, world, world.role(), level);
    }

    @NotNull
    public EntityUnit<?> entityToUnit(@NotNull Entity entity) {
        EntityBase feature = this.getFeature(Feature.Type.ENTITY, NexoMinecraft.id(entity.getType().builtInRegistryHolder().unwrapKey().orElseThrow()));
        assert feature != null;
        Role role = feature.role();
        if (entity instanceof net.minecraft.world.entity.player.Player player && role == null) {
            role = new PlayerRole(player.getUUID(), player.getGameProfile().getName());
        }
        return this.loadPlatformClass(MinecraftEntityUnit.class, this, feature, role, entity);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <C extends Role> EntityUnit<C> entityToUnit(@NotNull Entity entity, @NotNull Class<C> roleType) {
        EntityBase feature = this.getFeature(Feature.Type.ENTITY, NexoMinecraft.id(entity.getType().builtInRegistryHolder().unwrapKey().orElseThrow()));
        assert feature != null;
        Role role = feature.role();
        if (entity instanceof net.minecraft.world.entity.player.Player player && role == null) {
            role = new PlayerRole(player.getUUID(), player.getGameProfile().getName());
        }
        C typedRole = roleType.cast(role);
        if (typedRole == null) {
            throw new IllegalArgumentException("Entity " + entity.getType() + " does not have role " + roleType.getName());
        }
        return (EntityUnit<C>) this.loadPlatformClass(MinecraftEntityUnit.class, this, feature, typedRole, entity);
    }

    public @NotNull Entity createEntity(@NotNull EntityType<?> type, @NotNull Level level, @NotNull EntityBase feature) {
        return MinecraftEntity.defaultEntity(type, level);
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
        if (event.cancelable() && cancel) {
            return null;
        } else {
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

        if (this.getSide().isClient()) {
            try {
                Class<? extends T> clientPlatformClass = clazz.getClassLoader().loadClass(clientClassName).asSubclass(clazz);
                return this.instantiate(clientPlatformClass, parameterTypes, parameters);
            } catch (Exception ignored) {
            }
        }
        try {
            Class<? extends T> commonPlatformClass = clazz.getClassLoader().loadClass(commonClassName).asSubclass(clazz);
            return this.instantiate(commonPlatformClass, parameterTypes, parameters);
        } catch (Exception exception) {
            throw new NexoException("Failed to load platform class for " + clazz.getName(), exception);
        }
    }

    private <T> T instantiate(Class<? extends T> type, Class<?>[] parameterTypes, Object[] parameters) throws ReflectiveOperationException {
        for (Constructor<?> constructor : type.getConstructors()) {
            Class<?>[] constructorTypes = constructor.getParameterTypes();
            if (constructorTypes.length != parameterTypes.length) continue;
            boolean compatible = true;
            for (int i = 0; i < constructorTypes.length; i++) {
                if (!constructorTypes[i].isAssignableFrom(parameterTypes[i])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                @SuppressWarnings("unchecked")
                T instance = (T) constructor.newInstance(parameters);
                return instance;
            }
        }
        throw new NoSuchMethodException(type.getName());
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

}
