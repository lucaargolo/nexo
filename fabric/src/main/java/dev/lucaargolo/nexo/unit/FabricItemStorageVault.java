package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import org.jetbrains.annotations.NotNull;

public final class FabricItemStorageVault implements Vault<ItemUnit> {

    private final @NotNull NexoMinecraft nexo;
    private final @NotNull ItemUnit empty;
    private final @NotNull Storage<ItemVariant> storage;

    public FabricItemStorageVault(@NotNull NexoMinecraft nexo, @NotNull Storage<ItemVariant> storage) {
        this.nexo = nexo;
        this.empty = MinecraftItemUnit.empty(nexo);
        this.storage = storage;
    }

}
