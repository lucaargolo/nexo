package dev.lucaargolo.nexo;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.Event;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.Ticker;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.packet.Packet;
import dev.lucaargolo.nexo.api.feature.packet.PacketReceiver;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.language.Language;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.shader.Shader;
import dev.lucaargolo.nexo.api.render.shader.ShaderSource;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemCategoryUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.packet.MinecraftPacket;
import dev.lucaargolo.nexo.feature.packet.MinecraftPacketPayload;
import dev.lucaargolo.nexo.language.MinecraftLanguageHandler;
import dev.lucaargolo.nexo.mixed.UnitCacheMixed;
import dev.lucaargolo.nexo.render.MinecraftRenderingHandler;
import dev.lucaargolo.nexo.resource.MinecraftResourceType;
import dev.lucaargolo.nexo.unit.block.MinecraftBlockUnit;
import dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemCategoryUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import dev.lucaargolo.nexo.unit.screen.MinecraftScreenUnit;
import dev.lucaargolo.nexo.unit.world.MinecraftWorldUnit;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public abstract class NexoMinecraft<N extends NexoMinecraft<N, D, H, R>, D extends NexoModDiscoveryHandler<N>, H extends MinecraftRegistryHandler<N>, R extends MinecraftRenderingHandler<N>> implements Nexo {

    public static final String MOD_ID = "nexo";
    public static final Logger LOGGER = LoggerFactory.getLogger("Nexo");

    private static final int MAX_DATA_SIZE = 1 << 20;

    private static final Map<Location, ResourceLocation> RL_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Location> ID_CACHE = new ConcurrentHashMap<>();

    protected final D discoveryHandler;
    protected final H registryHandler;
    protected final R renderingHandler;
    protected final MinecraftLanguageHandler languageHandler;

    private final Map<Class<?>, Map<Event.Priority, CopyOnWriteArrayList<Predicate<?>>>> listeners = new ConcurrentHashMap<>();
    public final Map<Feature.Type<?, ?>, Map<Location, Feature<?, ?>>> directRegistry = new ConcurrentHashMap<>();

    public NexoMinecraft() {
        this.discoveryHandler = Utils.loadPlatformClass(this, NexoModDiscoveryHandler.class, this);
        this.registryHandler = Utils.loadPlatformClass(this, MinecraftRegistryHandler.class, this);
        this.renderingHandler = Utils.loadPlatformClass(this, MinecraftRenderingHandler.class, this);
        this.languageHandler = new MinecraftLanguageHandler(this);
    }

    protected final void init() {
        this.registerResource(Resource.Type.FONT, Graphics2D.DEFAULT_FONT);
        this.registryHandler.init();
        this.renderingHandler.init();
        this.registryHandler.beginFeatureRegistration();
        try {
            this.discoveryHandler.init();
        } finally {
            this.registryHandler.endFeatureRegistration();
        }
    }

    public D getDiscoveryHandler() {
        return discoveryHandler;
    }

    public H getRegistryHandler() {
        return registryHandler;
    }

    public R getRenderingHandler() {
        return renderingHandler;
    }

    public MinecraftLanguageHandler getLanguageHandler() {
        return languageHandler;
    }

    public abstract Side getSide();

    public abstract String getPlatform();

    public abstract String getMapping(@NotNull Class<?> ownerType, @NotNull String memberName, @NotNull Class<?> returnType, Class<?>... parameterTypes);

    public abstract boolean isModLoaded(String modId);

    public abstract MinecraftServer getServer();

    public abstract Player createFakePlayer(Level level, UUID uuid, String name);

    @Override
    public @NotNull Logger getLogger() {
        return LOGGER;
    }

    @Override
    public @NotNull Language language() {
        return languageHandler;
    }

    @Override
    public @NotNull Shader createShader(@NotNull ShaderSource source) {
        return renderingHandler.shaderHandler().createShader(source);
    }

    @Override
    public byte @Nullable [] loadResource(@NotNull Location location) {
        // 1. Try resources bundled with the Nexo API
        if (MOD_ID.equals(location.namespace())) {
            try (InputStream is = Nexo.class.getResourceAsStream("/" + location.path())) {
                if (is != null) {
                    return is.readAllBytes();
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to read bundled Nexo API resource {}", location);
            }
        }

        // 2. Try Nexo mod (directory or JAR)
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

        // 3. Try Minecraft resource manager (any namespace, any resource type)
        try {
            ResourceLocation rl = NexoMinecraft.rl(location);
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
    public @Nullable <T extends Feature<T, U>, U extends Unit<T, ?>> T getFeature(@NotNull Feature.Type<T, U> type, @NotNull Location location) {
        return MinecraftFeatureType.of(type).lookup(this, location);
    }

    @Override
    public @NotNull <T extends Feature<T, U>, U extends Unit<T, ?>, F extends T> F registerFeature(@NotNull F feature) {
        for (Feature.Type<?, ?> type : Feature.Type.values()) {
            MinecraftFeatureType<?, ?, ?> t = MinecraftFeatureType.of(type);
            if (t.isInstance(feature)) {
                t.register(this, feature);
                return feature;
            }
        }
        throw new IllegalStateException(String.format("Cannot register %s", feature.getClass()));
    }

    @Override
    public @Nullable <T extends Feature<T, U>, U extends Unit<T, ?>> U unit(@NotNull Feature<T, U> feature) {
        return MinecraftFeatureType.of(feature.type()).base(this, feature);
    }

    @Override
    public final void sendPacket(@NotNull PacketReceiver receiver, @NotNull Packet<?, ?> packet) {
        Packet<?, ?> registered = MinecraftPacket.lookup(packet.location());
        if (registered == null) {
            throw new IllegalArgumentException("Cannot send unregistered packet: " + packet.location());
        }
        sendMinecraftPacket(receiver, new MinecraftPacketPayload(packet));
    }

    protected abstract void sendMinecraftPacket(@NotNull PacketReceiver receiver, @NotNull MinecraftPacketPayload payload);

    public final void handleMinecraftPacket(@NotNull MinecraftPacketPayload payload, @NotNull PacketReceiver receiver) {
        payload.packet().dispatch(receiver, ByteBuffer.wrap(payload.data()));
    }

    @Override
    public @Nullable <T extends Resource<T>> T getResource(@NotNull Resource.Type<T> type, @NotNull Location location) {
        return MinecraftResourceType.of(type).lookup(this, location);
    }

    public @NotNull <T extends Resource<T>> T registerResource(@NotNull Resource.Type<T> type, @NotNull Location location) {
        return MinecraftResourceType.of(type).register(this, location);
    }

    public @NotNull <T extends Resource<T>> T registerResource(@NotNull Resource.Type<T> type, @NotNull Location location, byte @NotNull [] data) {
        return MinecraftResourceType.of(type).register(this, location, data);
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
                        Class<Predicate<E>> clazz = Nexo.type(Predicate.class);
                        try {
                            cancel = cancel || !clazz.cast(predicate).test(event);
                        } catch (Exception e) {
                            LOGGER.error("Failed to emit event {} to listener {}", event.getClass().getSimpleName(), predicate, e);
                        }
                    }
                }
            }
        }
        if (event.cancelable() && cancel) {
            return null;
        }
        return event.value();
    }

    public @NotNull BlockUnit<?> stateToUnit(@NotNull BlockState state) {
        return blockToUnit(null, null, state, null);
    }

    public @NotNull BlockUnit<?> blockToUnit(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return blockToUnit(level, pos, state, level.getBlockEntity(pos));
    }

    public @NotNull BlockUnit<?> blockToUnit(@Nullable Level level, @Nullable BlockPos pos, @NotNull BlockState state, @Nullable BlockEntity blockEntity) {
        return blockToUnit(level, pos, state, blockEntity, null);
    }

    public @NotNull BlockUnit<?> blockToUnit(@Nullable Level level, @Nullable BlockPos pos, @NotNull BlockState state, @Nullable BlockEntity blockEntity, @Nullable Direction direction) {
        UnitCacheMixed cache = blockEntity != null ? (UnitCacheMixed) blockEntity : (UnitCacheMixed) state;
        MinecraftBlockUnit<?, ?> cached = (MinecraftBlockUnit<?, ?>) cache.nexo$getUnit();
        if (cached != null) {
            return cached;
        }
        BlockBase block = MinecraftFeatureType.BLOCK.convert(this, state.getBlock());
        MinecraftBlockUnit<?, ?> unit = Utils.loadPlatformClass(this, MinecraftBlockUnit.class, this, block, block.role(), level, pos, state, blockEntity, direction);
        cache.nexo$setUnit(unit);
        return unit;
    }

    public @NotNull ItemUnit<?> stackToUnit(@NotNull ItemStack stack) {
        UnitCacheMixed cache = (UnitCacheMixed) (Object) stack;
        MinecraftItemUnit<?> cached = (MinecraftItemUnit<?>) cache.nexo$getUnit();
        if (cached != null) {
            return cached;
        }
        ItemBase item = MinecraftFeatureType.ITEM.convert(this, stack.getItem());
        MinecraftItemUnit<?> unit = Utils.loadPlatformClass(this, MinecraftItemUnit.class, this, item, item.role(), stack);
        cache.nexo$setUnit(unit);
        return unit;
    }

    public @NotNull WorldUnit<?> levelToUnit(@NotNull Level level) {
        UnitCacheMixed cache = (UnitCacheMixed) level;
        MinecraftWorldUnit<?> cached = (MinecraftWorldUnit<?>) cache.nexo$getUnit();
        if (cached != null) {
            return cached;
        }
        ResourceKey<LevelStem> key = Registries.levelToLevelStem(level.dimension());
        Location location = NexoMinecraft.id(key.location());
        WorldBase world = MinecraftFeatureType.WORLD.lookup(location);
        if (world == null) {
            throw new IllegalStateException("Couldn't find world from level");
        }
        MinecraftWorldUnit<?> unit = Utils.loadPlatformClass(this, MinecraftWorldUnit.class, this, world, world.role(), level);
        cache.nexo$setUnit(unit);
        return unit;
    }

    public @NotNull <E extends Entity> MinecraftEntityUnit<?, ?, E> entityToUnit(@NotNull E entity) {
        UnitCacheMixed cache = (UnitCacheMixed) entity;
        MinecraftEntityUnit<?, ?, ?> cached = (MinecraftEntityUnit<?, ?, ?>) cache.nexo$getUnit();
        if (cached != null) {
            Class<MinecraftEntityUnit<?, ?, E>> cachedClass = Nexo.type(MinecraftEntityUnit.class);
            return cachedClass.cast(cached);
        }
        EntityBase feature = MinecraftFeatureType.ENTITY.convert(this, entity.getType());
        MinecraftEntityUnit<?, ?, ?> unit = Utils.loadPlatformClass(this, MinecraftEntityUnit.class, this, feature, feature.role(), entity);
        cache.nexo$setUnit(unit);
        Class<MinecraftEntityUnit<?, ?, E>> clazz = Nexo.type(MinecraftEntityUnit.class);
        return clazz.cast(unit);
    }

    public ItemCategoryUnit<?> tabToUnit(CreativeModeTab tab) {
        UnitCacheMixed cache = (UnitCacheMixed) tab;
        MinecraftItemCategoryUnit<?, ?> cached = (MinecraftItemCategoryUnit<?, ?>) cache.nexo$getUnit();
        if (cached != null) {
            return cached;
        }
        ItemCategoryBase feature = MinecraftFeatureType.ITEM_CATEGORY.convert(this, tab);
        MinecraftItemCategoryUnit<?, ?> unit = Utils.loadPlatformClass(this, MinecraftItemCategoryUnit.class, this, feature, feature.role(), tab);
        cache.nexo$setUnit(unit);
        return unit;
    }

    public MinecraftScreenUnit<?> screenToUnit(@NotNull Screen screen) {
        UnitCacheMixed cache = (UnitCacheMixed) screen;
        MinecraftScreenUnit<?> cached = (MinecraftScreenUnit<?>) cache.nexo$getUnit();
        if (cached != null) {
            return cached;
        }
        ScreenBase feature = MinecraftFeatureType.SCREEN.convert(this, screen);
        MinecraftScreenUnit<?> unit = Utils.loadPlatformClass(this, MinecraftScreenUnit.class, this, feature, feature.role(), screen);
        cache.nexo$setUnit(unit);
        return unit;
    }

    public void tickWorld(@NotNull Level level) {
        WorldUnit<?> unit = this.levelToUnit(level);
        WorldBase world = unit.feature();
        Ticker<WorldUnit<?>> ticker = world.ticker();
        if (ticker != null) {
            ticker.tick(unit);
        }
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
                if (length < 0 || length > MAX_DATA_SIZE || length > buf.readableBytes()) {
                    throw new IllegalArgumentException("Invalid data length " + length);
                }
                byte[] bytes = new byte[length];
                buf.readBytes(bytes);
                return data.read(ByteBuffer.wrap(bytes));
            }
        };
    }



}
