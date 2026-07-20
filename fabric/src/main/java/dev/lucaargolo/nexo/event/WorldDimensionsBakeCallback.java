package dev.lucaargolo.nexo.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.Map;

public interface WorldDimensionsBakeCallback  {

    Event<WorldDimensionsBakeCallback> EVENT = EventFactory.createArrayBacked(WorldDimensionsBakeCallback.class, callbacks -> (registry, dimensions) -> {
        for (WorldDimensionsBakeCallback callback : callbacks) {
            callback.onBake(registry, dimensions);
        }
    });

    void onBake(Registry<LevelStem> registry, Map<ResourceKey<LevelStem>, LevelStem> dimensions);

}
