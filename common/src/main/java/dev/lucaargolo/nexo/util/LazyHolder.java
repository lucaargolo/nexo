package dev.lucaargolo.nexo.util;

import dev.lucaargolo.nexo.NexoMinecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

import java.util.function.Supplier;

public class LazyHolder<T> implements Supplier<Holder<T>> {

    private final ResourceKey<T> key;
    private Holder<T> holder;

    public LazyHolder(ResourceKey<T> key) {
        this.key = key;
    }

    public LazyHolder(Holder<T> holder) {
        this.key = holder.unwrapKey().orElseThrow();
        this.holder = holder;
    }

    public ResourceKey<T> key() {
        return key;
    }

    @Override
    public Holder<T> get() {
        if (holder == null) {
            RegistryAccess access = NexoMinecraft.getHelper().getRegistry();
            return access.registry(key.registryKey()).flatMap(r -> r.getHolder(key)).orElse(null);
        }
        return holder;
    }

}
