package dev.lucaargolo.nexo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {

    @Unique
    private static final ThreadLocal<Boolean> IS_SERVER = ThreadLocal.withInitial(() -> false);

    @WrapOperation(
            method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resources/RegistryDataLoader;load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;"
            )
    )
    private static RegistryAccess.Frozen nexo$wrapIsServerCall(
            @Coerce Object loadingFunction,
            RegistryAccess registryAccess,
            List<RegistryDataLoader.RegistryData<?>> entries,
            Operation<RegistryAccess.Frozen> original
    ) {
        try {
            IS_SERVER.set(true);
            return original.call(loadingFunction, registryAccess, entries);
        } finally {
            IS_SERVER.set(false);
        }
    }

    @Inject(
            method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
                    ordinal = 0
            )
    )
    private static void nexo$registerDynamicFeatures(
            @Coerce Object loadingFunction,
            RegistryAccess registryAccess,
            List<RegistryDataLoader.RegistryData<?>> registryData,
            CallbackInfoReturnable<RegistryAccess.Frozen> cir,
            @Local(ordinal = 1) List<RegistryDataLoader.Loader<?>> loaders
    ) {
        if (!IS_SERVER.get()) return;

        Map<ResourceKey<?>, Consumer<Registry<?>>> features = NexoRegistryHandler.get().getDynamicRegistrars();
        if (features.isEmpty()) return;

        for (RegistryDataLoader.Loader<?> loader : loaders) {
            ResourceKey<? extends Registry<?>> registryKey = loader.data().key();
            features.forEach((key, registrar) -> {
                if (key.registryKey().equals(registryKey)) {
                    registrar.accept(loader.registry());
                }
            });
        }
    }

}
