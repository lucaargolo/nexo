package dev.lucaargolo.nexo.event;

import dev.lucaargolo.nexo.util.DynamicRegistryView;
import net.neoforged.bus.api.Event;

public class DynamicRegistrySetupEvent extends Event {

    private final DynamicRegistryView view;

    public DynamicRegistrySetupEvent(DynamicRegistryView view) {
        this.view = view;
    }

    public DynamicRegistryView view() {
        return view;
    }

}
