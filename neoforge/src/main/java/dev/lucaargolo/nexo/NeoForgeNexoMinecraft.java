package dev.lucaargolo.nexo;

import com.mojang.authlib.GameProfile;
import dev.lucaargolo.nexo.api.feature.packet.PacketReceiver;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.event.LanguageLookupEvent;
import dev.lucaargolo.nexo.event.LanguageReloadEvent;
import dev.lucaargolo.nexo.feature.packet.MinecraftPacketPayload;
import dev.lucaargolo.nexo.render.NeoForgeMinecraftRenderingHandler;
import dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Mod(NexoMinecraft.MOD_ID)
public class NeoForgeNexoMinecraft extends NexoMinecraft<NeoForgeNexoMinecraft, NeoForgeNexoModDiscoveryHandler, NeoForgeMinecraftRegistryHandler, NeoForgeMinecraftRenderingHandler> {

    private final IEventBus modBus;

    public NeoForgeNexoMinecraft(IEventBus modBus) {
        this.modBus = modBus;
        this.modBus.addListener(this::registerPackets);
        NeoForge.EVENT_BUS.addListener(LanguageLookupEvent.class, event -> event.translation(this.getLanguageHandler().translateNexo(event.key())));
        NeoForge.EVENT_BUS.addListener(LanguageReloadEvent.class, event -> this.getLanguageHandler().select(event.locale()));
        this.init();
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> this.tickWorld(event.getLevel()));
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playBidirectional(MinecraftPacketPayload.TYPE, MinecraftPacketPayload.CODEC, (payload, context) -> {
            PacketReceiver receiver = context.flow() == PacketFlow.SERVERBOUND ? this.entityToUnit((ServerPlayer) context.player()) : PacketReceiver.client();
            this.handleMinecraftPacket(payload, receiver);
        });
    }

    @Override
    protected void sendMinecraftPacket(@NotNull PacketReceiver receiver, @NotNull MinecraftPacketPayload payload) {
        if (receiver == PacketReceiver.server()) {
            PacketDistributor.sendToServer(payload);
        } else if (receiver instanceof MinecraftEntityUnit<?, ?, ?> unit && unit.get() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, payload);
        } else {
            throw new IllegalArgumentException("Minecraft packets can only be sent to the server or a server player");
        }
    }

    public IEventBus modBus() {
        return modBus;
    }

    @Override
    public String getPlatform() {
        return "NeoForge";
    }

    @Override
    public String getMapping(@NotNull Class<?> ownerType, @NotNull String memberName, @NotNull Class<?> returnType, Class<?>... parameterTypes) {
        return memberName;
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

    @Override
    public Player createFakePlayer(Level level, UUID uuid, String name) {
        return FakePlayerFactory.get((ServerLevel) level, new GameProfile(uuid, name));
    }

}
