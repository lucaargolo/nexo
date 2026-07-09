package dev.lucaargolo.nexo.mixin;

import com.mojang.datafixers.util.Pair;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(WorldLoader.class)
public class WorldLoaderMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/RegistryDataLoader;load(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;"), method = "load", locals = LocalCapture.CAPTURE_FAILSOFT)
    private static <D, R> void nexo$collectLoadingRegistry(
            WorldLoader.InitConfig pInitConfig,
            WorldLoader.WorldDataSupplier<D> pWorldDataSupplier,
            WorldLoader.ResultFactory<D, R> pResultFactory,
            Executor pBackgroundExecutor,
            Executor pGameExecutor,
            CallbackInfoReturnable<CompletableFuture<R>> cir,
            Pair<WorldDataConfiguration, CloseableResourceManager> pair,
            CloseableResourceManager closeableresourcemanager,
            LayeredRegistryAccess<RegistryLayer> layeredregistryaccess,
            LayeredRegistryAccess<RegistryLayer> layeredregistryaccess1,
            RegistryAccess.Frozen registryaccess$frozen) {
        NexoRegistryHandler.captureRegistry(registryaccess$frozen);
    }

    @Inject(at = @At("RETURN"), method = "load")
    private static <D, R> void nexo$collectLoadingRegistry(
            WorldLoader.InitConfig pInitConfig,
            WorldLoader.WorldDataSupplier<D> pWorldDataSupplier,
            WorldLoader.ResultFactory<D, R> pResultFactory,
            Executor pBackgroundExecutor,
            Executor pGameExecutor,
            CallbackInfoReturnable<CompletableFuture<R>> cir) {
        NexoRegistryHandler.captureRegistry(null);
    }


}
