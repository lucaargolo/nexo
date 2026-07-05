package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.model.NeoForgeNexoModelHandler;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

@Mixin(BlockStateModelLoader.class)
public abstract class BlockStateModelLoaderMixin {

    @Accessor("discoveredModelOutput")
    abstract BiConsumer<ModelResourceLocation, UnbakedModel> getDiscoveredModelOutput();

    @Inject(method = "loadBlockStateDefinitions", at = @At("HEAD"), cancellable = true)
    private void nexo$injectBlockStates(ResourceLocation id, StateDefinition<Block, BlockState> definition, CallbackInfo ci) {
        UnbakedModel customModel = NeoForgeNexoModelHandler.getBlockModel(id);
        if (customModel != null) {
            BiConsumer<ModelResourceLocation, UnbakedModel> output = getDiscoveredModelOutput();
            definition.getPossibleStates().forEach(state -> {
                ModelResourceLocation mrl = BlockModelShaper.stateToModelLocation(id, state);
                output.accept(mrl, customModel);
            });
            ci.cancel();
        }
    }
}
