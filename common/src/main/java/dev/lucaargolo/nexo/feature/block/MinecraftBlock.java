package dev.lucaargolo.nexo.feature.block;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
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
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftBlock extends BlockBase {

    private static final ConcurrentHashMap<Location, BlockBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, NexoHolder<Block>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<BlockBase, NexoHolder<Block>> CONVERT = new Bijection<>() {
        @Override
        public NexoHolder<Block> forward(BlockBase feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public BlockBase backward(NexoHolder<Block> holder) {
            return FEATURE_MAP.get(holder.location());
        }
    };

    @NotNull
    private final NexoHolder<Block> holder;

    private MinecraftBlock(NexoRegistryHandler<?> helper, @NotNull NexoHolder<Block> holder) {
        super(holder.location(), MinecraftRoleType.uncraft(helper, Type.BLOCK, holder));
        this.holder = holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @Nullable StaticRenderer<Graphics3D, BlockUnit<?>> renderer() {
        //TODO: This
        return null;
    }

    @Override
    public @Nullable ItemBase item() {
        Item item = this.holder.get().asItem();
        if (item != Items.AIR) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            return this.holder.nexo().getFeature(Type.ITEM, NexoMinecraft.id(itemId));
        } else {
            return null;
        }
    }

    @Override
    public @NotNull Interaction onInteract(@NotNull BlockUnit<?> block, @NotNull WorldUnit<?> world, @NotNull EntityUnit<PlayerRole> entity, @NotNull Vector3i pos) {
        BlockState state = ((MinecraftBlockUnit) block).get();
        Level level = ((MinecraftWorldUnit) world).get();
        Player player = (Player) ((MinecraftEntityUnit<PlayerRole, ?>) entity).get();
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
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(block.location().namespace(), block.location().path());
        NexoHolder<Block> holder = helper.registerBuiltinFeature(BuiltInRegistries.BLOCK, id, MinecraftFeatureType.BLOCK.craft(helper, block));
        FEATURE_MAP.put(block.location(), block);
        HOLDER_MAP.put(block.location(), holder);
        return block;
    }

    @SuppressWarnings("deprecation")
    public static NexoHolder<Block> index(NexoRegistryHandler<?> helper, Block block) {
        Holder<Block> h = block.builtInRegistryHolder();
        Location location = NexoMinecraft.id(h);
        NexoHolder<Block> indexed = HOLDER_MAP.get(location);
        if (indexed != null) {
            return indexed;
        }
        NexoHolder<Block> holder = new NexoHolder<>(helper.nexo(), h, Block.class);
        FEATURE_MAP.putIfAbsent(location, new MinecraftBlock(helper, holder));
        HOLDER_MAP.put(location, holder);
        return holder;
    }

    public static Block craft(NexoRegistryHandler<?> helper, BlockBase block) {
        return new Block(BlockBehaviour.Properties.of()) {
            @Override
            protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull net.minecraft.world.entity.player.Player pPlayer, @NotNull BlockHitResult pHitResult) {
                BlockUnit<?> state = helper.nexo().stateToUnit(pState);
                WorldUnit<?> level = helper.nexo().levelToUnit(pLevel);
                Interaction interaction = block.onInteract(state, level, helper.nexo().entityToUnit(pPlayer).with(PlayerRole.class), new Vector3i(pPos.getX(), pPos.getY(), pPos.getZ()));
                return switch (interaction) {
                    case PASS -> InteractionResult.PASS;
                    case FAIL -> InteractionResult.FAIL;
                    case SUCCESS -> InteractionResult.SUCCESS;
                };
            }
        };
    }


}
