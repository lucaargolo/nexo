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
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.BlockItem;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class BlockTest {

    private static final int CHEST_CAPACITY = 27;

    private static final Renderer<Graphics3D, BlockUnit<?>> DYNAMIC_RENDERER = new Renderer<>() {

        @Override
        public void render(@NotNull BlockUnit<?> unit, @NotNull Graphics3D graphics) {
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

    private static final class ChestVault<V extends Unit<?, ?>> extends AbstractList<V> implements Vault<V> {

        private final Class<V> type;
        private final BlockUnit<?> block;
        private final DataBase<List<ItemUnit<?>>> data;
        private final List<@Nullable ItemUnit<?>> items;
        private final @NotNull V defaultValue;

        private ChestVault(@NotNull Class<V> type, @NotNull BlockUnit<?> block, @NotNull DataBase<List<ItemUnit<?>>> data) {
            this.type = type;
            this.block = block;
            this.data = data;
            List<ItemUnit<?>> stored = block.getData(data);
            this.items = new ArrayList<>(Collections.nCopies(CHEST_CAPACITY, null));
            if (stored != null) {
                if (stored.size() > CHEST_CAPACITY) {
                    throw new IllegalArgumentException("Stored chest contents exceed fixed size");
                }
                for (int index = 0; index < stored.size(); index++) {
                    this.items.set(index, stored.get(index));
                }
            }
            ItemBase item = new ItemBase(Location.of("nexo", "empty_vault_slot")) {
                @Override
                public @Nullable Renderer<Graphics3D, ItemUnit<?>> renderer() {
                    return null;
                }
            };
            ItemUnit<Role> fallback = new ItemUnit<>(block.nexo(), item, null) {
                @Override
                public @NotNull List<@NotNull DataBase<?>> data() {
                    return List.of();
                }

                @Override
                public <D> @Nullable D getData(@NotNull DataBase<D> data) {
                    return null;
                }

                @Override
                public <D> @NotNull ItemUnit<Role> setData(@NotNull DataBase<D> data, @Nullable D value) {
                    return this;
                }
            };
            this.defaultValue = type.cast(fallback);
        }

        @Override
        public @NotNull V defaultValue() {
            return this.defaultValue;
        }

        @Override
        public @NotNull V get(int index) {
            Objects.checkIndex(index, this.items.size());
            ItemUnit<?> item = this.items.get(index);
            return item == null ? this.defaultValue : this.type.cast(item);
        }

        @Override
        public @NotNull V set(int index, @NotNull V unit) {
            Objects.checkIndex(index, this.items.size());
            if (!(unit instanceof ItemUnit<?> item)) {
                throw new IllegalArgumentException("ChestVault only accepts ItemUnit instances");
            }
            ItemUnit<?> previous = this.items.set(index, item == this.defaultValue ? null : item);
            this.contentsChanged();
            return previous == null ? this.defaultValue : this.type.cast(previous);
        }

        @Override
        public boolean isFull() {
            return this.items.stream().allMatch(Objects::nonNull);
        }

        @Override
        public boolean add(@NotNull V unit) {
            if (this.isFull()) {
                return false;
            }
            if (!(unit instanceof ItemUnit<?> item)) {
                throw new IllegalArgumentException("ChestVault only accepts ItemUnit instances");
            }
            if (item == this.defaultValue) {
                return false;
            }
            for (int index = 0; index < this.items.size(); index++) {
                if (this.items.get(index) == null) {
                    this.items.set(index, item);
                    this.contentsChanged();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setContents(@NotNull Collection<? extends V> contents) {
            if (contents.size() > this.items.size()) {
                throw new IllegalArgumentException("Vault contents exceed fixed size");
            }
            this.items.clear();
            this.items.addAll(Collections.nCopies(CHEST_CAPACITY, null));
            int index = 0;
            for (V unit : contents) {
                if (!(unit instanceof ItemUnit<?> item)) {
                    throw new IllegalArgumentException("ChestVault only accepts ItemUnit instances");
                }
                this.items.set(index++, item == this.defaultValue ? null : item);
            }
            this.contentsChanged();
        }

        @Override
        public @NotNull V remove(int index) {
            Objects.checkIndex(index, this.items.size());
            ItemUnit<?> previous = this.items.set(index, null);
            if (previous != null) {
                this.contentsChanged();
            }
            return previous == null ? this.defaultValue : this.type.cast(previous);
        }

        @Override
        public int size() {
            return CHEST_CAPACITY;
        }

        @Override
        public void contentsChanged() {
            List<ItemUnit<?>> contents = new ArrayList<>();
            for (ItemUnit<?> item : this.items) {
                if (item != null) {
                    contents.add(item);
                }
            }
            this.block.setData(this.data, List.copyOf(contents));
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
                if (world.side().isClient() && !entity.openScreen(new ScreenTest())) {
                    throw new IllegalStateException("Failed to create screen unit");
                }
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
