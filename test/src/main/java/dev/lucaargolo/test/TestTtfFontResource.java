package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.resource.font.FontResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class TestTtfFontResource extends FontResource.TTF {

    public TestTtfFontResource(@NotNull Location location, @NotNull Supplier<byte @Nullable []> supplier) {
        super(location, supplier);
    }
}
