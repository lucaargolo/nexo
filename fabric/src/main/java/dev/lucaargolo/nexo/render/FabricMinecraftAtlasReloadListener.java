package dev.lucaargolo.nexo.render;

import dev.lucaargolo.nexo.NexoMinecraft;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class FabricMinecraftAtlasReloadListener implements IdentifiableResourceReloadListener {

    private final MinecraftAtlas atlas;

    public FabricMinecraftAtlasReloadListener(@NotNull MinecraftAtlas atlas) {
        this.atlas = atlas;
    }

    @Override
    public @NotNull ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(NexoMinecraft.MOD_ID, "atlas_reload");
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(
            @NotNull PreparationBarrier barrier,
            @NotNull ResourceManager resourceManager,
            @NotNull ProfilerFiller preparationsProfiler,
            @NotNull ProfilerFiller reloadProfiler,
            @NotNull Executor backgroundExecutor,
            @NotNull Executor gameExecutor
    ) {
        return atlas.reload(barrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
    }

}
