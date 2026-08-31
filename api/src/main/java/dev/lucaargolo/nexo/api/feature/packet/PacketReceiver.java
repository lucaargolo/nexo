package dev.lucaargolo.nexo.api.feature.packet;

import org.jetbrains.annotations.NotNull;


public interface PacketReceiver {

    static @NotNull PacketReceiver client() {
        return Endpoint.CLIENT;
    }

    static @NotNull PacketReceiver server() {
        return Endpoint.SERVER;
    }

    enum Endpoint implements PacketReceiver {
        CLIENT,
        SERVER
    }

}
