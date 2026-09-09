package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.NeoForgeItemHandlerVault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class NeoForgeMinecraftItemUnit extends MinecraftItemUnit {

    public NeoForgeMinecraftItemUnit(@NotNull NexoMinecraft nexo, @NotNull ItemBase feature, @Nullable Role role, @NotNull ItemStack stack) {
        super(nexo, feature, role, stack);
    }

    @Override
    public @NotNull <U extends Unit<?>> Set<String> vaults(@NotNull Class<U> type) {
        if(type == ItemUnit.class) {
            if (this.itemHandler() != null) {
                return Set.of("inventory");
            }
        }
        return Set.of();
    }

    @Override
    public @Nullable <U extends Unit<?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        if(type == ItemUnit.class) {
            if (key.equals("inventory")) {
                return NeoForgeItemHandlerVault.create(this.nexo, type, this.itemHandler());
            }
        }
        return null;
    }

    private @Nullable IItemHandler itemHandler() {
        return Capabilities.ItemHandler.ITEM.getCapability(this.get(), null);
    }

}
