package dev.lucaargolo.nexo;

import com.mojang.authlib.GameProfile;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.util.Side;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FabricNexoMinecraft extends NexoMinecraft implements ModInitializer {

    @Nullable
    private MinecraftServer currentServer;

    @Override
    public void onInitialize() {
        this.init();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);
    }

    @Override
    public String getPlatform() {
        return "Fabric";
    }

    @Override
    public @NotNull Entity createEntity(@NotNull EntityType<?> type, @NotNull Level level, @NotNull EntityBase feature) {
        if (feature.get(PlayerRole.class) instanceof PlayerRole(UUID uuid, String name) && level instanceof ServerLevel serverLevel) {
            return FakePlayer.get(serverLevel, new GameProfile(uuid, name));
        }
        return super.createEntity(type, level, feature);
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Side getSide() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? Side.CLIENT : Side.SERVER;
    }

    @Override
    public @Nullable Mod getMod(@NotNull String id) {
        return this.discoveryHandler.getMod(id);
    }

    @Override
    public MinecraftServer getServer() {
        return this.currentServer;
    }

}
