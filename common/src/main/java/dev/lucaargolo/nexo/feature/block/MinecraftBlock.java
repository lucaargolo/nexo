package dev.lucaargolo.nexo.feature.block;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeature;
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftBlock extends BlockBase implements MinecraftFeature<Block> {

    private static final ConcurrentHashMap<Location, BlockBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, NexoHolder<Block, Block>> HOLDER_MAP = new ConcurrentHashMap<>();

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final NexoHolder<Block, Block> holder;

    private MinecraftBlock(@NotNull NexoMinecraft nexo, @NotNull NexoHolder<Block, Block> holder) {
        super(holder.location());
        this.nexo = nexo;
        this.holder = holder;
    }

    private MinecraftBlock(@NotNull NexoMinecraft nexo, @NotNull Holder<Block> holder) {
        this(nexo, new NexoHolder<>(nexo, holder, Block.class));
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
    }

    @Override
    public @NotNull NexoHolder<Block, Block> holder() {
        return this.holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @Nullable Model model() {
        //TODO: This
        return null;
    }

    @Override
    public @Nullable ItemBase item() {
        Item item = this.holder().get().asItem();
        if(item != Items.AIR) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            return this.nexo.getFeature(Type.ITEM, NexoMinecraft.id(itemId));
        }else{
            return null;
        }
    }

    public static BlockBase lookup(NexoRegistryHandler<?> helper, Location location) {
        return FEATURE_MAP.computeIfAbsent(location, l -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
            MinecraftBlock block = BuiltInRegistries.BLOCK.getHolder(id).map(h -> new MinecraftBlock(helper.nexo(), h)).orElse(null);
            if(block != null) HOLDER_MAP.put(location, block.holder);
            return block;
        });
    }

    public static BlockBase register(NexoRegistryHandler<?> helper, ResourceLocation id, BlockBase block) {
        NexoHolder<Block, Block> holder = helper.registerBuiltinFeature(BuiltInRegistries.BLOCK, id, () -> new Block(BlockBehaviour.Properties.of()) {
            @Override
            protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull Player pPlayer, @NotNull BlockHitResult pHitResult) {
                WorldUnit world = helper.nexo().world(pLevel);
                Interaction interaction = block.onInteract(world, new Vector3i(pPos.getX(), pPos.getY(), pPos.getZ()));
                return switch (interaction) {
                    case PASS -> InteractionResult.PASS;
                    case FAIL -> InteractionResult.FAIL;
                    case SUCCESS -> InteractionResult.SUCCESS;
                };
            }
        });
        FEATURE_MAP.put(block.location(), block);
        HOLDER_MAP.put(block.location(), holder);
        return block;
    }

    public static Block craft(BlockBase block) {
        return Objects.requireNonNull(HOLDER_MAP.get(block.location()).get());
    }

}
