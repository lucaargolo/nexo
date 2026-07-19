package dev.lucaargolo.nexo.feature.entity;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.NexoHolder;
import dev.lucaargolo.nexo.util.NexoUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class MinecraftEntity extends EntityBase {

    private static final ConcurrentHashMap<Location, EntityBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, NexoHolder<EntityType<?>>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<EntityBase, NexoHolder<EntityType<?>>> CONVERT = new Bijection<>() {
        @Override
        public NexoHolder<EntityType<?>> forward(EntityBase feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public EntityBase backward(NexoHolder<EntityType<?>> holder) {
            return FEATURE_MAP.get(holder.location());
        }
    };

    @NotNull
    private final NexoHolder<EntityType<?>> holder;

    private MinecraftEntity(NexoRegistryHandler<?> helper, @NotNull NexoHolder<EntityType<?>> holder) {
        super(holder.location(), MinecraftRoleType.uncraft(helper, Type.ENTITY, holder));
        this.holder = holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @Nullable Renderer<Graphics3D, EntityUnit<?>> renderer() {
        //TODO: This
        return null;
    }

    public static EntityBase lookup(Location location) {
        return FEATURE_MAP.get(location);
    }

    public static EntityBase register(NexoRegistryHandler<?> helper, EntityBase entity) {
        EntityBase registered = FEATURE_MAP.get(entity.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(entity.location());
        FEATURE_MAP.put(entity.location(), entity);
        helper.registerBuiltinFeature(BuiltInRegistries.ENTITY_TYPE, id, MinecraftFeatureType.ENTITY.craft(helper, entity));
        return entity;
    }

    @SuppressWarnings("deprecation")
    public static NexoHolder<EntityType<?>> index(NexoRegistryHandler<?> helper, EntityType<?> entity) {
        Holder<EntityType<?>> h = entity.builtInRegistryHolder();
        Location location = NexoMinecraft.id(h);
        NexoHolder<EntityType<?>> indexed = HOLDER_MAP.get(location);
        if (indexed != null) {
            return indexed;
        }
        NexoHolder<EntityType<?>> holder = new NexoHolder<>(helper.nexo(), h);
        FEATURE_MAP.putIfAbsent(location, new MinecraftEntity(helper, holder));
        HOLDER_MAP.put(location, holder);
        return holder;
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
