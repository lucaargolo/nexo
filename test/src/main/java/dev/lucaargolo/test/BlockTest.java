package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Ticker;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.block.SimpleBlock;
import dev.lucaargolo.nexo.api.feature.data.BooleanData;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.data.StringData;
import dev.lucaargolo.nexo.api.feature.item.BlockItem;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;
import java.util.Map;

public final class BlockTest {

    private static final Renderer<Graphics3D, BlockUnit<?>> EMPTY_RENDERER = new Renderer<>() {
        @Override
        public void render(@NotNull Graphics3D graphics, @NotNull BlockUnit<?> unit) {
        }

        @Override
        public @NotNull Map<String, Material<?>> materials() {
            return Map.of();
        }

        @Override
        public @NotNull Transform transform(@NotNull Location location) {
            return new Transform(new Vector3f(), new Vector3f(), new Vector3f(1.0F));
        }
    };

    private static final BooleanData STATE = new BooleanData(NexoTestMod.id("test_state"), false);

    private BlockTest() {
    }

    public static void register(@NotNull Nexo nexo, @NotNull ItemCategoryBase category) {
        registerModelBlock(nexo, category, NexoTestMod.id("test_block"), ModelResource.full(NexoTestMod.id("test_block")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_block_2"), ModelResource.full(Location.of("minecraft", "block/yellow_wool")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_block_3"), nexo.getResource(Resource.Type.MODEL, NexoTestMod.id("test_block.json")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_gltf"), nexo.getResource(Resource.Type.MODEL, NexoTestMod.id("test_model.gltf")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_obj"), nexo.getResource(Resource.Type.MODEL, NexoTestMod.id("test_model.obj")));

        registerStateBlock(nexo, category);
        registerDynamicBlock(nexo, category);
    }

    private static void registerModelBlock(@NotNull Nexo nexo, @NotNull ItemCategoryBase category, @NotNull Location location, @NotNull ModelResource model) {
        BlockBase block = nexo.registerFeature(new SimpleBlock(location, model));
        nexo.registerFeature(new BlockItem(block, category));
    }

    private static void registerStateBlock(@NotNull Nexo nexo, @NotNull ItemCategoryBase category) {
        ModelResource model = nexo.getResource(Resource.Type.MODEL, NexoTestMod.id("test_block.json"));
        BlockBase block = nexo.registerFeature(new SimpleBlock(NexoTestMod.id("test_state"), model) {
            @Override
            public @NotNull List<@NotNull DataBase<?>> data() {
                return List.of(STATE);
            }

            @Override
            public @NotNull Interaction onInteract(@NotNull BlockUnit<?> block, @NotNull WorldUnit<?> world, @NotNull EntityUnit<PlayerRole> entity, @NotNull Vector3i pos) {
                world.setBlock(pos, block.withData(STATE, toggled -> !toggled));
                return Interaction.SUCCESS;
            }
        });
        nexo.registerFeature(new BlockItem(block, category));
    }

    private static void registerDynamicBlock(@NotNull Nexo nexo, @NotNull ItemCategoryBase category) {
        StringData dynamicData = new StringData(NexoTestMod.id("dynamic_block_data"), "initial");
        nexo.registerFeature(dynamicData);
        BlockBase block = nexo.registerFeature(new BlockBase(NexoTestMod.id("dynamic_block")) {
            @Override
            public Renderer<Graphics3D, BlockUnit<?>> renderer() {
                return EMPTY_RENDERER;
            }

            @Override
            public BlockItem item() {
                return null;
            }

            @Override
            public @NotNull List<@NotNull DataBase<?>> data() {
                return List.of(dynamicData);
            }

            @Override
            public Ticker<BlockUnit<?>> ticker() {
                return unit -> { };
            }

            @Override
            public @NotNull Interaction onInteract(@NotNull BlockUnit<?> block, @NotNull WorldUnit<?> world, @NotNull EntityUnit<PlayerRole> entity, @NotNull Vector3i pos) {
                block.withData(dynamicData, value -> value + "!");
                return Interaction.SUCCESS;
            }
        });
        nexo.registerFeature(new BlockItem(block, category) {
            @Override
            public Ticker<ItemUnit<?>> ticker() {
                return unit -> { };
            }
        });
    }
}
