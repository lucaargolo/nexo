package dev.lucaargolo.nexo.event;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.Nullable;

public class ModelLoadingQueryEvent extends Event implements IModBusEvent {

    private final ResourceLocation id;
    @Nullable private UnbakedModel result;

    public ModelLoadingQueryEvent(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation id() {
        return id;
    }

    public void setResult(@Nullable UnbakedModel model) {
        this.result = model;
    }

    public @Nullable UnbakedModel getResult() {
        return result;
    }

}
