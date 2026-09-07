package dev.lucaargolo.nexo.feature.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.data.TextData;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Text;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.input.GlfwKeyConversions;
import dev.lucaargolo.nexo.render.DynamicMinecraftGraphics2D;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
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

    public static final Map<Location, ScreenCrafter<?>> CRAFTER_MAP = new ConcurrentHashMap<>();
    public static final Map<Location, Holder<MenuType<?>>> MENU_HOLDER_MAP = new ConcurrentHashMap<>();

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
        ScreenCrafter<?> screenCrafter = MinecraftFeatureType.SCREEN.craft(nexo, screen).get();
        FEATURE_MAP.put(screen.location(), screen);
        CRAFTER_MAP.put(screen.location(), screenCrafter);
        if (MinecraftScreen.isDynamicScreen(screen)) {
            Class<ScreenCrafter<D>> screenCrafterType = Nexo.type(ScreenCrafter.class);
            Class<MenuCrafter<D>> menuCrafterType = Nexo.type(MenuCrafter.class);
            MenuCrafter<?> menuCrafter = MinecraftFeatureType.SCREEN.craft(nexo, MenuCrafter.class, screen).get();
            MenuType<?> menuType = nexo.getRegistryHandler().craftMenuType(screen, menuCrafterType.cast(menuCrafter), screenCrafterType.cast(screenCrafter)).type();
            Holder<MenuType<?>> menuHolder = nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.MENU, NexoMinecraft.rl(screen.location()), () -> menuType);
            MENU_HOLDER_MAP.put(screen.location(), menuHolder);
        }
        return screen;
    }

    public static @NotNull <M extends ExtendedMenu<?>> MinecraftScreen.MenuCrafter<?> craftMenu(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Utils.Extender<M> extender, @Nullable Function<MenuParameters<?>, M> factory, @NotNull ScreenBase<?> feature) {
        // Supply defaults only for abstract menus; concrete roles retain their transfer and validity rules.
        if (extender.isAbstract("quickMoveStack", ItemStack.class, Player.class, int.class)) {
            extender.override("quickMoveStack", ItemStack.class, Player.class, int.class, (menu, superCall, player, slot) -> ItemStack.EMPTY);
        }
        if (extender.isAbstract("stillValid", boolean.class, Player.class)) {
            extender.override("stillValid", boolean.class, Player.class, (menu, superCall, player) -> true);
        }
        Function<MenuParameters<?>, M> menuFactory = factory != null ? factory : extender::instantiate;
        return menuCrafter(menuFactory);
    }

    private static <M extends ExtendedMenu<?>> MinecraftScreen.MenuCrafter<?> menuCrafter(@NotNull Function<MenuParameters<?>, M> menuFactory) {
        return parameters -> (ExtendedMenu<Object>) menuFactory.apply(parameters);
    }

    public static @NotNull <M extends Screen> MinecraftScreen.ScreenCrafter<?> craftScreen(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Utils.Extender<M> extender, @Nullable Function<ScreenParameters<?>, M> factory, @NotNull ScreenBase<?> feature) {
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
        return screenCrafter(screenFactory, feature);
    }

    private static <D, M extends Screen> MinecraftScreen.ScreenCrafter<D> screenCrafter(@NotNull Function<ScreenParameters<?>, M> screenFactory, @NotNull ScreenBase<D> feature) {
        return screenFactory::apply;
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

    public record ScreenParameters<D>(@Nullable AbstractContainerMenu menu, @Nullable Inventory inventory, @NotNull Component title, @NotNull D data) {

    }

    public interface ScreenCrafter<D> {
        @NotNull Screen craft(ScreenParameters<D> parameters);
    }

    public record MenuParameters<D>(@NotNull MenuType<?> type, int id, @NotNull Inventory inventory, @NotNull D data, @Nullable Unit<?> owner) {

    }

    public interface MenuCrafter<D> {

        @NotNull ExtendedMenu<D> craft(MenuParameters<D> parameters);

    }

    public interface ExtendedMenuType<D> {

        @NotNull MenuType<?> type();

        @NotNull ExtendedMenu<D> craftMenu(int id, @NotNull Inventory inventory, @NotNull D data, @Nullable Unit<?> owner);

        @NotNull AbstractContainerScreen<ExtendedMenu<D>> craftScreen(@NotNull ExtendedMenu<D> menu, @NotNull Inventory inventory, @NotNull Component title);

    }

    public static abstract class ExtendedMenu<D> extends AbstractContainerMenu {

        @NotNull private final Inventory inventory;
        @NotNull private final D data;
        @Nullable private final Unit<?> owner;

        protected ExtendedMenu(@NotNull MenuType<?> type, int id, @NotNull Inventory inventory, @NotNull D data, @Nullable Unit<?> owner) {
            super(type, id);
            this.inventory = inventory;
            this.owner = owner;
            this.data = data;
        }

        protected ExtendedMenu(@NotNull MenuParameters<D> parameters) {
            this(parameters.type, parameters.id, parameters.inventory, parameters.data, parameters.owner);
        }

        public Inventory inventory() {
            return inventory;
        }

        public Unit<?> owner() {
            return owner;
        }

        public D data() {
            return data;
        }
        
    }

}
