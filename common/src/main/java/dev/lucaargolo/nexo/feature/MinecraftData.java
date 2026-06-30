package dev.lucaargolo.nexo.feature;

import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.feature.data.IData;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class MinecraftData<D> extends MinecraftFeature<DataComponentType<D>, IData<D>> implements IData<D> {

    public MinecraftData(Holder<DataComponentType<D>> holder, IData<D> delegate) {
        super(holder, delegate);
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull D data) {
        return this.getDelegate().write(data);
    }

    @Override
    public @NotNull D read(@NotNull ByteBuffer buffer) {
        return this.getDelegate().read(buffer);
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull D data) {
        return this.getDelegate().serialize(data);
    }

    @Override
    public @NotNull D deserialize(@NotNull JsonElement element) {
        return this.getDelegate().deserialize(element);
    }
}
