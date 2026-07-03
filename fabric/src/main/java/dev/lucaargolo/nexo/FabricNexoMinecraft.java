package dev.lucaargolo.nexo;

import com.google.common.collect.Maps;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.feature.block.IBlock;
import dev.lucaargolo.nexo.api.component.BlockItemComponent;
import dev.lucaargolo.nexo.api.feature.data.IData;
import dev.lucaargolo.nexo.api.feature.item.IItem;
import dev.lucaargolo.nexo.api.feature.item.IItemCategory;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FabricNexoMinecraft extends NexoMinecraft implements ModInitializer {

    @Override
    public void onInitialize() {
        this.init();
    }

    @Override
    public String getPlatform() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public @Nullable Mod getMod(@NotNull String id) {
        return this.modDiscovery.getMod(id);
    }

}
