package dev.lucaargolo.nexo;

import com.google.common.collect.Maps;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.feature.block.IBlock;
import dev.lucaargolo.nexo.api.feature.data.IData;
import dev.lucaargolo.nexo.api.feature.item.IItem;
import dev.lucaargolo.nexo.api.feature.item.IItemCategory;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import dev.lucaargolo.nexo.feature.MinecraftData;
import dev.lucaargolo.nexo.feature.MinecraftItem;
import dev.lucaargolo.nexo.feature.MinecraftItemCategory;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(NexoMinecraft.MOD_ID)
public class NeoForgeNexoMinecraft extends NexoMinecraft {

    private final IEventBus modBus;

    private final Map<Class<? extends IFeature>, Map<Location, IFeature>> FEATURE_REGISTRY = Maps.newHashMap();
    private final Map<String, DeferredRegister.Blocks> BLOCKS = new ConcurrentHashMap<>();
    private final Map<String, DeferredRegister.Items> ITEMS = new ConcurrentHashMap<>();
    private final Map<String, DeferredRegister<CreativeModeTab>> CREATIVE_TABS = new ConcurrentHashMap<>();
    private final Map<String, DeferredRegister<DataComponentType<?>>> DATA_COMPONENTS = new ConcurrentHashMap<>();
    private final Map<String, DeferredRegister<AttachmentType<?>>> ATTACHMENTS = new ConcurrentHashMap<>();

    public NeoForgeNexoMinecraft(IEventBus modBus) {
        this.modBus = modBus;
        this.init();
    }

    public IEventBus getModBus() {
        return modBus;
    }

    @Override
    public String getPlatform() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public @Nullable Mod getMod(@NotNull String id) {
        return this.modDiscovery.getMod(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends IFeature, I extends T> T registerFeature(Class<T> type, I feature) {
        Location location = feature.location();
        if (type.isAssignableFrom(IBlock.class) && feature instanceof IBlock block) {
            DeferredRegister.Blocks registry = BLOCKS.computeIfAbsent(location.namespace(), ns -> {
                DeferredRegister.Blocks dr = DeferredRegister.createBlocks(ns);
                dr.register(modBus);
                return dr;
            });
            DeferredHolder<Block, ? extends Block> holder = registry.register(location.path(), () -> new Block(BlockBehaviour.Properties.of()));
            MinecraftBlock minecraftBlock = emit(new FeatureRegisteredEvent<>(location, new MinecraftBlock(holder, block)));
            FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftBlock);
            return (T) minecraftBlock;
        }else if(type.isAssignableFrom(IItem.class) && feature instanceof IItem item) {
            DeferredRegister.Items registry = ITEMS.computeIfAbsent(location.namespace(), ns -> {
                DeferredRegister.Items dr = DeferredRegister.createItems(ns);
                dr.register(modBus);
                return dr;
            });
            DeferredHolder<Item, ? extends Item> holder = registry.register(location.path(), () -> new Item(new Item.Properties()));
            MinecraftItem minecraftItem = emit(new FeatureRegisteredEvent<>(location, new MinecraftItem(holder, item)));
            FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftItem);
            return (T) minecraftItem;
        }else if(type.isAssignableFrom(IItemCategory.class) && feature instanceof IItemCategory itemCategory) {
            DeferredRegister<CreativeModeTab> registry = CREATIVE_TABS.computeIfAbsent(location.namespace(), ns -> {
                DeferredRegister<CreativeModeTab> dr = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ns);
                dr.register(modBus);
                return dr;
            });
            DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> holder = registry.register(location.path(), () -> CreativeModeTab.builder().build());
            MinecraftItemCategory minecraftItemCategory = emit(new FeatureRegisteredEvent<>(location, new MinecraftItemCategory(holder, itemCategory)));
            FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftItemCategory);
            return (T) minecraftItemCategory;
        }else if(type.isAssignableFrom(IData.class) && feature instanceof IData<?> data) {
            DeferredRegister<DataComponentType<?>> dcRegistry = DATA_COMPONENTS.computeIfAbsent(location.namespace(), ns -> {
                DeferredRegister<DataComponentType<?>> dr = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ns);
                dr.register(modBus);
                return dr;
            });

            DataComponentType.Builder dcBuilder = DataComponentType.builder();
            if(data.persistent()) dcBuilder.persistent(NexoMinecraft.createCodec(data));
            if(data.synced()) dcBuilder.networkSynchronized(NexoMinecraft.createPacketCodec(data));

            DataComponentType<?> dc = dcBuilder.build();
            DeferredHolder<DataComponentType<?>, ? extends DataComponentType<?>> dcHolder = dcRegistry.register(location.path(), () -> dc);

            DeferredRegister<AttachmentType<?>> atRegistry = ATTACHMENTS.computeIfAbsent(location.namespace(), ns -> {
                DeferredRegister<AttachmentType<?>> dr = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ns);
                dr.register(modBus);
                return dr;
            });

            AttachmentType.Builder atBuilder = AttachmentType.builder(() -> null);
            if(data.persistent()) {
                atBuilder.serialize(NexoMinecraft.createCodec(data));
                atBuilder.copyOnDeath();
            }
            if(data.synced()) atBuilder.sync(NexoMinecraft.createPacketCodec(data));
            AttachmentType<?> at = atBuilder.build();
            atRegistry.register(location.path(), () -> at);

            MinecraftData<?> minecraftData = emit(new FeatureRegisteredEvent<>(location, new MinecraftData(dcHolder, data)));
            FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftData);
            return (T) minecraftData;
        }
        return null;
    }

    @Override
    public @NotNull <T extends IFeature> Map<Location, IFeature> getFeatureRegistry(Class<T> type) {
        return FEATURE_REGISTRY.getOrDefault(type, Map.of());
    }
}
