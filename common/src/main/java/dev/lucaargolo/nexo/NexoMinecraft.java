package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Identifier;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Block;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NexoMinecraft implements Nexo {

    public static final String MOD_ID = "nexo";

    private static final Map<ResourceLocation, Identifier> ID_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, MinecraftBlock> BLOCK_CACHE = new ConcurrentHashMap<>();

    protected static void cacheBlock(Identifier id, MinecraftBlock block) {
        BLOCK_CACHE.put(id, block);
    }

    @Override
    @SuppressWarnings("unchecked")
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

    public static Identifier id(ResourceLocation location) {
        return ID_CACHE.computeIfAbsent(location, k -> Identifier.of(k.getNamespace(), k.getPath()));
    }
}
