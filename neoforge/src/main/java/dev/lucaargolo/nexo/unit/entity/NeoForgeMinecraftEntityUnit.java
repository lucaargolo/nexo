package dev.lucaargolo.nexo.unit.entity;

import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.unit.MinecraftContainerVault;
import dev.lucaargolo.nexo.unit.NeoForgeItemHandlerVault;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class NeoForgeMinecraftEntityUnit<C extends Role, E extends Entity> extends MinecraftEntityUnit<NeoForgeNexoMinecraft, C, E> {

    public NeoForgeMinecraftEntityUnit(@NotNull NeoForgeNexoMinecraft nexo, @NotNull EntityBase feature, @Nullable C role, @NotNull E entity) {
        super(nexo, feature, role, entity);
    }

    @Override
    public @NotNull <U extends Unit<?, ?>> Set<String> vaults(@NotNull Class<U> type) {
        if (!MinecraftContainerVault.supports(type)) {
            return super.vaults(type);
        }
        Set<String> vaults = new HashSet<>(super.vaults(type));
        if (this.itemHandler() != null) {
            vaults.add(MinecraftContainerVault.KEY);
        }
        return Set.copyOf(vaults);
    }

    @Override
    public @Nullable <U extends Unit<?, ?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        if (!MinecraftContainerVault.KEY.equals(key) || !MinecraftContainerVault.supports(type)) {
            return super.vault(type, key);
        }
        IItemHandler handler = this.itemHandler();
        if (handler != null) {
            Class<Vault<U>> clazz = Nexo.type(Vault.class);
            return clazz.cast(new NeoForgeItemHandlerVault(this.nexo, handler));
        }
        return super.vault(type, key);
    }

    private @Nullable IItemHandler itemHandler() {
        return Capabilities.ItemHandler.ENTITY.getCapability(this.entity, null);
    }

    @Override
    public @NotNull Side side() {
        return entity.level().isClientSide() ? Side.CLIENT : Side.SERVER;
    }

    @Override
    public @Nullable <D> D getData(@NotNull DataBase<D> data) {
        AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
        return this.feature.data().contains(data) ? this.entity.getData(type) : this.entity.getExistingDataOrNull(type);
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
        AttachmentType<D> type = nexo.getRegistryHandler().getDataAttachment(data);
        if (d == null) {
            this.entity.removeData(type);
        } else {
            this.entity.setData(type, d);
        }
    }


}
