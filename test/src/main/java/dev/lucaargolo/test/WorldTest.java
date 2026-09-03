package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Ticker;
import dev.lucaargolo.nexo.api.feature.world.SimpleWorld;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import org.jetbrains.annotations.NotNull;

public final class WorldTest {

    private WorldTest() {
    }

    public static void register(@NotNull Nexo nexo) {
        nexo.registerFeature(new SimpleWorld(NexoTestMod.id("test")) {
            @Override
            public Ticker<WorldUnit> ticker() {
                return unit -> { };
            }
        });
    }
}
