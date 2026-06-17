package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Location;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.event.IEvent;
import dev.lucaargolo.nexo.api.feature.IBlock;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public abstract class NexoMinecraft implements Nexo {

    public static final String MOD_ID = "nexo";
    public static final Logger LOGGER = LoggerFactory.getLogger("Nexo");

    private static final Map<ResourceLocation, Location> ID_CACHE = new ConcurrentHashMap<>();
    private static final Map<Location, IBlock> BLOCK_CACHE = new ConcurrentHashMap<>();

    private static NexoMinecraft instance;

    private final NexoModDiscovery modDiscovery;
    private final Map<Class<?>, List<Predicate<?>>> listeners = new ConcurrentHashMap<>();

    public NexoMinecraft() {
        instance = this;
        this.modDiscovery = loadPlatformClass(NexoModDiscovery.class);
        on(FeatureRegisteredEvent.class, event -> {
            if (event.feature() instanceof IBlock block) {
                BLOCK_CACHE.put(event.location(), block);
            }
            return true;
        });
    }

    protected final void init() {
        this.modDiscovery.discover(this);
    }

    public final NexoModDiscovery getModDiscovery() {
        return this.modDiscovery;
    }

    @Override
    public @Nullable <T extends IFeature> T getFeature(Class<T> type, Location location) {
        if(type.isAssignableFrom(IBlock.class)) {
            return (T) BLOCK_CACHE.computeIfAbsent(location, i -> BuiltInRegistries.BLOCK
                    .getHolder(ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path()))
                    .map(holder -> new MinecraftBlock(holder, null))
                    .orElse(null)
            );
        }
        return null;
    }

    public abstract String getPlatform();

    public abstract boolean isModLoaded(String modId);

    public static <T> T loadPlatformClass(Class<T> clazz, Object... parameters) {
        return loadPlatformClass(null, clazz, parameters);
    }

    public static <T> T loadPlatformClass(String mod, Class<T> clazz, Object... parameters) {
        String originalName = clazz.getName();
        String clazzPrefix = mod == null ? instance.getPlatform() : instance.isModLoaded(mod) ? instance.getPlatform() : "Empty";
        String clazzName = originalName.substring(0, originalName.lastIndexOf('.')) + "." + clazzPrefix + originalName.substring(originalName.lastIndexOf('.') + 1);
        Class<?>[] parameterTypes = new Class<?>[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parameterTypes[i] = parameters[i].getClass();
        }
        try {
            return (T) clazz.getClassLoader().loadClass(clazzName).getConstructor(parameterTypes).newInstance(parameters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Location id(ResourceLocation location) {
        return ID_CACHE.computeIfAbsent(location, k -> Location.of(k.getNamespace(), k.getPath()));
    }

    @Override
    public <E extends IEvent<T>, T> void on(@NotNull Class<E> eventType, @NotNull Predicate<E> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public <E extends IEvent<T>, T> void off(@NotNull Class<E> eventType, @NotNull Predicate<E> listener) {
        List<Predicate<?>> predicates = listeners.get(eventType);
        if (predicates != null) {
            predicates.remove(listener);
        }
    }

    @Override
    public <E extends IEvent<T>, T> T emit(@NotNull E event) {
        List<Predicate<?>> predicates = listeners.get(event.getClass());
        boolean cancel = false;
        if (predicates != null) {
            for (Predicate<?> predicate : predicates) {
                cancel = cancel || !((Predicate<E>) predicate).test(event);
            }
        }
        if(event.cancelable() && cancel) {
            return null;
        }else{
            return event.value();
        }
    }
}
