package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.IMod;
import dev.lucaargolo.nexo.api.Location;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.IBlock;
import dev.lucaargolo.nexo.api.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@IMod(
        value = "nexo_test",
        name = "Nexo Test Mod",
        description = "A test Nexo mod for development",
        version = "0.0.1",
        authors = {"D4rkness_King"}
)
public class NexoTestMod {

    public NexoTestMod(Nexo nexo) {
        Location location = Location.of("nexo_test", "test_block");
        Location texture = Location.of("nexo_test", "test_block");
        IBlock block = nexo.registerFeature(IBlock.class, location, new TestBlock(location, Model.full(texture)));
        System.out.println("Registered "+ block);
    }

    private record TestBlock(@NotNull Location location, @Nullable Model model) implements IBlock {
        @Override
        public @NotNull List<@NotNull Tag> tags() {
            return List.of();
        }
    }
}
