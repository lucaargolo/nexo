package dev.lucaargolo.nexo.feature.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.render.*;
import dev.lucaargolo.nexo.api.feature.screen.widget.Widget;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.input.GlfwKeyConversions;
import dev.lucaargolo.nexo.render.DynamicMinecraftGraphics2D;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class MinecraftScreen extends ScreenBase {

    private static final Map<Location, ScreenBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final Map<Location, Screen> SCREEN_MAP = new ConcurrentHashMap<>();

    private static final Map<Location, Holder<MenuType<?>>> MENU_HOLDER_MAP = new ConcurrentHashMap<>();

    public static final Bijection<ScreenBase, Screen> CONVERT = new Bijection<>() {
        @Override
        public Screen forward(ScreenBase screen) {
            return SCREEN_MAP.get(screen.location());
        }

        @Override
        public ScreenBase backward(Screen screen) {
            //TODO
            return null;//FEATURE_MAP.get();
        }
    };

    private final @NotNull Screen screen;

    private MinecraftScreen(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Screen screen) {
        super(MinecraftScreen.location(nexo, screen), MinecraftRoleType.uncraft(nexo, Type.SCREEN, screen));
        this.screen = screen;
    }

    public @NotNull Screen screen() {
        return this.screen;
    }

    @Override
    public void build(float width, float height) {
        //TODO
    }

    @Override
    public void render(@NotNull Graphics2D graphics, @NotNull Vector2f mouse) {
        //TODO
    }

    @Override
    public @NotNull Map<String, Material<?>> materials() {
        //TODO
        return Map.of();
    }

    @Override
    public @NotNull List<@NotNull Widget> widgets() {
        //TODO
        return List.of();
    }

    @Override
    public void addWidget(@NotNull Widget widget) {
        //TODO
    }

    @Override
    public void removeWidget(@NotNull Widget widget) {
        //TODO
    }

    @Override
    public boolean inputPressed(@NotNull Input input) {
        //TODO
        return false;
    }

    @Override
    public boolean inputReleased(@NotNull Input input) {
        //TODO
        return false;
    }

    @Override
    public boolean inputMove(@NotNull Input.Axis axis, float delta) {
        //TODO
        return false;
    }

    public static @Nullable ScreenBase lookup(@NotNull Location location) {
        return FEATURE_MAP.get(location);
    }

    public static @NotNull ScreenBase register(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase screen) {
        ScreenBase registered = FEATURE_MAP.get(screen.location());
        if (registered != null) {
            return registered;
        }
        FEATURE_MAP.put(screen.location(), screen);
        SCREEN_MAP.put(screen.location(), MinecraftFeatureType.SCREEN.craft(nexo, screen).get());
        if (MinecraftScreen.isDynamicScreen(screen)) {
            AtomicReference<MenuType<AbstractContainerMenu>> reference = new AtomicReference<>();
            MenuType<AbstractContainerMenu> type = nexo.getRegistryHandler().craftMenuType((containerId, inventory, player) -> new AbstractContainerMenu(reference.get(), containerId) {
                @Override
                public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slot) {
                    return ItemStack.EMPTY;
                }

                @Override
                public boolean stillValid(@NotNull Player player) {
                    return player == inventory.player;
                }
            });
            reference.set(type);
            Holder<MenuType<?>> holder = nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.MENU, NexoMinecraft.rl(screen.location()), reference::get);
            MENU_HOLDER_MAP.put(screen.location(), holder);
        }
        return screen;
    }

    public static @NotNull ScreenBase index(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Screen screen) {
        Location location = MinecraftScreen.location(nexo, screen);
        SCREEN_MAP.put(location, screen);
        return FEATURE_MAP.computeIfAbsent(location, key -> new MinecraftScreen(nexo, screen));
    }

    public static @NotNull <M extends Screen> Screen craft(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Utils.Extender<M> extender, @Nullable Function<Component, M> factory, @NotNull ScreenBase screen) {
        extender.override(Utils.At.AFTER_SUPER, "init", void.class, instance -> {
            screen.build(instance.width, instance.height);
            return null;
        });
        extender.override(Utils.At.AFTER_SUPER, "render", void.class, GuiGraphics.class, int.class, int.class, float.class, (instance, graphics, mouseX, mouseY, partialTick) -> {
            DynamicMinecraftGraphics2D g = new DynamicMinecraftGraphics2D(nexo, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            try {
                //TODO: Optimize and maybe get full value instead of integer?
                screen.render(g, new Vector2f(mouseX, mouseY));
            } catch (Throwable t) {
                NexoMinecraft.LOGGER.error("Failed to render Nexo screen {}", screen.location(), t);
                throw t;
            } finally {
                g.finish();
            }
            return null;
        });
        extender.override(Utils.At.AFTER_SUPER, "keyPressed", boolean.class, int.class, int.class, int.class, (instance, keyCode, scanCode, modifiers) -> {
            return screen.inputPressed(Input.keyboard(GlfwKeyConversions.key(keyCode)));
        });
        extender.override(Utils.At.AFTER_SUPER, "keyReleased", boolean.class, int.class, int.class, int.class, (instance, keyCode, scanCode, modifiers) -> {
            return screen.inputReleased(Input.keyboard(GlfwKeyConversions.key(keyCode)));
        });
        extender.override(Utils.At.AFTER_SUPER, "mouseClicked", boolean.class, double.class, double.class, int.class, (instance, mouseX, mouseY, button) -> {
            return screen.inputPressed(Input.mouse(GlfwKeyConversions.mouse(button)));
        });
        extender.override(Utils.At.AFTER_SUPER, "mouseReleased", boolean.class, double.class, double.class, int.class, (instance, mouseX, mouseY, button) -> {
            return screen.inputReleased(Input.keyboard(GlfwKeyConversions.mouse(button)));
        });
        extender.override(Utils.At.AFTER_SUPER, "mouseDragged", boolean.class, double.class, double.class, int.class, double.class, double.class, (instance, mouseX, mouseY, button, dragX, dragY) -> {
            if(screen.inputMove(Input.Axis.MOUSE_X, dragX.floatValue())) {
                return true;
            }
            return screen.inputMove(Input.Axis.MOUSE_Y, dragY.floatValue());
        });
        extender.override(Utils.At.AFTER_SUPER, "mouseScrolled", boolean.class, double.class, double.class, double.class, double.class, (instance, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
            return screen.inputMove(Input.Axis.SCROLL, verticalAmount.floatValue());
        });
        extender.override(Utils.At.AFTER_SUPER, "mouseMoved", void.class, double.class, double.class, (instance, mouseX, mouseY) -> {
            Vector2f previous = new Vector2f(); //TODO: Actually collect previous
            if (previous.x >= 0.0 && previous.y >= 0.0) {
                if(!screen.inputMove(Input.Axis.MOUSE_X, (float) (mouseX - previous.x))) {
                    screen.inputMove(Input.Axis.MOUSE_Y, (float) (mouseY - previous.y));
                }
            }
            return null;
        });
        //TODO: Properly get the title
        Component title = Component.empty();
        if (factory != null) {
            return factory.apply(title);
        }
        return extender.instantiate(title);
    }

    public static @NotNull MenuType<?> menu(@NotNull ScreenBase feature) {
        Holder<MenuType<?>> holder = MENU_HOLDER_MAP.get(feature.location());
        if (holder == null) {
            throw new IllegalStateException("No menu registered for server screen " + feature.location());
        }
        return holder.value();
    }

    private static Location location(NexoMinecraft<?, ?, ?, ?> nexo, Screen screen) {
        //TODO
        return Location.of("minecraft", "screen");
    }

    public static boolean isDynamicScreen(@NotNull ScreenBase base) {
        //TODO
        return base.role() != null;
    }

}
