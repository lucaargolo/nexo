package dev.lucaargolo.nexo.feature.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.TextData;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Text;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.input.GlfwKeyConversions;
import dev.lucaargolo.nexo.render.DynamicMinecraftGraphics2D;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class MinecraftScreen extends ScreenBase<Text> {

    private static final Map<Location, ScreenBase<?>> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final Map<Location, ScreenCrafter> CRAFTER_MAP = new ConcurrentHashMap<>();

    private static final Map<Location, Holder<MenuType<?>>> MENU_HOLDER_MAP = new ConcurrentHashMap<>();

    public static final Bijection<ScreenBase<?>, ScreenCrafter> CONVERT = new Bijection<>() {
        @Override
        public ScreenCrafter forward(ScreenBase<?> screen) {
            return CRAFTER_MAP.get(screen.location());
        }

        @Override
        public ScreenBase<?> backward(ScreenCrafter crafter) {
            return FEATURE_MAP.get(crafter.location());
        }
    };

    public static Bijection<ScreenBase<?>, Holder<MenuType<?>>> CONVERT_MENU = new Bijection<>() {
        @Override
        public Holder<MenuType<?>> forward(ScreenBase<?> feature) {
            return MENU_HOLDER_MAP.get(feature.location());
        }

        @Override
        public ScreenBase<?> backward(Holder<MenuType<?>> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    private final @NotNull Screen screen;

    private MinecraftScreen(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Screen screen) {
        super(MinecraftRoleType.uncraft(nexo, Type.SCREEN, screen), TextData.TEXT);
        this.identify(nexo, nexo.getRegistryHandler().identity(location(nexo, screen)));
        this.screen = screen;
    }

    public @NotNull Screen screen() {
        return this.screen;
    }

    @Override
    public void build(@NotNull ScreenUnit<Text> unit) {
        //TODO
    }

    @Override
    public void render(@NotNull ScreenUnit<Text> unit, @NotNull Graphics2D graphics) {
        //TODO
    }

    @Override
    public boolean inputPressed(@NotNull ScreenUnit<Text> unit, @NotNull Input input) {
        //TODO
        return false;
    }

    @Override
    public boolean inputReleased(@NotNull ScreenUnit<Text> unit, @NotNull Input input) {
        //TODO
        return false;
    }

    @Override
    public boolean inputMove(@NotNull ScreenUnit<Text> unit, @NotNull Input.Axis axis, float delta) {
        //TODO
        return false;
    }

    @Override
    public @NotNull Map<String, Material<?>> materials() {
        //TODO
        return Map.of();
    }

    public static @Nullable ScreenBase<?> lookup(@NotNull Location location) {
        return FEATURE_MAP.get(location);
    }

    public static <D> @NotNull ScreenBase<D> register(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase<D> screen) {
        FEATURE_MAP.put(screen.location(), screen);
        ScreenCrafter screenCrafter = MinecraftFeatureType.SCREEN.craft(nexo, screen).get();
        CRAFTER_MAP.put(screen.location(), screenCrafter);
        if (MinecraftScreen.isDynamicScreen(screen)) {
            MenuCrafter<D> menuCrafter = MinecraftFeatureType.SCREEN.craft(nexo, MenuCrafter.class, screen).get();
            MenuType<AbstractContainerMenu> type = nexo.getRegistryHandler().craftMenuType(menuCrafter, screen.data());
            Holder<MenuType<?>> holder = nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.MENU, NexoMinecraft.rl(screen.location()), () -> type);
            MENU_HOLDER_MAP.put(screen.location(), holder);
        }
        return screen;
    }

    public static @NotNull <D, M extends AbstractContainerMenu> MinecraftScreen.MenuCrafter<D> craftMenu(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Utils.Extender<M> extender, @Nullable Function<MenuParameters<D>, M> factory, @NotNull ScreenBase<?> feature) {
        // Supply defaults only for abstract menus; concrete roles retain their transfer and validity rules.
        if (extender.isAbstract("quickMoveStack", ItemStack.class, Player.class, int.class)) {
            extender.override("quickMoveStack", ItemStack.class, Player.class, int.class, (menu, superCall, player, slot) -> ItemStack.EMPTY);
        }
        if (extender.isAbstract("stillValid", boolean.class, Player.class)) {
            extender.override("stillValid", boolean.class, Player.class, (menu, superCall, player) -> true);
        }
        Function<MenuParameters<D>, M> menuFactory = factory != null ? factory : parameters -> extender.instantiate(parameters.pType, parameters.id);
        return menuFactory::apply;
    }

    public static @NotNull <M extends Screen> MinecraftScreen.ScreenCrafter craftScreen(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Utils.Extender<M> extender, @Nullable Function<ScreenParameters<?>, M> factory, @NotNull ScreenBase<?> feature) {
        extender.override("init", void.class, (screen, superCall) -> {
            superCall.apply(screen);
            nexo.screenToUnit(screen, feature).build();
            return null;
        });
        extender.override("render", void.class, GuiGraphics.class, int.class, int.class, float.class, (screen, superCall, graphics, mouseX, mouseY, partialTick) -> {
            superCall.apply(screen, graphics, mouseX, mouseY, partialTick);
            DynamicMinecraftGraphics2D g = new DynamicMinecraftGraphics2D(nexo, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            try {
                nexo.screenToUnit(screen, feature).mouse().set(mouseX, mouseY);
                nexo.screenToUnit(screen, feature).render(g);
            } catch (Throwable t) {
                NexoMinecraft.LOGGER.error("Failed to render Nexo screen {}", feature.location(), t);
                throw t;
            } finally {
                g.finish();
            }
            return null;
        });
        extender.override("keyPressed", boolean.class, int.class, int.class, int.class, (screen, superCall, keyCode, scanCode, modifiers) -> {
            boolean handled = superCall.apply(screen, keyCode, scanCode, modifiers);
            return nexo.screenToUnit(screen, feature).inputPressed(Input.keyboard(GlfwKeyConversions.key(keyCode))) || handled;
        });
        extender.override("keyReleased", boolean.class, int.class, int.class, int.class, (screen, superCall, keyCode, scanCode, modifiers) -> {
            boolean handled = superCall.apply(screen, keyCode, scanCode, modifiers);
            return nexo.screenToUnit(screen, feature).inputReleased(Input.keyboard(GlfwKeyConversions.key(keyCode))) || handled;
        });
        extender.override("mouseClicked", boolean.class, double.class, double.class, int.class, (screen, superCall, mouseX, mouseY, button) -> {
            boolean handled = superCall.apply(screen, mouseX, mouseY, button);
            return nexo.screenToUnit(screen, feature).inputPressed(Input.mouse(GlfwKeyConversions.mouse(button))) || handled;
        });
        extender.override("mouseReleased", boolean.class, double.class, double.class, int.class, (screen, superCall, mouseX, mouseY, button) -> {
            boolean handled = superCall.apply(screen, mouseX, mouseY, button);
            return nexo.screenToUnit(screen, feature).inputReleased(Input.mouse(GlfwKeyConversions.mouse(button))) || handled;
        });
        extender.override("mouseDragged", boolean.class, double.class, double.class, int.class, double.class, double.class, (screen, superCall, mouseX, mouseY, button, dragX, dragY) -> {
            boolean handled = superCall.apply(screen, mouseX, mouseY, button, dragX, dragY);
            return nexo.screenToUnit(screen, feature).handleMouseDragged(mouseX, mouseY, dragX, dragY) || handled;
        });
        extender.override("mouseScrolled", boolean.class, double.class, double.class, double.class, double.class, (screen, superCall, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
            boolean handled = superCall.apply(screen, mouseX, mouseY, horizontalAmount, verticalAmount);
            return nexo.screenToUnit(screen, feature).inputMove(Input.Axis.SCROLL, verticalAmount.floatValue()) || handled;
        });
        extender.override("mouseMoved", void.class, double.class, double.class, (screen, superCall, mouseX, mouseY) -> {
            superCall.apply(screen, mouseX, mouseY);
            nexo.screenToUnit(screen, feature).handleMouseMoved(mouseX, mouseY);
            return null;
        });
        Function<ScreenParameters<?>, M> screenFactory = factory != null ? factory : parameters -> extender.instantiate(parameters.title);
        return new ScreenCrafter() {
            @Override
            public @NotNull Screen craft(ScreenParameters<?> parameters) {
                return screenFactory.apply(parameters);
            }

            @Override
            public @NotNull Location location() {
                return feature.location();
            }
        };
    }

    private static Location location(NexoMinecraft<?, ?, ?, ?> nexo, Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> container) {
            return NexoMinecraft.id(BuiltInRegistries.MENU.getKey(container.getMenu().getType()));
        }
        //TODO: Use the mod namespace for modded screens.
        String name = screen.getClass().getSimpleName()
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
        return Location.of("minecraft", name);
    }

    public static boolean isDynamicScreen(@NotNull ScreenBase<?> base) {
        //TODO
        return base.role() != null;
    }

    public record ScreenParameters<T extends AbstractContainerMenu>(@Nullable T menu, @Nullable Inventory inventory, @NotNull Component title) {

    }

    public interface ScreenCrafter {
        @NotNull Screen craft(ScreenParameters<?> parameters);

        @NotNull Location location();
    }

    public record MenuParameters<D>(@NotNull MenuType<?> pType, int id, @NotNull D data) {

    }

    public interface MenuCrafter<D> {

        @NotNull AbstractContainerMenu craft(MenuParameters<D> parameters);

    }

}
