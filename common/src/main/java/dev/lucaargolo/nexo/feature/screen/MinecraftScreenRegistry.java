package dev.lucaargolo.nexo.feature.screen;

import dev.lucaargolo.nexo.MinecraftRegistryHandler;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.Bijection;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MinecraftScreenRegistry {

    public static final ResourceKey<Registry<ScreenBase>> REGISTRY = ResourceKey.createRegistryKey(
            NexoMinecraft.rl(Location.of(NexoMinecraft.MOD_ID, "screen"))
    );

    private static final Map<Location, ScreenBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final Map<Location, Holder<ScreenBase>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static final Bijection<ScreenBase, Holder<ScreenBase>> CONVERT = new Bijection<>() {
        @Override
        public Holder<ScreenBase> forward(ScreenBase screen) {
            return HOLDER_MAP.get(screen.location());
        }

        @Override
        public ScreenBase backward(Holder<ScreenBase> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    private MinecraftScreenRegistry() {
    }

    public static @Nullable ScreenBase lookup(@NotNull Location location) {
        return FEATURE_MAP.get(location);
    }

    public static @NotNull ScreenBase register(@NotNull MinecraftRegistryHandler<?> helper, @NotNull ScreenBase screen) {
        ScreenBase registered = FEATURE_MAP.putIfAbsent(screen.location(), screen);
        if (registered != null) {
            return registered;
        }
        Holder<ScreenBase> holder = helper.registerBuiltinFeature(REGISTRY, NexoMinecraft.rl(screen.location()), () -> screen);
        HOLDER_MAP.put(screen.location(), holder);
        return screen;
    }

    public static @NotNull ScreenBase index(@NotNull MinecraftRegistryHandler<?> helper, @NotNull Holder<ScreenBase> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, key -> holder.value());
    }

    public static @NotNull ScreenBase craft(@NotNull MinecraftRegistryHandler<?> helper, @NotNull ScreenBase screen) {
        return screen;
    }

}
