package dev.lucaargolo.nexo.event;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.bus.api.Event;

import java.util.Map;

public class WorldDimensionsBakeEvent extends Event {

    private final Registry<LevelStem> registry;
    private final Map<ResourceKey<LevelStem>, LevelStem> dimensions;

    public WorldDimensionsBakeEvent(Registry<LevelStem> registry, Map<ResourceKey<LevelStem>, LevelStem> dimensions) {
        this.registry = registry;
        this.dimensions = dimensions;
    }

    public Registry<LevelStem> registry() {
        return registry;
    }

    public Map<ResourceKey<LevelStem>, LevelStem> dimensions() {
        return dimensions;
    }
}
