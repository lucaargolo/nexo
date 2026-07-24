package dev.lucaargolo.nexo.feature.block;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.Ticker;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.unit.block.MinecraftBlockUnit;
import dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit;
import dev.lucaargolo.nexo.unit.world.MinecraftWorldUnit;
import dev.lucaargolo.nexo.util.Bijection;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
    private final NexoRegistryHandler<?> helper;
    @NotNull
    private final Holder<Block> holder;

    private MinecraftBlock(@NotNull NexoRegistryHandler<?> helper, @NotNull Holder<Block> holder) {
        super(NexoMinecraft.id(holder), MinecraftRoleType.uncraft(helper, Type.BLOCK, holder));
        this.helper = helper;
        this.holder = holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @Nullable Renderer<Graphics3D, BlockUnit<?>> renderer() {
        //TODO: This
        return null;
    }

    @Override
    public @Nullable ItemBase item() {
        Item item = this.holder.value().asItem();
        return MinecraftFeatureType.ITEM.convert(helper, item);
    }

    @Override
    public @NotNull Interaction onInteract(@NotNull BlockUnit<?> block, @NotNull WorldUnit<?> world, @NotNull EntityUnit<PlayerRole> entity, @NotNull Vector3i pos) {
        BlockState state = ((MinecraftBlockUnit<?, ?>) block).get();
        Level level = ((MinecraftWorldUnit<?>) world).get();
        Player player = (Player) ((MinecraftEntityUnit<?, PlayerRole, ?>) entity).get();
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

    public static BlockBase register(NexoRegistryHandler<?> helper, BlockBase block) {
        BlockBase registered = FEATURE_MAP.get(block.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(block.location());
        FEATURE_MAP.put(block.location(), block);
        helper.registerBuiltinFeature(BuiltInRegistries.BLOCK, id, MinecraftFeatureType.BLOCK.craft(helper, block));
        if(isDynamicBlock(block)) {
            Holder<BlockEntityType<?>> holder = helper.registerBuiltinFeature(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, () -> {
                BlockEntityType.BlockEntitySupplier<?> supplier = ENTITY_MAP.get(block.location());
                return BlockEntityType.Builder.of(supplier, HOLDER_MAP.get(block.location()).value()).build(null);
            });
            ENTITY_HOLDER_MAP.put(block.location(), holder);
        }
        return block;
    }

    public static BlockBase index(NexoRegistryHandler<?> helper, Holder<Block> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, l -> new MinecraftBlock(helper, holder));
    }

    public static Block craft(NexoRegistryHandler<?> helper, BlockBase block) {
        boolean blockEntity = isDynamicBlock(block);
        class CraftedBlock extends Block {

            private @Nullable List<DataProperty<?>> properties;

            private CraftedBlock() {
                super(BlockBehaviour.Properties.of());
                BlockState state = this.stateDefinition.any();
                for (DataProperty<?> property : this.dataProperties()) {
                    state = property.setDefault(state);
                }
                this.registerDefaultState(state);
            }

            @Override
            protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
                for (DataProperty<?> property : this.dataProperties()) {
                    builder.add(property);
                }
            }

            @Override
            protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
                BlockUnit<?> unit = helper.nexo().blockToUnit(level, pos, state);
                WorldUnit<?> world = helper.nexo().levelToUnit(level);
                Interaction interaction = block.onInteract(unit, world, helper.nexo().entityToUnit(player).withRole(PlayerRole.class), new Vector3i(pos.getX(), pos.getY(), pos.getZ()));
                return switch (interaction) {
                    case PASS -> InteractionResult.PASS;
                    case FAIL -> InteractionResult.FAIL;
                    case SUCCESS -> InteractionResult.SUCCESS;
                };
            }

            private @NotNull List<DataProperty<?>> dataProperties() {
                if (this.properties == null) {
                    this.properties = new ArrayList<>();
                    for (DataBase<?> data : block.data()) {
                        if (data instanceof DataBase.Constrained<?> constrained) {
                            this.properties.add(new DataProperty<>(constrained));
                        }
                    }
                }
                return this.properties;
            }
        }

        class DynamicCraftedBlock extends CraftedBlock implements EntityBlock {

            private DynamicCraftedBlock() {
                super();
                ENTITY_MAP.put(block.location(), this::newBlockEntity);
            }

            @Nullable
            private BlockEntityType<?> blockEntityType;

            @Override
            public @NotNull BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
                return new Entity(pos, state);
            }

            @Override
            public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
                Ticker<BlockUnit<?>> ticker = block.ticker();
                if (ticker == null || type != blockEntityType()) {
                    return null;
                }
                return (tickerLevel, pos, tickerState, entity) -> ticker.tick(helper.nexo().blockToUnit(level, pos, state, entity));
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

        }

        return blockEntity ? new DynamicCraftedBlock() : new CraftedBlock();
    }


    public static boolean isDynamicBlock(@NotNull BlockBase block) {
        Renderer<Graphics3D, BlockUnit<?>> renderer = block.renderer();
        return block.ticker() != null
                || (renderer != null && !(renderer instanceof StaticRenderer<?, ?>))
                || block.data().stream().anyMatch(data -> !(data instanceof DataBase.Constrained<?>));
    }

}
