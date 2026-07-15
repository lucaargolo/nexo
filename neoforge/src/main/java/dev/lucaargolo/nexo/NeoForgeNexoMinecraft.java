package dev.lucaargolo.nexo;

import com.mojang.authlib.GameProfile;
import dev.lucaargolo.nexo.api.role.PlayerRole;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.util.Side;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod(NexoMinecraft.MOD_ID)
public class NeoForgeNexoMinecraft extends NexoMinecraft {

    private final IEventBus modBus;

    public NeoForgeNexoMinecraft(IEventBus modBus) {
        this.modBus = modBus;
        this.init();
    }

    public IEventBus modBus() {
        return modBus;
    }

    @Override
    public String getPlatform() {
        return "NeoForge";
    }

    @Override
    public Entity createEntity(EntityType<?> type, Level level, EntityBase feature) {
        if (feature.getRole(PlayerRole.class) instanceof PlayerRole player && level instanceof ServerLevel serverLevel) {
            return new net.neoforged.neoforge.common.util.FakePlayer(serverLevel, new GameProfile(player.uuid(), player.name()));
        }
        return super.createEntity(type, level, feature);
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Side getSide() {
        return FMLEnvironment.dist.isClient() ? Side.CLIENT : Side.SERVER;
    }

    @Override
    public @Nullable Mod getMod(@NotNull String id) {
        return this.discoveryHandler.getMod(id);
    }

    @Override
    public MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

}
