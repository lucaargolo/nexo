package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Location;
import dev.lucaargolo.nexo.api.Mod;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.IBlock;
import dev.lucaargolo.nexo.api.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Mod(
        value = NexoTestMod.MOD_ID,
        name = "Nexo Test Mod",
        description = "A test Nexo mod for development",
        version = "0.0.1",
        authors = {"D4rkness_King"}
)
public class NexoTestMod {

    public static final String MOD_ID = "nexo_test";

    public NexoTestMod(Nexo nexo) {
        nexo.registerFeature(IBlock.class, new SimpleBlock(
            NexoTestMod.id("test_block"),
            Model.full(nexo, NexoTestMod.id("test_block.png"))
        ));
        nexo.registerFeature(IBlock.class, new SimpleBlock(
                id("test_block_2"),
                Model.full(nexo, Location.of("minecraft", "block/yellow_wool.png"))
        ));
        nexo.registerFeature(IBlock.class, new SimpleBlock(
            id("test_block_3"),
            Model.load(nexo, NexoTestMod.id("test_block.json"))
        ));
    }

    public static Location id(String path) {
        return Location.of(MOD_ID, path);
    }

    private record SimpleBlock(@NotNull Location location, @Nullable Model model) implements IBlock {
        @Override
        public @NotNull List<@NotNull Tag> tags() {
            return List.of();
        }
    }


}
