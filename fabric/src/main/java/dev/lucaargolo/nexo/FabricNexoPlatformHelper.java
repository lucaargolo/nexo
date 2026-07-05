package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.feature.item.BaseItemCategory;
import dev.lucaargolo.nexo.api.util.Location;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class FabricNexoPlatformHelper extends NexoPlatformHelper<FabricNexoMinecraft> {

    @Nullable
    private static MinecraftServer currentServer;

    public FabricNexoPlatformHelper(FabricNexoMinecraft nexo) {
        super(nexo);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);
    }

    @SuppressWarnings("unchecked")
    public <T, F extends T> Holder<F> registerFeature(Registry<T> registry, ResourceLocation id, Supplier<F> feature) {
        return (Holder<F>) Registry.registerForHolder(registry, id, feature.get());
    }

    public Supplier<CreativeModeTab> createCreativeTab(BaseItemCategory category) {
        Location location = category.location();
        Component title = Component.translatable("itemGroup."+location.namespace()+"."+location.path());
        return () -> FabricItemGroup.builder().title(title).build();
    }

    public RegistryAccess getRegistryAccess() {
        if (currentServer != null) {
            if(currentServer.isSameThread()) {
                return currentServer.registryAccess();
            }else{
                return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            }
        }
        return RegistryAccess.EMPTY;
    }

}
