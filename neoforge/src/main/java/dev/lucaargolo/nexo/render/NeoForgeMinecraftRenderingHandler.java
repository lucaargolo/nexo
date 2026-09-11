package dev.lucaargolo.nexo.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.lucaargolo.nexo.NeoForgeMinecraftRegistryHandler;
import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.event.AtlasStitchedEvent;
import dev.lucaargolo.nexo.event.InjectOnAtlasStitchEvent;
import dev.lucaargolo.nexo.event.ModelLoadingQueryEvent;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.block.MinecraftBlock;
import dev.lucaargolo.nexo.feature.fluid.MinecraftFluid;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NeoForgeMinecraftRenderingHandler extends MinecraftRenderingHandler {

    private final Map<ResourceLocation, Supplier<UnbakedModel>> customModels = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Supplier<UnbakedModel>> blockModels = new ConcurrentHashMap<>();

    private final List<ResourceLocation> itemModels = new ArrayList<>();
    private final List<ItemBase> itemsToRegister = new ArrayList<>();
    private final List<BlockBase> blocksToRegister = new ArrayList<>();
    private final List<EntityBase> entitiesToRegister = new ArrayList<>();
    private final List<Consumer<RegisterMenuScreensEvent>> menuScreensToRegister = new ArrayList<>();
    private final List<Supplier<FluidType>> fluidTypesToRegister = new ArrayList<>();

    private static final IClientFluidTypeExtensions FLUID_EXTENSIONS = new IClientFluidTypeExtensions() {
        private static final int WATER_TINT = 0xFF3F76E4;

        @Override
        public ResourceLocation getStillTexture() {
            return ResourceLocation.withDefaultNamespace("block/water_still");
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return ResourceLocation.withDefaultNamespace("block/water_flow");
        }

        @Override
        public ResourceLocation getOverlayTexture() {
            return ResourceLocation.withDefaultNamespace("block/water_overlay");
        }

        @Override
        public int getTintColor() {
            return WATER_TINT;
        }

        @Override
        public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return getter != null && pos != null ? BiomeColors.getAverageWaterColor(getter, pos) : WATER_TINT;
        }
    };

    public NeoForgeMinecraftRenderingHandler(NeoForgeNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        super.init();
        NeoForgeNexoMinecraft nexo = (NeoForgeNexoMinecraft) this.nexo;
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, event -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
                shaderHandler.beginFrame();
            }
            else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
                shaderHandler.endFrame();
            }
        });
        NeoForge.EVENT_BUS.addListener(GameShuttingDownEvent.class, event -> shaderHandler.close());
        nexo.modBus().addListener(ModelEvent.RegisterAdditional.class, event -> {
            for (ResourceLocation modelId : itemModels) {
                event.register(new ModelResourceLocation(modelId, ModelResourceLocation.STANDALONE_VARIANT));
            }
        });
        nexo.modBus().addListener(RegisterClientExtensionsEvent.class, event -> {
            for (ItemBase base : itemsToRegister) {
                Item item = MinecraftFeatureType.ITEM.convert(base);
                IClientItemExtensions extensions = createItemExtensions(this.nexo, base);
                event.registerItem(extensions, item);
            }
            for (Supplier<FluidType> type : fluidTypesToRegister) {
                event.registerFluidType(FLUID_EXTENSIONS, type.get());
            }
        });
        nexo.modBus().addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            for (BlockBase base : blocksToRegister) {
                BlockEntityType<?> type = MinecraftBlock.CONVERT_ENTITY.forward(base).value();
                this.registerBlockRenderer(type, base, event::registerBlockEntityRenderer);
            }
            for (EntityBase base : entitiesToRegister) {
                EntityType<? extends Entity> type = MinecraftFeatureType.ENTITY.convert(base);
                this.registerEntityRenderer(type, base, event::registerEntityRenderer);
            }
        });
        nexo.modBus().addListener(RegisterMenuScreensEvent.class, event -> {
            for (Consumer<RegisterMenuScreensEvent> registration : menuScreensToRegister) {
                registration.accept(event);
            }
        });
        nexo.modBus().addListener(ModelLoadingQueryEvent.class, event -> {
            UnbakedModel model;
            Supplier<UnbakedModel> supplier = customModels.get(event.id());
            if (supplier != null) {
                model = supplier.get();
                if (model != null) { event.setResult(model); return; }
            }
            supplier = blockModels.get(event.id());
            if (supplier != null) {
                model = supplier.get();
                if (model != null) { event.setResult(model); return; }
            }
        });
        nexo.modBus().addListener(InjectOnAtlasStitchEvent.class, event -> {
            event.injected().addAll(this.atlasHandler.getSpriteContents(event.atlas()));
        });
        nexo.modBus().addListener(AtlasStitchedEvent.class, event -> {
            this.atlasHandler.onAtlasStitched(event.atlas(), event.preparations());
        });
        nexo.modBus().addListener(RegisterClientReloadListenersEvent.class, event -> event.registerReloadListener(atlasHandler));
    }

    @Override
    public void registerModel(@NotNull ResourceLocation modelId, @NotNull Supplier<UnbakedModel> model) {
        customModels.put(modelId, model);
    }

    @Override
    protected void collectModel(@NotNull Feature<?, ?> feature, @NotNull ResourceLocation modelId, @NotNull Supplier<UnbakedModel> model) {
        registerModel(modelId, model);
        if (feature instanceof BlockBase) {
            ResourceLocation blockKey = NexoMinecraft.rl(feature.location());
            blockModels.put(blockKey, model);
        } else if (feature instanceof ItemBase) {
            itemModels.add(modelId);
        }
    }

    @Override
    protected void registerFluidBlock(@NotNull BlockBase block) {
        if (!this.nexo.getSide().isClient()) {
            return;
        }
        MinecraftFluid.Entry entry = MinecraftFluid.entry(block.location());
        if (entry == null) {
            return;
        }
        NeoForgeMinecraftRegistryHandler registryHandler = (NeoForgeMinecraftRegistryHandler) this.nexo.getRegistryHandler();
        this.fluidTypesToRegister.add(() -> registryHandler.getFluidType(entry));
        this.blockModels.put(NexoMinecraft.rl(block.location()), () -> BlockModel.fromString("{\"textures\":{\"particle\":\"minecraft:block/water_still\"}}"));
        ItemBlockRenderTypes.setRenderLayer(entry.source(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(entry.flowing(), RenderType.translucent());
    }

    @Override
    public void registerBlockRenderer(BlockBase block) {
        blocksToRegister.add(block);
    }

    @Override
    protected void registerItemRenderer(ItemBase item) {
        itemsToRegister.add(item);
    }

    private IClientItemExtensions createItemExtensions(NexoMinecraft nexo, ItemBase base) {
        ItemRenderer renderer = createItemRenderer(nexo, base);
        Minecraft minecraft = Minecraft.getInstance();
        BlockEntityRenderDispatcher dispatcher = minecraft.getBlockEntityRenderDispatcher();
        EntityModelSet models = minecraft.getEntityModels();
        return new IClientItemExtensions() {
            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return new BlockEntityWithoutLevelRenderer(dispatcher, models) {
                @Override
                public void renderByItem(@NotNull ItemStack pStack, @NotNull ItemDisplayContext pDisplayContext, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
                    renderer.render(pStack, pDisplayContext, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
                }
            };
            }
        };
    }

    @Override
    protected void registerEntityRenderer(EntityBase entity) {
        entitiesToRegister.add(entity);
    }

    @Override
    protected <D, T extends MenuType<MinecraftScreen.ExtendedMenu<D>> & MinecraftScreen.ExtendedMenuType<D>> void registerMenuScreen(Supplier<T> supplier) {
        menuScreensToRegister.add(event -> event.register(supplier.get(), supplier.get()::craftScreen));
    }

}
