package dev.lucaargolo.test;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Ticker;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.block.SimpleBlock;
import dev.lucaargolo.nexo.api.feature.data.BooleanData;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.data.ItemData;
import dev.lucaargolo.nexo.api.feature.data.StringData;
import dev.lucaargolo.nexo.api.feature.item.BlockItem;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.test.feature.TestInventoryScreen;
import dev.lucaargolo.test.feature.TestScreen;
import dev.lucaargolo.test.render.TestDynamicBlockRenderer;
import dev.lucaargolo.test.util.TestChestVault;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class BlockTest {


    private static final Renderer<Graphics3D, BlockUnit> DYNAMIC_RENDERER = new TestDynamicBlockRenderer();
    private static final BooleanData STATE = new BooleanData(false);

    private BlockTest() {
    }

    public static void register(@NotNull Nexo nexo, @NotNull ItemCategoryBase category, @NotNull TestScreen testScreen, @NotNull TestInventoryScreen testInventoryScreen) {
        registerModelBlock(nexo, category, NexoTestMod.id("test_block"), ModelResource.full(NexoTestMod.id("test_block")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_block_2"), ModelResource.full(Location.of("minecraft", "block/yellow_wool")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_block_3"), requireNonNull(nexo.getResource(Resource.Type.MODEL, NexoTestMod.id("test_block.json")), "Missing test_block.json model"));
        registerModelBlock(nexo, category, NexoTestMod.id("test_block_cutout"), ModelResource.full(Location.of("minecraft", "block/oak_leaves")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_block_translucent"), ModelResource.full(NexoTestMod.id("test_block_translucent")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_block_translucent_2"), ModelResource.full(Location.of("minecraft", "block/orange_stained_glass")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_gltf"), requireNonNull(nexo.getResource(Resource.Type.MODEL, NexoTestMod.id("test_model.gltf")), "Missing test_model.gltf model"));
        registerModelBlock(nexo, category, NexoTestMod.id("test_obj"), requireNonNull(nexo.getResource(Resource.Type.MODEL, NexoTestMod.id("test_model.obj")), "Missing test_model.obj model"));

        registerStateBlock(nexo, category);
        registerDynamicBlock(nexo, category, testScreen);
        registerChestBlock(nexo, category, testInventoryScreen);

    }

    private static void registerChestBlock(@NotNull Nexo nexo, @NotNull ItemCategoryBase category, @NotNull TestInventoryScreen screen) {
        ItemData chestData = nexo.registerFeature(new ItemData(nexo), NexoTestMod.id("test_chest_data"));
        DataBase<List<ItemUnit>> chestInventory = nexo.registerFeature(DataBase.list(chestData), NexoTestMod.id("test_chest_data_list"));
        BlockBase chest = nexo.registerFeature(new SimpleBlock(nexo.getResource(Resource.Type.MODEL, Location.of("minecraft", "block/barrel"))) {
            @Override
            public void onBreak(@NotNull BlockUnit block) {
                List<ItemUnit> items = block.getData(chestInventory);
                if (items != null) {
                    for (ItemUnit item : items) {
                        block.drop(item);
                    }
                }
            }

            @Override
            public @NotNull List<@NotNull DataBase<?>> initialData() {
                return List.of(chestInventory);
            }

            @Override
            public @NotNull Interaction onInteract(@NotNull BlockUnit block, @NotNull WorldUnit world, @NotNull EntityUnit entity, @NotNull Vector3i pos) {
                requireNonNull(nexo.unit(screen)).open(entity);
                return Interaction.SUCCESS;
            }

            @Override
            public <V extends Unit<?>> @NotNull Map<String, Function<BlockUnit, ? extends Vault<V>>> vaults(@NotNull Class<V> type) {
                if(type == ItemUnit.class) {
                    ItemBase item = requireNonNull(nexo.getFeature(Type.ITEM, Location.of("minecraft", "air")));
                    ItemUnit empty = requireNonNull(nexo.unit(item));
                    V initial = type.cast(empty);
                    return Map.of("inventory", unit -> new TestChestVault<>(type, initial, unit, chestInventory));
                }else{
                    return Map.of();
                }
            }
        }, NexoTestMod.id("test_chest"));
        nexo.registerFeature(new BlockItem(chest, category), chest.location());
    }



    private static void registerModelBlock(@NotNull Nexo nexo, @NotNull ItemCategoryBase category, @NotNull Location location, @NotNull ModelResource model) {
        BlockBase block = nexo.registerFeature(new SimpleBlock(model), location);
        nexo.registerFeature(new BlockItem(block, category), block.location());
    }

    private static void registerStateBlock(@NotNull Nexo nexo, @NotNull ItemCategoryBase category) {
        ModelResource model = requireNonNull(nexo.getResource(Resource.Type.MODEL, NexoTestMod.id("test_block.json")), "Missing test_block.json model");
        nexo.registerFeature(STATE, NexoTestMod.id("test_state"));
        BlockBase block = nexo.registerFeature(new SimpleBlock(model) {
            @Override
            public @NotNull List<@NotNull DataBase<?>> initialData() {
                return List.of(STATE);
            }

            @Override
            public @NotNull Interaction onInteract(@NotNull BlockUnit block, @NotNull WorldUnit world, @NotNull EntityUnit entity, @NotNull Vector3i pos) {
                world.setBlock(pos, block.withData(STATE, toggled -> !toggled));
                return Interaction.SUCCESS;
            }
        }, NexoTestMod.id("test_state"));
        nexo.registerFeature(new BlockItem(block, category), block.location());
    }

    private static void registerDynamicBlock(@NotNull Nexo nexo, @NotNull ItemCategoryBase category, @NotNull TestScreen screen) {
        StringData dynamicData = new StringData("initial");
        nexo.registerFeature(dynamicData, NexoTestMod.id("dynamic_block_data"));
        BlockBase block = nexo.registerFeature(new BlockBase() {
            @Override
            public Renderer<Graphics3D, BlockUnit> renderer() {
                return DYNAMIC_RENDERER;
            }

            @Override
            public BlockItem item() {
                return null;
            }

            @Override
            public @NotNull List<@NotNull DataBase<?>> initialData() {
                return List.of(dynamicData);
            }

            @Override
            public Ticker<BlockUnit> ticker() {
                return unit -> { };
            }

            @Override
            public @NotNull Interaction onInteract(@NotNull BlockUnit block, @NotNull WorldUnit world, @NotNull EntityUnit entity, @NotNull Vector3i pos) {
                requireNonNull(nexo.unit(screen)).open(entity);
                block.withData(dynamicData, value -> value + "!");
                return Interaction.SUCCESS;
            }
        }, NexoTestMod.id("dynamic_block"));
        nexo.registerFeature(new BlockItem(block, category) {
            @Override
            public Ticker<ItemUnit> ticker() {
                return unit -> { };
            }
        }, block.location());
    }


}
