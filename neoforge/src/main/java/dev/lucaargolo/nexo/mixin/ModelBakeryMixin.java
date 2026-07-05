package dev.lucaargolo.nexo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.lucaargolo.nexo.model.NeoForgeNexoModelHandler;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ModelBakery.class)
public abstract class ModelBakeryMixin {

    @Accessor("unbakedCache")
    abstract Map<ResourceLocation, UnbakedModel> getUnbakedCache();

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void nexo$injectCustomModel(ResourceLocation id, CallbackInfoReturnable<UnbakedModel> cir) {
        UnbakedModel custom = NeoForgeNexoModelHandler.getCustomModel(id);
        if (custom != null) {
            getUnbakedCache().put(id, custom);
            cir.setReturnValue(custom);
            cir.cancel();
        }
    }

    @WrapOperation(
        method = "registerModelAndLoadDependencies",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/ModelBakery;registerModel(Lnet/minecraft/client/resources/model/ModelResourceLocation;Lnet/minecraft/client/resources/model/UnbakedModel;)V"
        )
    )
    private void nexo$replaceBlockStateModel(ModelBakery instance, ModelResourceLocation mrl, UnbakedModel model, Operation<Void> original) {
        UnbakedModel customModel = NeoForgeNexoModelHandler.getBlockModel(mrl.id());
        if (customModel != null) {
            original.call(instance, mrl, customModel);
        } else {
            original.call(instance, mrl, model);
        }
    }
}
