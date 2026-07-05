package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.feature.item.BaseItemCategory;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NeoForgeNexoPlatformHelper extends NexoPlatformHelper<NeoForgeNexoMinecraft> {

    private final Map<Registry<?>, Map<String, DeferredRegister<?>>> deferredRegistries = new HashMap<>();

    public NeoForgeNexoPlatformHelper(NeoForgeNexoMinecraft nexo) {
        super(nexo);
    }

    @SuppressWarnings("unchecked")
    public <T, F extends T> Holder<F> registerFeature(Registry<T> registry, ResourceLocation id, Supplier<F> feature) {
        DeferredRegister<T> deferredRegistry = (DeferredRegister<T>) deferredRegistries
                .computeIfAbsent(registry, r -> new HashMap<>())
                .computeIfAbsent(id.getNamespace(), n -> {
                    DeferredRegister<?> r = DeferredRegister.create(registry, id.getNamespace());
                    r.register(this.nexo().modBus());
                    return r;
                });

        return (Holder<F>) deferredRegistry.register(id.getPath(), feature);
    }

    public Supplier<CreativeModeTab> createCreativeTab(BaseItemCategory category) {
        Location location = category.location();
        Component title = Component.translatable("itemGroup."+location.namespace()+"."+location.path());
        return () -> CreativeModeTab.builder().title(title).build();
    }

    public RegistryAccess getRegistryAccess() {
        MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
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
