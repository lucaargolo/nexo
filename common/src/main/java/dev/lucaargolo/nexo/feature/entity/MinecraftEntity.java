package dev.lucaargolo.nexo.feature.entity;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class MinecraftEntity extends EntityBase {

    private static final ConcurrentHashMap<Location, EntityBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, NexoHolder<EntityType<?>, EntityType<?>>> HOLDER_MAP = new ConcurrentHashMap<>();

    @NotNull
    private final NexoHolder<EntityType<?>, EntityType<?>> holder;

    private MinecraftEntity(@NotNull NexoHolder<EntityType<?>, EntityType<?>> holder) {
        super(holder.location());
        this.holder = holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static EntityType<?> crafted(EntityBase entity) {
        return Objects.requireNonNull(HOLDER_MAP.get(entity.location())).get();
    }

    public static EntityBase lookup(NexoRegistryHandler<?> helper, Location location) {
        return FEATURE_MAP.computeIfAbsent(location, l -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
            return helper.getRegistry().registry(Registries.ENTITY_TYPE)
                    .flatMap(registry -> registry.getHolder(id))
                    .map(holder -> {
                        NexoHolder<EntityType<?>, EntityType<?>> nexoHolder = new NexoHolder<>(
                                helper.nexo(),
                                holder.unwrapKey().orElseThrow(),
                                holder::value
                        );
                        HOLDER_MAP.put(location, nexoHolder);
                        return (EntityBase) new MinecraftEntity(nexoHolder);
                    })
                    .orElse(null);
        });
    }

    public static EntityBase register(NexoRegistryHandler<?> helper, EntityBase entity) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(entity.location().namespace(), entity.location().path());
        NexoHolder<EntityType<?>, EntityType<?>> holder = helper.registerBuiltinFeature(BuiltInRegistries.ENTITY_TYPE, id, MinecraftFeatureType.ENTITY.craft(helper, entity));
        FEATURE_MAP.put(entity.location(), entity);
        HOLDER_MAP.put(entity.location(), holder);
        return entity;
    }

    public static EntityType<?> craft(NexoRegistryHandler<?> helper, EntityBase entity) {
        EntityType.EntityFactory<?> factory = (type, level) -> new Entity(type, level) {
            @Override
            protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {

            }

            @Override
            protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {

            }

            @Override
            protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {

            }
        };
        return EntityType.Builder
                .of(factory, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .build(entity.location().toString());
    }

}
