package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.NeoForgeItemHandlerVault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeoForgeMinecraftItemUnit extends MinecraftItemUnit {

    public NeoForgeMinecraftItemUnit(@NotNull NeoForgeNexoMinecraft nexo, @NotNull ItemBase feature, @Nullable Role role, @NotNull ItemStack stack) {
        super(nexo, feature, role, stack);
    }

    @Override
    protected @Nullable Vault<ItemUnit> itemVault() {
        IItemHandler handler = Capabilities.ItemHandler.ITEM.getCapability(this.get(), null);
        return handler == null ? null : new NeoForgeItemHandlerVault(this.nexo, handler);
    }

}
