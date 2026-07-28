package dev.lucaargolo.nexo;

import com.mojang.authlib.GameProfile;
import dev.lucaargolo.nexo.api.feature.packet.PacketReceiver;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.feature.packet.MinecraftPacketPayload;
import dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FabricNexoMinecraft extends NexoMinecraft implements ModInitializer {

    @Nullable
    private MinecraftServer currentServer;

    @Override
    public void onInitialize() {
        this.init();
        PayloadTypeRegistry.playC2S().register(MinecraftPacketPayload.TYPE, MinecraftPacketPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MinecraftPacketPayload.TYPE, MinecraftPacketPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MinecraftPacketPayload.TYPE, (payload, context) -> this.handleMinecraftPacket(payload, this.entityToUnit(context.player())));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);
        ServerTickEvents.END_WORLD_TICK.register(this::tickWorld);
        if (this.getSide() == Side.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(MinecraftPacketPayload.TYPE, (payload, context) -> this.handleMinecraftPacket(payload, PacketReceiver.client()));
            ClientTickEvents.END_WORLD_TICK.register(this::tickWorld);
        }
    }

    @Override
    protected void sendMinecraftPacket(@NotNull PacketReceiver receiver, @NotNull MinecraftPacketPayload payload) {
        if (receiver == PacketReceiver.server()) {
            ClientPlayNetworking.send(payload);
        } else if (receiver instanceof MinecraftEntityUnit<?, ?, ?> unit && unit.get() instanceof ServerPlayer player) {
            ServerPlayNetworking.send(player, payload);
        } else {
            throw new IllegalArgumentException("Minecraft packets can only be sent to the server or a server player");
        }
    }

    @Override
    public String getPlatform() {
        return "Fabric";
    }

    @Override
    public String getMapping(@NotNull Class<?> ownerType, @NotNull String memberName, @NotNull Class<?> returnType, Class<?>... parameterTypes) {
        MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();
        String namedOwner = resolver.unmapClassName("named", ownerType.getName());
        StringBuilder descriptorBuilder = new StringBuilder("(");
        for (Class<?> parameterType : parameterTypes) {
            String parameterDescriptor = this.getDescriptor(resolver, parameterType);
            descriptorBuilder.append(parameterDescriptor);
        }
        String returnDescriptor = this.getDescriptor(resolver, returnType);
        String fullDescriptor = descriptorBuilder.append(')').append(returnDescriptor).toString();
        return resolver.mapMethodName("named", namedOwner, memberName, fullDescriptor);
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

    @Override
    public Player createFakePlayer(Level level, UUID uuid, String name) {
        return FakePlayer.get((ServerLevel) level, new GameProfile(uuid, name));
    }

    private String getDescriptor(MappingResolver resolver, Class<?> type) {
        if (type.isArray())
            return '[' + getDescriptor(resolver, type.getComponentType());
        if (!type.isPrimitive()) {
            return 'L' + resolver.unmapClassName("named", type.getName()).replace('.', '/') + ';';
        }
        if (type == void.class) return "V";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        if (type == double.class) return "D";
        throw new IllegalArgumentException("Unsupported method type: " + type.getName());
    }

}
