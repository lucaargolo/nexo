package dev.lucaargolo.nexo;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.Event;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemCategoryUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.render.NexoRenderingHandler;
import dev.lucaargolo.nexo.resource.MinecraftResourceType;
import dev.lucaargolo.nexo.unit.block.MinecraftBlockUnit;
import dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemCategoryUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import dev.lucaargolo.nexo.unit.world.MinecraftWorldUnit;
import dev.lucaargolo.nexo.util.NexoUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public abstract class NexoMinecraft implements Nexo {

    public static final String MOD_ID = "nexo";
    public static final Logger LOGGER = LoggerFactory.getLogger("Nexo");

    private static final Map<Location, ResourceLocation> RL_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Location> ID_CACHE = new ConcurrentHashMap<>();

    protected final NexoModDiscoveryHandler<?> discoveryHandler;
    protected final NexoRegistryHandler<?> registryHandler;
    protected final NexoRenderingHandler<?> renderingHandler;

    private final Map<Class<?>, Map<Event.Priority, CopyOnWriteArrayList<Predicate<?>>>> listeners = new ConcurrentHashMap<>();

    public NexoMinecraft() {
        this.discoveryHandler = NexoUtils.loadPlatformClass(this, NexoModDiscoveryHandler.class, this);
        this.registryHandler = NexoUtils.loadPlatformClass(this, NexoRegistryHandler.class, this);
        this.renderingHandler = NexoUtils.loadPlatformClass(this, NexoRenderingHandler.class, this);
    }

    protected final void init() {
        this.registryHandler.init();
        this.renderingHandler.init();
        this.discoveryHandler.init();
    }

    public abstract Side getSide();

    public abstract String getPlatform();

    public abstract boolean isModLoaded(String modId);

    public abstract MinecraftServer getServer();

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
                LOGGER.debug("Failed to read resource {} from mod {}", location, mod.value());
            }
        }

        // 2. Try Minecraft resource manager (any namespace, any resource type)
        ResourceLocation rl = NexoMinecraft.rl(location);
        try {
            Minecraft minecraft = Minecraft.getInstance();
            var optResource = minecraft.getResourceManager().getResource(rl);
            if (optResource.isPresent()) {
                try (InputStream is = optResource.get().open()) {
                    return is.readAllBytes();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to read resource {} from Minecraft resource manager", location);
        }

        return null;
    }

    @Override
    public @Nullable <T extends Feature<T>> T getFeature(@NotNull Feature.Type<T> type, @NotNull Location location) {
        Feature<?> feature = MinecraftFeatureType.of(type).lookup(location);
        return type.cast(feature);
    }

    @Override
    public @NotNull <T extends Feature<T>> T registerFeature(@NotNull T feature) {
        for (Feature.Type<?> type : Feature.Type.values()) {
            MinecraftFeatureType<?, ?> t = MinecraftFeatureType.of(type);
            if (t.isInstance(feature)) {
                t.register(this.registryHandler, feature);
                return feature;
            }
        }
        throw new IllegalStateException(String.format("Cannot register %s", feature.getClass()));
    }

    @Override
    public @Nullable <T extends Resource<T>> T getResource(@NotNull Resource.Type<T> type, @NotNull Location location) {
        return MinecraftResourceType.of(type).lookup(this, location);
    }

    @Override
    public @NotNull <T extends Resource<T>> T registerResource(@NotNull T resource) {
        return MinecraftResourceType.of(resource.type()).register(this, resource);
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
                        try {
                            cancel = cancel || !typedPredicate.test(event);
                        } catch (Exception e) {
                            LOGGER.error("Failed to emit event {} to listener {}", event.getClass().getSimpleName(), predicate, e);
                        }
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

    public NexoRenderingHandler<?> getRenderingHandler() {
        return renderingHandler;
    }

    public @NotNull BlockUnit<?> stateToUnit(@NotNull BlockState state) {
        BlockBase block = MinecraftFeatureType.BLOCK.convert(this.registryHandler, state.getBlock());
        return new MinecraftBlockUnit(block, block.role(), state);
    }

    public @NotNull ItemUnit<?> stackToUnit(@NotNull ItemStack stack) {
        ItemBase item = MinecraftFeatureType.ITEM.convert(this.registryHandler, stack.getItem());
        return new MinecraftItemUnit(item, item.role(), stack);
    }

    public @NotNull ItemCategoryUnit<?> tabToUnit(@NotNull CreativeModeTab tab) {
        ItemCategoryBase itemCategory = MinecraftFeatureType.ITEM_CATEGORY.convert(this.registryHandler, tab);
        return NexoUtils.loadPlatformClass(this, MinecraftItemCategoryUnit.class, this.registryHandler, itemCategory, itemCategory.role(), tab);
    }

    public @NotNull WorldUnit<?> levelToUnit(@NotNull Level level) {
        ResourceKey<LevelStem> key = Registries.levelToLevelStem(level.dimension());
        Location location = NexoMinecraft.id(key.location());
        WorldBase world = MinecraftFeatureType.WORLD.lookup(location);
        assert world != null;
        return NexoUtils.loadPlatformClass(this, MinecraftWorldUnit.class, this.registryHandler, world, world.role(), level);
    }

    @SuppressWarnings("unchecked")
    public @NotNull <E extends Entity> MinecraftEntityUnit<?, ?, E> entityToUnit(@NotNull E entity) {
        EntityBase feature = MinecraftFeatureType.ENTITY.convert(this.registryHandler, entity.getType());
        return NexoUtils.loadPlatformClass(this, MinecraftEntityUnit.class, this.registryHandler, feature, feature.role(), entity);
    }

    public static ResourceLocation rl(Location location) {
        return RL_CACHE.computeIfAbsent(location, k -> ResourceLocation.fromNamespaceAndPath(k.namespace(), k.path()));
    }

    public static Location id(ResourceLocation location) {
        return ID_CACHE.computeIfAbsent(location, k -> Location.of(k.getNamespace(), k.getPath()));
    }

    public static Location id(ResourceKey<?> key) {
        return id(key.location());
    }

    public static Location id(Holder<?> holder) {
        return id(holder.unwrapKey().orElseThrow());
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
