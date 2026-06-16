package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Identifier;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Block;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public abstract class NexoMinecraft implements Nexo {

    public static final String MOD_ID = "nexo";
    public static final Logger LOGGER = LoggerFactory.getLogger("Nexo");

    private static final Map<ResourceLocation, Identifier> ID_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, MinecraftBlock> BLOCK_CACHE = new ConcurrentHashMap<>();

    private static NexoMinecraft instance;

    private final NexoModDiscovery modDiscovery;

    public NexoMinecraft() {
        instance = this;
        this.modDiscovery = loadPlatformClass(NexoModDiscovery.class);
    }

    protected final void init() {
        this.modDiscovery.discover(this);
    }

    public final NexoModDiscovery getModDiscovery() {
        return this.modDiscovery;
    }

    @Override
    public @Nullable <T extends Feature> T get(Class<T> type, Identifier id) {
        if(type.isAssignableFrom(Block.class)) {
            return (T) BLOCK_CACHE.computeIfAbsent(id, i -> BuiltInRegistries.BLOCK
                    .getHolder(ResourceLocation.fromNamespaceAndPath(id.namespace(), id.path()))
                    .map(MinecraftBlock::new)
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

    public static Identifier id(ResourceLocation location) {
        return ID_CACHE.computeIfAbsent(location, k -> Identifier.of(k.getNamespace(), k.getPath()));
    }

    protected static void cacheBlock(Identifier id, MinecraftBlock block) {
        BLOCK_CACHE.put(id, block);
    }

}
