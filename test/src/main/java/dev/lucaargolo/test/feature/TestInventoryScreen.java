package dev.lucaargolo.test.feature;

import dev.lucaargolo.nexo.api.feature.data.TextData;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Text;
import dev.lucaargolo.nexo.api.role.screen.InventoryRole;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.test.NexoTestMod;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import java.util.Map;

public final class TestInventoryScreen extends ScreenBase<Text> {

    public static Location PLAYER_INVENTORY_KEY = NexoTestMod.id("player_inventory");
    public static Location CHEST_INVENTORY_KEY = NexoTestMod.id("chest_inventory");

    public TestInventoryScreen() {
        super(() -> new InventoryRole(Map.of(
                CHEST_INVENTORY_KEY, new InventoryRole.Config(18, 18,
                        InventoryRole.Config.SlotDistribution.grid(9).margin(8, 18)
                ),
                PLAYER_INVENTORY_KEY, new InventoryRole.Config(18, 18,
                        ((InventoryRole.Config.SlotDistribution) (index, slotCount, slotWidth, slotHeight) -> {
                            int column = index % 9;
                            if (index < 9) {
                                return new Vector2i(column * slotWidth, 58);
                            }
                            int row = (index - 9) / 9;
                            return new Vector2i(column * slotWidth, row * slotHeight);
                        }).margin(8, 84)
                )
        )), TextData.TEXT);
    }

    @Override
    public @NotNull Map<String, Material<?>> materials() {
        return Map.of();
    }

}
