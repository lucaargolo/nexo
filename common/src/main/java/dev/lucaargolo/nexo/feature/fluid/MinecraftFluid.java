package dev.lucaargolo.nexo.feature.fluid;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.fluid.FluidBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class MinecraftFluid extends FluidBase {

    private static final ConcurrentHashMap<Location, FluidBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, Holder<Fluid>> HOLDER_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, Entry> ENTRY_MAP = new ConcurrentHashMap<>();

    public static final Bijection<FluidBase, Holder<Fluid>> CONVERT = new Bijection<>() {
        @Override
        public Holder<Fluid> forward(FluidBase feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public FluidBase backward(Holder<Fluid> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    private final @NotNull Holder<Fluid> holder;

    private MinecraftFluid(@NotNull NexoMinecraft nexo, @NotNull Holder<Fluid> holder) {
        super(MinecraftRoleType.uncraft(nexo, Type.FLUID, holder));
        this.identify(nexo, nexo.getRegistryHandler().identity(holder));
        this.holder = holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static FluidBase lookup(Location location) {
        return FEATURE_MAP.get(location);
    }

    public static FluidBase register(NexoMinecraft nexo, FluidBase fluid) {
        FluidBase registered = FEATURE_MAP.get(fluid.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(fluid.location());
        ResourceLocation flowingId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "flowing_" + id.getPath());
        Entry entry = entryFor(nexo, fluid.location());
        nexo.getRegistryHandler().registerFluidType(entry);
        FEATURE_MAP.put(fluid.location(), fluid);
        nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.FLUID, id, entry::source);
        nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.FLUID, flowingId, entry::flowing);
        Holder<Block> block = nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.BLOCK, id, () -> new RegisteredFluidBlock(entry.source()));
        entry.setBlock(block);
        return fluid;
    }

    public static FluidBase index(NexoMinecraft nexo, Holder<Fluid> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, ignored -> new MinecraftFluid(nexo, holder));
    }

    public static Fluid craft(NexoMinecraft nexo, FluidBase fluid) {
        return entryFor(nexo, fluid.location()).source();
    }

    public static boolean isFluidBlock(@NotNull Location location) {
        return ENTRY_MAP.containsKey(location);
    }

    public static @Nullable Entry entry(@NotNull Location location) {
        return ENTRY_MAP.get(location);
    }

    private static @NotNull Entry entryFor(@NotNull NexoMinecraft nexo, @NotNull Location location) {
        return ENTRY_MAP.computeIfAbsent(location, ignored -> new Entry(nexo, location));
    }

    private static final class RegisteredFluidBlock extends LiquidBlock {

        private RegisteredFluidBlock(@NotNull FlowingFluid fluid) {
            super(fluid, BlockBehaviour.Properties.of()
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid());
        }

    }

    public static class ExtendedFluid extends FlowingFluid {

        private final @NotNull Entry entry;
        private final boolean source;

        public ExtendedFluid(@NotNull Entry entry, boolean source) {
            this.entry = entry;
            this.source = source;
        }

        public @NotNull Entry entry() {
            return this.entry;
        }

        @Override
        protected void createFluidStateDefinition(@NotNull StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            if (!this.source) {
                builder.add(LEVEL);
            }
        }

        @Override
        public @NotNull Fluid getFlowing() {
            return this.entry.flowing();
        }

        @Override
        public @NotNull Fluid getSource() {
            return this.entry.source();
        }

        @Override
        public @NotNull Item getBucket() {
            return Items.AIR;
        }

        @Override
        protected boolean canBeReplacedWith(@NotNull FluidState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Fluid fluid, @NotNull Direction direction) {
            return direction == Direction.DOWN && !this.isSame(fluid);
        }

        @Override
        protected boolean canConvertToSource(@NotNull Level level) {
            return false;
        }

        @Override
        protected void beforeDestroyingBlock(@NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockState state) {
            BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            Block.dropResources(state, level, pos, blockEntity);
        }

        @Override
        protected int getSlopeFindDistance(@NotNull LevelReader level) {
            return 4;
        }

        @Override
        protected int getDropOff(@NotNull LevelReader level) {
            return 1;
        }

        @Override
        public int getTickDelay(@NotNull LevelReader level) {
            return 5;
        }

        @Override
        protected @NotNull BlockState createLegacyBlock(@NotNull FluidState state) {
            Holder<Block> block = this.entry.block();
            if (block == null) {
                return Blocks.AIR.defaultBlockState();
            }
            return block.value().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(@NotNull Fluid fluid) {
            return fluid == this.entry.source() || fluid == this.entry.flowing();
        }

        @Override
        protected float getExplosionResistance() {
            return 100.0F;
        }

        @Override
        public boolean isSource(@NotNull FluidState state) {
            return this.source;
        }

        @Override
        public int getAmount(@NotNull FluidState state) {
            return this.source ? 8 : state.getValue(LEVEL);
        }

    }

    public static final class Entry {

        private final @NotNull NexoMinecraft nexo;
        private final @NotNull Location location;

        private volatile @Nullable ExtendedFluid source;
        private volatile @Nullable ExtendedFluid flowing;
        private volatile @Nullable Holder<Block> block;

        Entry(@NotNull NexoMinecraft nexo, @NotNull Location location) {
            this.nexo = nexo;
            this.location = location;
        }

        public @NotNull Location location() {
            return this.location;
        }

        /**
         * Fluid objects are created lazily: NeoForge only allows constructing vanilla
         * Fluids while its registries are open (inside RegisterEvent), so both the
         * source and the flowing fluid are built on first access instead of eagerly.
         */
        public @NotNull ExtendedFluid source() {
            ExtendedFluid fluid = this.source;
            if (fluid == null) {
                synchronized (this) {
                    fluid = this.source;
                    if (fluid == null) {
                        fluid = createFluid(true);
                        this.source = fluid;
                    }
                }
            }
            return fluid;
        }

        public @NotNull ExtendedFluid flowing() {
            ExtendedFluid fluid = this.flowing;
            if (fluid == null) {
                synchronized (this) {
                    fluid = this.flowing;
                    if (fluid == null) {
                        fluid = createFluid(false);
                        this.flowing = fluid;
                    }
                }
            }
            return fluid;
        }

        private @NotNull ExtendedFluid createFluid(boolean source) {
            return this.nexo.getRegistryHandler().craftFluid(this, source);
        }

        void setBlock(@NotNull Holder<Block> block) {
            if (this.block != null) {
                throw new IllegalStateException("Fluid block is already registered");
            }
            this.block = block;
        }

        @Nullable Holder<Block> block() {
            return this.block;
        }

    }

}
