package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.item.SimpleItemCategory;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.feature.world.BiomeBase;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.image.ImageResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public class NexoTestMod {

    public static final String MOD_ID = "nexo_test";

    public NexoTestMod(@NotNull Nexo nexo) {
        nexo.registerResource(Resource.Type.FONT, id("fonts/fira_mono"));
        nexo.registerResource(Resource.Type.FONT, id("fonts/source_sans_3_regular"));
        nexo.registerResource(Resource.Type.FONT, id("fonts/source_serif_4_regular"));
        nexo.registerResource(Resource.Type.IMAGE, id("test_block_jpeg"));
        nexo.registerResource(Resource.Type.IMAGE, id("test_block_webp"));
        nexo.registerResource(Resource.Type.IMAGE, id("test_block_translucent"));
        ItemCategoryBase category = nexo.registerFeature(new SimpleItemCategory(NexoTestMod.id("test")));
        ScreenBase testScreen = ScreenTest.register(nexo);
        BlockTest.register(nexo, category);
        PacketTest.register(nexo, category, testScreen);
        WorldTest.register(nexo);
        EntityTest.register(nexo);

        // API round trips: events, mod lookup, resource loading, data-backed resource registration, biomes.
        AtomicBoolean eventReceived = new AtomicBoolean();
        Predicate<FeatureRegisteredEvent> listener = event -> {
            eventReceived.set(true);
            return true;
        };
        nexo.on(FeatureRegisteredEvent.class, listener);
        nexo.emit(new FeatureRegisteredEvent(NexoTestMod.id("test_event"), category));
        nexo.off(FeatureRegisteredEvent.class, listener);
        if (!eventReceived.get()) {
            throw new IllegalStateException("FeatureRegisteredEvent was not delivered to listeners");
        }
        if (nexo.getMod(MOD_ID) == null) {
            throw new IllegalStateException("getMod failed for the test mod");
        }
        byte[] png = requireNonNull(nexo.loadResource(id("test_block.png")), "Missing test_block.png resource");
        nexo.registerResource(Resource.Type.IMAGE, id("test_block_registered.png"), png);
        if (nexo.getResource(Resource.Type.IMAGE, id("test_block_registered.png")) == null) {
            throw new IllegalStateException("registerResource/getResource round trip failed");
        }
        BiomeBase biome = nexo.registerFeature(new BiomeBase(NexoTestMod.id("test_biome")) {
        });
        if (nexo.getFeature(Feature.Type.BIOME, id("test_biome")) != biome) {
            throw new IllegalStateException("Biome feature round trip failed");
        }
    }

    public static Location id(String path) {
        return Location.of(MOD_ID, path);
    }
}
