package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.event.PlayerBlockInteractEvent;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.SimpleItemCategory;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.feature.world.BiomeBase;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.api.resource.language.LanguageResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public class NexoTestMod {

    public static final String MOD_ID = "nexo_test";

    public NexoTestMod(@NotNull Nexo nexo) {
        nexo.registerResource(Resource.Type.FONT, id("fonts/fira_mono"));
        nexo.registerResource(Resource.Type.FONT, id("fonts/source_sans_3_regular"));
        nexo.registerResource(Resource.Type.FONT, id("fonts/source_serif_4_regular"));
        LanguageResource runtimeLanguage = nexo.registerResource(
                Resource.Type.LANGUAGE,
                id("languages/en_us.json"),
                "{\"text.nexo_test.programmatic\":\"Programmatic language\"}".getBytes(StandardCharsets.UTF_8)
        );
        runtimeLanguage.entry("text.nexo_test.programmatic", "[i]Direct language entry[/i]");
        if (!"[i]Direct language entry[/i]".equals(nexo.language().translate("text.nexo_test.programmatic"))) {
            throw new IllegalStateException("LanguageResource direct entry failed");
        }
        for (String path : List.of("en_us.json", "lang/es_es.json", "language/fr.json", "languages/de_de.json")) {
            requireNonNull(
                    nexo.getResource(Resource.Type.LANGUAGE, id(path)),
                    "Missing language resource " + path
            );
        }
        nexo.registerResource(Resource.Type.IMAGE, id("test_block_jpeg"));
        nexo.registerResource(Resource.Type.IMAGE, id("test_block_webp"));
        nexo.registerResource(Resource.Type.IMAGE, id("test_block_translucent"));
        ItemCategoryBase category = nexo.registerFeature(new SimpleItemCategory(NexoTestMod.id("test")));
        ScreenBase testScreen = ScreenTest.register(nexo);
        ScreenUnit<?> testScreenUnit = requireNonNull(nexo.unit(testScreen), "Missing test screen unit");
        if (testScreenUnit.feature() != testScreen) {
            throw new IllegalStateException("Screen unit did not retain its registered feature");
        }
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

        // Vanilla-backed features must expose their native blockstate properties and data components as initial data.
        BlockBase campfire = requireNonNull(
                nexo.getFeature(Feature.Type.BLOCK, Location.of("minecraft", "campfire")),
                "Vanilla campfire block was not indexed"
        );
        DataBase.Constrained<Boolean> lit = findConstrained(campfire.initialData(), "lit");
        requireNonNull(lit, "Campfire lit state was not exposed as initial data");
        DataBase.Constrained<Boolean> signalFire = findConstrained(campfire.initialData(), "signal_fire");
        requireNonNull(signalFire, "Campfire signal_fire state was not exposed as initial data");
        BlockUnit<?> campfireUnit = requireNonNull(nexo.unit(campfire), "Missing campfire unit");
        if (!Boolean.TRUE.equals(campfireUnit.getData(lit)) || !Boolean.FALSE.equals(campfireUnit.getData(signalFire))) {
            throw new IllegalStateException("Campfire blockstate initial data mismatch");
        }
        campfireUnit.setData(lit, Boolean.FALSE);
        if (!Boolean.FALSE.equals(campfireUnit.getData(lit))) {
            throw new IllegalStateException("Campfire blockstate initial data write failed");
        }
        ItemBase apple = requireNonNull(
                nexo.getFeature(Feature.Type.ITEM, Location.of("minecraft", "apple")),
                "Vanilla apple item was not indexed"
        );
        DataBase<Integer> maxStackSize = findData(apple.initialData(), "max_stack_size");
        requireNonNull(maxStackSize, "Apple max_stack_size component was not exposed as initial data");
        ItemUnit<?> appleUnit = requireNonNull(nexo.unit(apple), "Missing apple unit");
        if (!Integer.valueOf(64).equals(appleUnit.getData(maxStackSize))) {
            throw new IllegalStateException("Apple max_stack_size initial data mismatch");
        }

        // Right clicking a redstone lamp cycles its lit blockstate instead of the vanilla interaction.
        nexo.on(PlayerBlockInteractEvent.class, event -> {
            BlockUnit<?> block = event.value();
            if (!block.feature().location().equals(Location.of("minecraft", "redstone_lamp"))) {
                return true;
            }
            DataBase.Constrained<Boolean> lampLit = findConstrained(block.feature().initialData(), "lit");
            requireNonNull(lampLit, "Redstone lamp lit state was not exposed as initial data");
            block.withData(lampLit, lampLit::cycle);
            return false;
        });
    }

    private static <T extends Comparable<T>> @Nullable DataBase.Constrained<T> findConstrained(@NotNull List<@NotNull DataBase<?>> data, @NotNull String name) {
        for (DataBase<?> entry : data) {
            if (entry instanceof DataBase.Constrained<?> constrained && constrained.name().equals(name)) {
                Class<DataBase.Constrained<T>> clazz = Nexo.type(DataBase.Constrained.class);
                return clazz.cast(constrained);
            }
        }
        return null;
    }

    private static <D> @Nullable DataBase<D> findData(@NotNull List<@NotNull DataBase<?>> data, @NotNull String pathSuffix) {
        for (DataBase<?> entry : data) {
            if (entry.location().path().endsWith(pathSuffix)) {
                Class<DataBase<D>> clazz = Nexo.type(DataBase.class);
                return clazz.cast(entry);
            }
        }
        return null;
    }

    public static Location id(String path) {
        return Location.of(MOD_ID, path);
    }
}
