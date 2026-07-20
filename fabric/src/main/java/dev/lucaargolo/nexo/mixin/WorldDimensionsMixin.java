package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.event.WorldDimensionsBakeCallback;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(WorldDimensions.class)
public class WorldDimensionsMixin {

    @Shadow
    @Final
    private Map<ResourceKey<LevelStem>, LevelStem> dimensions;

    @Inject(at = @At("RETURN"), method = "bake")
    private void nexo$indexBakedRegistry(Registry<LevelStem> pStemRegistry, CallbackInfoReturnable<WorldDimensions.Complete> cir) {
        WorldDimensions.Complete complete = cir.getReturnValue();
        Registry<LevelStem> registry = complete.dimensions();
        WorldDimensionsBakeCallback.EVENT.invoker().onBake(registry, dimensions);
    }

}
