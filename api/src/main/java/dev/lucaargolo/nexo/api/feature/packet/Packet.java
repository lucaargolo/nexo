package dev.lucaargolo.nexo.api.feature.packet;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

public abstract class Packet<D, R extends PacketReceiver> extends Feature<Packet<?, ?>, Unit<Packet<?, ?>, ?>> {

    @NotNull
    private final DataBase<D> data;
    @NotNull
    private final Class<R> receiverType;
    @NotNull
    private final ThreadLocal<Deque<D>> receivedValues = ThreadLocal.withInitial(ArrayDeque::new);

    protected Packet(@NotNull Location location, @NotNull DataBase<D> data, @NotNull Class<R> receiverType) {
        super(location);
        this.data = data;
        this.receiverType = receiverType;
    }

    @Override
    public final @NotNull Type<Packet<?, ?>, Unit<Packet<?, ?>, ?>> type() {
        return Type.packet();
    }

    public final @NotNull DataBase<D> data() {
        return data;
    }

    public final @NotNull Class<R> receiverType() {
        return receiverType;
    }


    /**
     * Returns the value received in the current handler invocation, or the data's initial value when called outside of a handler.
     */
    public final @NotNull D value() {
        Deque<D> values = receivedValues.get();
        return values.isEmpty() ? data.initial() : values.peek();
    }

    public abstract void handle(@NotNull R receiver);

    @ApiStatus.Internal
    public final @NotNull ByteBuffer encode() {
        return data.write(value());
    }

    @ApiStatus.Internal
    public final void dispatch(@NotNull PacketReceiver receiver, @NotNull ByteBuffer encoded) {
        D decoded = data.read(encoded);
        Deque<D> values = receivedValues.get();
        values.push(decoded);
        try {
            handle(receiverType.cast(receiver));
        } finally {
            values.pop();
            if (values.isEmpty()) {
                receivedValues.remove();
            }
        }
    }

}
