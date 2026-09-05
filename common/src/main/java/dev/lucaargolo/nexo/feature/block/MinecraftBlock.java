package dev.lucaargolo.nexo.feature.block;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Ticker;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.data.MinecraftPropertyData;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import dev.lucaargolo.nexo.unit.block.MinecraftBlockUnit;
import dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit;
import dev.lucaargolo.nexo.unit.world.MinecraftWorldUnit;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class MinecraftBlock extends BlockBase {

    private static final ConcurrentHashMap<Location, BlockBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, Holder<Block>> HOLDER_MAP = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Location, BlockEntityType.BlockEntitySupplier<?>> ENTITY_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, Holder<BlockEntityType<?>>> ENTITY_HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<BlockBase, Holder<Block>> CONVERT = new Bijection<>() {
        @Override
        public Holder<Block> forward(BlockBase feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public BlockBase backward(Holder<Block> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    public static Bijection<BlockBase, Holder<BlockEntityType<?>>> CONVERT_ENTITY = new Bijection<>() {
        @Override
        public Holder<BlockEntityType<?>> forward(BlockBase feature) {
            return ENTITY_HOLDER_MAP.get(feature.location());
        }

        @Override
        public BlockBase backward(Holder<BlockEntityType<?>> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    @NotNull
    private final NexoMinecraft<?, ?, ?, ?> nexo;
    @NotNull
    private final Holder<Block> holder;

    private final @NotNull List<@NotNull DataBase<?>> initialData;

    private MinecraftBlock(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Holder<Block> holder) {
        super(MinecraftRoleType.uncraft(nexo, Type.BLOCK, holder));
        this.identify(nexo, nexo.getRegistryHandler().identity(holder));
        this.nexo = nexo;
        this.holder = holder;
        Block block = holder.value();
        BlockState defaultState = block.defaultBlockState();
        List<@NotNull DataBase<?>> initialData = new ArrayList<>();
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            Location location = NexoMinecraft.id(holder).withPath(l -> l.path() + "/" + property.getName());
            MinecraftPropertyData<?> data = MinecraftPropertyData.of(property, defaultState::getValue);
            nexo.registerFeature(data, location);
            initialData.add(data);
        }
        this.initialData = List.copyOf(initialData);
    }

    @Override
    public @NotNull List<@NotNull DataBase<?>> initialData() {
        return this.initialData;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @Nullable Renderer<Graphics3D, BlockUnit> renderer() {
        // Minecraft-backed features are created from vanilla holders and carry no user-supplied renderer.
        return null;
    }

    @Override
    public <V extends Unit<?>> @NotNull Map<String, Function<BlockUnit, ? extends @Nullable Vault<V>>> vaults(@NotNull Class<V> type) {
        if (!MinecraftContainerVault.supports(type)) {
            return Map.of();
        }
        return Map.of(MinecraftContainerVault.KEY, unit -> unit.vault(type, MinecraftContainerVault.KEY));
    }

    @Override
    public @Nullable ItemBase item() {
        Item item = this.holder.value().asItem();
        return MinecraftFeatureType.ITEM.convert(nexo, item);
    }

    @Override
    public @NotNull Interaction onInteract(@NotNull BlockUnit block, @NotNull WorldUnit world, @NotNull EntityUnit entity, @NotNull Vector3i pos) {
        BlockState state = ((MinecraftBlockUnit<?>) block).get();
        Level level = ((MinecraftWorldUnit<?>) world).get();
        Player player = (Player) ((MinecraftEntityUnit<?, ?>) entity).get();
        Vec3 position = new Vec3(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(position, Direction.UP, BlockPos.containing(position), true);
        InteractionResult result = state.useWithoutItem(level, player, hitResult);
        return switch (result) {
            case SUCCESS, SUCCESS_NO_ITEM_USED, CONSUME, CONSUME_PARTIAL -> Interaction.SUCCESS;
            case PASS -> Interaction.PASS;
            case FAIL -> Interaction.FAIL;
        };
    }

    public static BlockBase lookup(Location location) {
        return FEATURE_MAP.get(location);
    }

    public static BlockBase register(NexoMinecraft<?, ?, ?, ?> nexo, BlockBase block) {
        BlockBase registered = FEATURE_MAP.get(block.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(block.location());
        FEATURE_MAP.put(block.location(), block);
        Holder<Block> blockHolder = nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.BLOCK, id, MinecraftFeatureType.BLOCK.craft(nexo, block));
        nexo.getRegistryHandler().registerVaults(MinecraftFeatureType.BLOCK, block, blockHolder::value);
        if (MinecraftBlock.isDynamicBlock(block)) {
            Holder<BlockEntityType<?>> holder = nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, () -> {
                BlockEntityType.BlockEntitySupplier<?> supplier = ENTITY_MAP.get(block.location());
                return BlockEntityType.Builder.of(supplier, HOLDER_MAP.get(block.location()).value()).build(null);
            });
            ENTITY_HOLDER_MAP.put(block.location(), holder);
        }
        return block;
    }

    public static BlockBase index(NexoMinecraft<?, ?, ?, ?> nexo, Holder<Block> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, l -> new MinecraftBlock(nexo, holder));
    }

    public static <M extends Block> Block craft(NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Utils.Extender<M> extender, @Nullable Function<BlockBehaviour.Properties, M> factory, BlockBase block) {
        @Nullable List<MinecraftDataProperty<?>> dataProperties = new ArrayList<>();
        for (DataBase<?> data : block.initialData()) {
            if (data instanceof DataBase.Constrained<?> constrained) {
                dataProperties.add(new MinecraftDataProperty<>(constrained));
            }
        }
        extender.initialize(feature -> {
            BlockState state = feature.getStateDefinition().any();
            for (MinecraftDataProperty<?> property : dataProperties) {
                state = property.setDefault(state);
            }
            feature.registerDefaultState(state);
            return null;
        });
        extender.override("createBlockStateDefinition", void.class, StateDefinition.Builder.class, (feature, superCall, builder) -> {
            superCall.apply(feature, builder);
            for (MinecraftDataProperty<?> property : dataProperties) {
                builder.add(property);
            }
            return null;
        });
        extender.override("useWithoutItem", InteractionResult.class, BlockState.class, Level.class, BlockPos.class, Player.class, BlockHitResult.class, (feature, superCall, state, level, pos, player, hitResult) -> {
            InteractionResult result = superCall.apply(feature, state, level, pos, player, hitResult);
            BlockUnit unit = nexo.blockToUnit(level, pos, state);
            WorldUnit world = nexo.levelToUnit(level);
            Interaction interaction = block.onInteract(unit, world, nexo.entityToUnit(player), new Vector3i(pos.getX(), pos.getY(), pos.getZ()));
            return switch (interaction) {
                case PASS -> result;
                case FAIL -> InteractionResult.FAIL;
                case SUCCESS -> InteractionResult.SUCCESS;
            };
        });
        extender.override("onRemove", void.class, BlockState.class, Level.class, BlockPos.class, BlockState.class, boolean.class, (feature, superCall, state, level, pos, newState, isMoving) -> {
            if (state.getBlock() != newState.getBlock() && !level.isClientSide) {
                block.onBreak(nexo.blockToUnit(level, pos, state, level.getBlockEntity(pos)));
            }
            superCall.apply(feature, state, level, pos, newState, isMoving);
            return null;
        });
        if (MinecraftBlock.isDynamicBlock(block)) {
            extender.implement(EntityBlock.class, feature -> new EntityBlock() {
                @Nullable
                private BlockEntityType<?> blockEntityType;

                {
                    ENTITY_MAP.put(block.location(), this::newBlockEntity);
                }

                @Override
                public @NotNull BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
                    return new Entity(pos, state);
                }

                @Override
                public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
                    Ticker<BlockUnit> ticker = block.ticker();
                    if (ticker == null || type != blockEntityType()) {
                        return null;
                    }
                    return (tickerLevel, pos, tickerState, entity) -> ticker.tick(nexo.blockToUnit(level, pos, state, entity));
                }

                private @NotNull BlockEntityType<?> blockEntityType() {
                    if (this.blockEntityType == null) {
                        this.blockEntityType = ENTITY_HOLDER_MAP.get(block.location()).value();
                    }
                    return this.blockEntityType;
                }

                class Entity extends BlockEntity {
                    public Entity(BlockPos pPos, BlockState pBlockState) {
                        super(blockEntityType(), pPos, pBlockState);
                    }
                }
            });
        }
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();
        if (factory != null) {
            return factory.apply(properties);
        }
        return extender.instantiate(properties);
    }

    private static boolean isDynamicBlock(@NotNull BlockBase block) {
        Renderer<Graphics3D, BlockUnit> renderer = block.renderer();
        return block.ticker() != null
                || (renderer != null && !(renderer instanceof StaticRenderer<?, ?>))
                || block.initialData().stream().anyMatch(data -> !(data instanceof DataBase.Constrained<?>))
                || !block.vaults(Nexo.type(ItemUnit.class)).isEmpty();
    }

}
