package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.item.SimpleItemCategory;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

public class NexoTestMod {

    public static final String MOD_ID = "nexo_test";

    public static final Location TEST_FONT = NexoTestMod.id("fonts/fira_mono.ttf");

    public NexoTestMod(@NotNull Nexo nexo) {
        nexo.registerResource(new TestTtfFontResource(TEST_FONT, () -> nexo.loadResource(TEST_FONT)));
        ItemCategoryBase category = nexo.registerFeature(new SimpleItemCategory(NexoTestMod.id("test")));
        ScreenBase testScreen = ScreenTest.register(nexo);
        BlockTest.register(nexo, category);
        PacketTest.register(nexo, category, testScreen);
        WorldTest.register(nexo);
        EntityTest.register(nexo);
    }

    public static Location id(String path) {
        return Location.of(MOD_ID, path);
    }
}
