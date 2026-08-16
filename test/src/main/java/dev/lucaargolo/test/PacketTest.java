package dev.lucaargolo.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.block.SimpleBlock;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.BlockItem;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.packet.Packet;
import dev.lucaargolo.nexo.api.feature.packet.PacketReceiver;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

import java.nio.ByteBuffer;

public final class PacketTest {

    private PacketTest() {
    }

    public static void register(@NotNull Nexo nexo, @NotNull ItemCategoryBase category, @NotNull ScreenBase testScreen) {
        BlockPositionData blockPositionData = nexo.registerFeature(new BlockPositionData());

        BlockBase air = nexo.getFeature(Feature.Type.BLOCK, Location.of("minecraft", "air"));
        assert air != null;
        BlockUnit<?> airUnit = nexo.unit(air);
        assert airUnit != null;

        Class<EntityUnit<PlayerRole>> receiverType = Nexo.type(EntityUnit.class);
        Packet<Vector3i, EntityUnit<PlayerRole>> packet = nexo.registerFeature(new Packet<>(NexoTestMod.id("break_block"), blockPositionData, receiverType) {
            @Override
            public void handle(@NotNull EntityUnit<PlayerRole> receiver) {
                WorldUnit<?> world = receiver.world();
                BlockUnit<?> block = world.getBlock(value());
                if (block != null && block.feature().location().equals(NexoTestMod.id("packet_test_block"))) {
                    world.setBlock(value(), airUnit);
                }
            }
        });

        BlockBase packetTestBlock = nexo.registerFeature(new SimpleBlock(
                NexoTestMod.id("packet_test_block"),
                ModelResource.full(Location.of("minecraft", "block/bedrock"))
        ) {
            @Override
            public @NotNull Interaction onInteract(@NotNull BlockUnit<?> block, @NotNull WorldUnit<?> world, @NotNull EntityUnit<PlayerRole> entity, @NotNull Vector3i pos) {
                if (world.side().isClient()) {
                    ScreenUnit<?> unit = nexo.unit(testScreen);
                    if (unit != null) {
                        unit.open();
                    }
                    nexo.sendPacket(PacketReceiver.server(), packet);
                }
                return Interaction.SUCCESS;
            }
        });
        nexo.registerFeature(new BlockItem(packetTestBlock, category));
    }

    private static final class BlockPositionData extends DataBase<Vector3i> {

        @NotNull
        private final Vector3i initial;

        private BlockPositionData() {
            super(NexoTestMod.id("block_position"));
            this.initial = new Vector3i();
        }

        @Override
        public @NotNull Vector3i initial() {
            return initial;
        }

        @Override
        public @NotNull ByteBuffer write(@NotNull Vector3i value) {
            return ByteBuffer.allocate(3 * Integer.BYTES)
                    .putInt(value.x)
                    .putInt(value.y)
                    .putInt(value.z)
                    .flip();
        }

        @Override
        public @NotNull Vector3i read(@NotNull ByteBuffer buffer) {
            return new Vector3i(buffer.getInt(), buffer.getInt(), buffer.getInt());
        }

        @Override
        public @NotNull JsonElement serialize(@NotNull Vector3i value) {
            JsonArray array = new JsonArray(3);
            array.add(value.x);
            array.add(value.y);
            array.add(value.z);
            return array;
        }

        @Override
        public @NotNull Vector3i deserialize(@NotNull JsonElement element) {
            JsonArray array = element.getAsJsonArray();
            return new Vector3i(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
        }
    }
}
