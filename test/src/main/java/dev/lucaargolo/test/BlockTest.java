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
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.awt.Color;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class BlockTest {

    private static final int CHEST_CAPACITY = 27;

    private static final Renderer<Graphics3D, BlockUnit<?>> DYNAMIC_RENDERER = new Renderer<>() {

        @Override
        public void render(@NotNull Graphics3D graphics, @NotNull BlockUnit<?> unit) {
            float value = (System.currentTimeMillis() % 10000) / 10000.0f;
            int color = Color.HSBtoRGB(value, 1.0F, 1.0F);
            graphics.pushState();
            graphics.color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
            graphics.drawCube(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
            graphics.popState();
        }

        @Override
        public @NotNull Map<String, Material<?>> materials() {
            return Map.of();
        }

        @Override
        public @NotNull Transform transform(@NotNull Location location) {
            return Map.of(
                    Location.of("minecraft", "gui"), new Transform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f)),
                    Location.of("minecraft", "ground"), new Transform(new Vector3f(0, 0, 0), new Vector3f(0, 3, 0), new Vector3f(0.25f, 0.25f, 0.25f)),
                    Location.of("minecraft", "fixed"), new Transform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f)),
                    Location.of("minecraft", "thirdperson_righthand"), new Transform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
                    Location.of("minecraft", "firstperson_righthand"), new Transform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)),
                    Location.of("minecraft", "firstperson_lefthand"), new Transform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f))
            ).getOrDefault(location, new Transform(new Vector3f(), new Vector3f(), new Vector3f(1.0F)));
        }

    };

    private static final BooleanData STATE = new BooleanData(NexoTestMod.id("test_state"), false);

    private BlockTest() {
    }

    public static void register(@NotNull Nexo nexo, @NotNull ItemCategoryBase category) {
        registerModelBlock(nexo, category, NexoTestMod.id("test_block"), ModelResource.full(NexoTestMod.id("test_block")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_block_2"), ModelResource.full(Location.of("minecraft", "block/yellow_wool")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_block_3"), requireNonNull(nexo.getResource(Resource.Type.MODEL, NexoTestMod.id("test_block.json")), "Missing test_block.json model"));
        registerModelBlock(nexo, category, NexoTestMod.id("test_block_cutout"), ModelResource.full(Location.of("minecraft", "block/oak_leaves")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_block_translucent"), ModelResource.full(NexoTestMod.id("test_block_translucent")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_block_translucent_2"), ModelResource.full(Location.of("minecraft", "block/orange_stained_glass")));
        registerModelBlock(nexo, category, NexoTestMod.id("test_gltf"), requireNonNull(nexo.getResource(Resource.Type.MODEL, NexoTestMod.id("test_model.gltf")), "Missing test_model.gltf model"));
        registerModelBlock(nexo, category, NexoTestMod.id("test_obj"), requireNonNull(nexo.getResource(Resource.Type.MODEL, NexoTestMod.id("test_model.obj")), "Missing test_model.obj model"));

        registerChestBlock(nexo, category);
        registerStateBlock(nexo, category);
        registerDynamicBlock(nexo, category);
    }

    private static void registerChestBlock(@NotNull Nexo nexo, @NotNull ItemCategoryBase category) {
        ItemData chestData = nexo.registerFeature(new ItemData(NexoTestMod.id("test_chest_data"), nexo));
        DataBase<List<ItemUnit<?>>> chestInventory = nexo.registerFeature(DataBase.list(chestData));
        BlockBase chest = nexo.registerFeature(new SimpleBlock(NexoTestMod.id("test_chest"), ModelResource.full(NexoTestMod.id("test_block"))) {
            @Override
            public void onBreak(@NotNull BlockUnit<?> block) {
                List<ItemUnit<?>> items = block.getData(chestInventory);
                if (items != null) {
                    for (ItemUnit<?> item : items) {
                        block.drop(item);
                    }
                }
            }

            @Override
            public @NotNull List<@NotNull DataBase<?>> initialData() {
                return List.of(chestInventory);
            }

            @Override
            public <V extends Unit<?, ?>> @NotNull Map<String, Function<BlockUnit<?>, ? extends Vault<V>>> vaults(@NotNull Class<V> type) {
                return Map.of("inventory", unit -> new ChestVault<>(type, unit, chestInventory));
            }
        });
        nexo.registerFeature(new BlockItem(chest, category));
    }

    private static final class ChestVault<V extends Unit<?, ?>> extends AbstractCollection<V> implements Vault<V> {

        private final Class<V> type;
        private final BlockUnit<?> block;
        private final DataBase<List<ItemUnit<?>>> data;
        private final List<ItemUnit<?>> items;

        private ChestVault(@NotNull Class<V> type, @NotNull BlockUnit<?> block, @NotNull DataBase<List<ItemUnit<?>>> data) {
            this.type = type;
            this.block = block;
            this.data = data;
            List<ItemUnit<?>> stored = block.getData(data);
            this.items = new ArrayList<>(stored != null ? stored : List.of());
        }

        @Override
        public boolean isFull() {
            return this.items.size() >= CHEST_CAPACITY;
        }

        @Override
        public boolean add(@NotNull V unit) {
            if (this.isFull()) {
                return false;
            }
            if (!(unit instanceof ItemUnit<?> item)) {
                throw new IllegalArgumentException("ChestVault only accepts ItemUnit instances");
            }
            boolean added = this.items.add(item);
            if (added) {
                this.contentsChanged();
            }
            return added;
        }

        @Override
        public void setContents(@NotNull Collection<? extends V> contents) {
            this.items.clear();
            for (V unit : contents) {
                if (!(unit instanceof ItemUnit<?> item)) {
                    throw new IllegalArgumentException("ChestVault only accepts ItemUnit instances");
                }
                this.items.add(item);
            }
            this.contentsChanged();
        }

        @Override
        public @NotNull Iterator<V> iterator() {
            Iterator<ItemUnit<?>> iterator = this.items.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public @NotNull V next() {
                    return type.cast(iterator.next());
                }

                @Override
                public void remove() {
                    iterator.remove();
                    ChestVault.this.contentsChanged();
                }
            };
        }

        @Override
        public int size() {
            return this.items.size();
        }

        @Override
        public void contentsChanged() {
            this.block.setData(this.data, List.copyOf(this.items));
        }
    }

    private static void registerModelBlock(@NotNull Nexo nexo, @NotNull ItemCategoryBase category, @NotNull Location location, @NotNull ModelResource model) {
        BlockBase block = nexo.registerFeature(new SimpleBlock(location, model));
        nexo.registerFeature(new BlockItem(block, category));
    }

    private static void registerStateBlock(@NotNull Nexo nexo, @NotNull ItemCategoryBase category) {
        ModelResource model = requireNonNull(nexo.getResource(Resource.Type.MODEL, NexoTestMod.id("test_block.json")), "Missing test_block.json model");
        BlockBase block = nexo.registerFeature(new SimpleBlock(NexoTestMod.id("test_state"), model) {
            @Override
            public @NotNull List<@NotNull DataBase<?>> initialData() {
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
            public Ticker<BlockUnit<?>> ticker() {
                return unit -> { };
            }

            @Override
            public @NotNull Interaction onInteract(@NotNull BlockUnit<?> block, @NotNull WorldUnit<?> world, @NotNull EntityUnit<PlayerRole> entity, @NotNull Vector3i pos) {
                block.withData(dynamicData, value -> value + "!");
                entity.openScreen(new ScreenTest());
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
