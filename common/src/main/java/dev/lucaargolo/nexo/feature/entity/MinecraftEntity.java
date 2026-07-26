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
import dev.lucaargolo.nexo.util.NexoUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class MinecraftEntity extends EntityBase {

    private static final ConcurrentHashMap<Location, EntityBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, Holder<EntityType<?>>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<EntityBase, Holder<EntityType<?>>> CONVERT = new Bijection<>() {
        @Override
        public Holder<EntityType<?>> forward(EntityBase feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public EntityBase backward(Holder<EntityType<?>> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    @NotNull
    private final Holder<EntityType<?>> holder;

    private MinecraftEntity(NexoRegistryHandler<?> helper, @NotNull Holder<EntityType<?>> holder) {
        super(NexoMinecraft.id(holder), MinecraftRoleType.uncraft(helper, Type.ENTITY, holder));
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

    public static EntityBase index(NexoRegistryHandler<?> helper, Holder<EntityType<?>> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, l -> new MinecraftEntity(helper, holder));
    }

    public static <M extends Entity> EntityType<?> craft(NexoRegistryHandler<?> helper, @NotNull NexoUtils.Extender<M> extender, @Nullable Function<Parameters, M> factory, EntityBase entity) {
        extender.override(NexoUtils.At.AFTER_SUPER, "tick", void.class, feature -> {
            if (entity.ticker() != null) {
                entity.ticker().tick(helper.nexo().entityToUnit(feature));
            }
            return null;
        });
        extender.override(NexoUtils.At.AFTER_SUPER, "defineSynchedData", void.class, SynchedEntityData.Builder.class, (feature, builder) -> null);
        extender.override(NexoUtils.At.AFTER_SUPER, "readAdditionalSaveData", void.class, CompoundTag.class, (feature, tag) -> null);
        extender.override(NexoUtils.At.AFTER_SUPER, "addAdditionalSaveData", void.class, CompoundTag.class, (feature, tag) -> null);

        Function<Parameters, M> entityFactory = factory != null ? factory : parameters -> extender.instantiate(parameters.type(), parameters.level());
        return EntityType.Builder
                .of((type, level) -> entityFactory.apply(new Parameters(type, level)), MobCategory.MISC)
                .noSave()
                .sized(0.6F, 1.8F)
                .build(entity.location().toString());
    }

    public record Parameters(@NotNull EntityType<?> type, @NotNull Level level) {
    }

}
