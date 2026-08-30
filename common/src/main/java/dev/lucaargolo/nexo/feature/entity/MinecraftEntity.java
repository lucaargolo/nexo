package dev.lucaargolo.nexo.feature.entity;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import dev.lucaargolo.nexo.unit.MinecraftEquipmentVault;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private MinecraftEntity(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Holder<EntityType<?>> holder) {
        super(NexoMinecraft.id(holder), MinecraftRoleType.uncraft(nexo, Type.ENTITY, holder));
        this.holder = holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @Nullable Renderer<Graphics3D, EntityUnit<?>> renderer() {
        // Minecraft-backed features are created from vanilla holders and carry no user-supplied renderer.
        return null;
    }

    @Override
    public <V extends Unit<?, ?>> @NotNull Map<String, Function<EntityUnit<?>, ? extends @Nullable Vault<V>>> vaults(@NotNull Class<V> type) {
        if (!MinecraftContainerVault.supports(type)) {
            return Map.of();
        }
        Map<String, Function<EntityUnit<?>, ? extends @Nullable Vault<V>>> vaults = new LinkedHashMap<>();
        vaults.put(MinecraftContainerVault.KEY, unit -> unit.vault(type, MinecraftContainerVault.KEY));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String key = MinecraftEquipmentVault.key(slot);
            vaults.put(key, unit -> unit.vault(type, key));
        }
        return Collections.unmodifiableMap(vaults);
    }

    public static EntityBase lookup(Location location) {
        return FEATURE_MAP.get(location);
    }

    public static EntityBase register(NexoMinecraft<?, ?, ?, ?> nexo, EntityBase entity) {
        EntityBase registered = FEATURE_MAP.get(entity.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(entity.location());
        FEATURE_MAP.put(entity.location(), entity);
        Holder<EntityType<?>> entityHolder = nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.ENTITY_TYPE, id, MinecraftFeatureType.ENTITY.craft(nexo, entity));
        nexo.getRegistryHandler().registerVaults(MinecraftFeatureType.ENTITY, entity, entityHolder::value);
        return entity;
    }

    public static EntityBase index(NexoMinecraft<?, ?, ?, ?> nexo, Holder<EntityType<?>> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, l -> new MinecraftEntity(nexo, holder));
    }

    public static <M extends Entity> EntityType<?> craft(NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Utils.Extender<M> extender, @Nullable Function<Parameters, M> factory, EntityBase entity) {
        extender.override(Utils.At.AFTER_SUPER, "tick", void.class, feature -> {
            if (entity.ticker() != null) {
                entity.ticker().tick(nexo.entityToUnit(feature));
            }
            return null;
        });
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
