package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Identifier;
import dev.lucaargolo.nexo.api.Mod;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Block;
import dev.lucaargolo.nexo.api.feature.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Mod("nexo_test")
public class NexoTestMod {

    public NexoTestMod(Nexo nexo) {
        Identifier id = Identifier.of("nexo_test", "test_block");
        nexo.add(id, new TestBlock(id));
    }

    private record TestBlock(@NotNull Identifier id) implements Block {
        @Override
        public @NotNull List<@NotNull Tag> tags() {
            return List.of();
        }
    }
}
