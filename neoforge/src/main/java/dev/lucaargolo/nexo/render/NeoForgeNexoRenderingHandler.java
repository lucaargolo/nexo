package dev.lucaargolo.nexo.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.event.ModelLoadingQueryEvent;
import dev.lucaargolo.nexo.event.SpriteAtlasStitchEvent;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class NeoForgeNexoRenderingHandler extends NexoRenderingHandler<NeoForgeNexoMinecraft> {

    private final Map<ResourceLocation, Supplier<UnbakedModel>> customModels = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Supplier<UnbakedModel>> blockModels = new ConcurrentHashMap<>();

    private final List<ResourceLocation> itemModels = new ArrayList<>();
    private final List<ItemBase> itemsToRegister = new ArrayList<>();
    private final List<EntityBase> entitiesToRegister = new ArrayList<>();

    public NeoForgeNexoRenderingHandler(NeoForgeNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        super.init();
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, event -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) shaderRenderer.beginFrame();
            else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) shaderRenderer.endFrame();
        });
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> shaderRenderer.close());
        this.nexo().modBus().addListener(ModelEvent.RegisterAdditional.class, event -> {
            for (ResourceLocation modelId : itemModels) {
                event.register(new ModelResourceLocation(modelId, ModelResourceLocation.STANDALONE_VARIANT));
            }
        });
        this.nexo().modBus().addListener(RegisterClientExtensionsEvent.class, event -> {
            for (ItemBase base : itemsToRegister) {
                Item item = MinecraftFeatureType.ITEM.convert(base);
                IClientItemExtensions extensions = createItemExtensions(this.nexo(), base);
                event.registerItem(extensions, item);
            }
        });
        this.nexo().modBus().addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            for (EntityBase base : entitiesToRegister) {
                EntityType<? extends Entity> type = MinecraftFeatureType.ENTITY.convert(base);
                registerEntityRenderer(this.nexo(), type, base, event::registerEntityRenderer);
            }
        });
        this.nexo().modBus().addListener(ModelLoadingQueryEvent.class, event -> {
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
        this.nexo().modBus().addListener(SpriteAtlasStitchEvent.class, event -> {
            event.registered().putAll(nexoAtlas.getRegistered(event.atlas()));
            event.embedded().putAll(nexoAtlas.getEmbedded(event.atlas()));
        });
    }

    @Override
    protected void collectModel(@NotNull Feature<?> feature, @NotNull ResourceLocation modelId, @NotNull Supplier<UnbakedModel> model) {
        registerResourceModel(modelId, model);
        if (feature instanceof BlockBase) {
            ResourceLocation blockKey = NexoMinecraft.rl(feature.location());
            blockModels.put(blockKey, model);
        } else if (feature instanceof ItemBase) {
            itemModels.add(modelId);
        }
    }


    @Override
    public void registerResourceModel( @NotNull ResourceLocation modelId, @NotNull Supplier<UnbakedModel> model) {
        customModels.put(modelId, model);
    }

    @Override
    protected void registerItemRenderer(ItemBase item) {
        itemsToRegister.add(item);
    }

    @Override
    protected void registerEntityRenderer(EntityBase entity) {
        entitiesToRegister.add(entity);
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

}
