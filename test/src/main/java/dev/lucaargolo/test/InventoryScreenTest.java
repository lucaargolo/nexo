package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.role.screen.InventoryRole;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import java.util.Map;

public final class InventoryScreenTest extends ScreenBase {

    public InventoryScreenTest(@NotNull BlockUnit<?> chest, @NotNull EntityUnit<PlayerRole> player) {
        super(NexoTestMod.id("test_chest_inventory"), () -> new InventoryRole(Map.of(
                chest, Map.of("inventory", new InventoryRole.Config(
                        18,
                        18,
                        InventoryRole.Config.SlotDistribution.grid(9).margin(8, 18)
                )),
                player, Map.of("inventory", new InventoryRole.Config(
                        18,
                        18,
                        ((InventoryRole.Config.SlotDistribution) (index, slotCount, slotWidth, slotHeight) -> {
                            int column = index % 9;
                            if (index < 9) {
                                return new Vector2i(column * slotWidth, 58);
                            }
                            int row = (index - 9) / 9;
                            return new Vector2i(column * slotWidth, row * slotHeight);
                        }).margin(8, 84)
                ))
        )));
    }

    @Override
    public @NotNull Map<String, Material<?>> materials() {
        return Map.of();
    }

}
