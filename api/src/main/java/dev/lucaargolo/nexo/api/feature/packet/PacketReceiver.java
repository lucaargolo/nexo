package dev.lucaargolo.nexo.api.feature.packet;

import org.jetbrains.annotations.NotNull;

/**
 * A logical endpoint for a packet.
 */
public interface PacketReceiver {

    /**
     * The logical game client. Packets received by a client are handled with this receiver.
     */
    static @NotNull PacketReceiver client() {
        return Endpoint.CLIENT;
    }

    /**
     * The logical game server. Use this receiver to send a packet from a client to its server.
     */
    static @NotNull PacketReceiver server() {
        return Endpoint.SERVER;
    }

    enum Endpoint implements PacketReceiver {
        CLIENT,
        SERVER
    }

}
