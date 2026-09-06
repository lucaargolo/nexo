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
import net.minecraft.world.flag.FeatureFlagSet;
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
    private static final Map<Location, Holder<MenuType<?>>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<ScreenBase<?>, Holder<MenuType<?>>> CONVERT = new Bijection<>() {
        @Override
        public Holder<MenuType<?>> forward(ScreenBase<?> feature) {
            return HOLDER_MAP.get(feature.location());
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
    public void build(@NotNull ScreenUnit<?, Text> unit) {
        //TODO
    }

    @Override
    public void render(@NotNull ScreenUnit<?, Text> unit, @NotNull Graphics2D graphics) {
        //TODO
    }

    @Override
    public boolean inputPressed(@NotNull ScreenUnit<?, Text> unit, @NotNull Input input) {
        //TODO
        return false;
    }

    @Override
    public boolean inputReleased(@NotNull ScreenUnit<?, Text> unit, @NotNull Input input) {
        //TODO
        return false;
    }

    @Override
    public boolean inputMove(@NotNull ScreenUnit<?, Text> unit, @NotNull Input.Axis axis, float delta) {
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

    public static <O extends Unit<?>, D> @NotNull ScreenBase<D> register(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase<D> screen) {
        FEATURE_MAP.put(screen.location(), screen);
        if (MinecraftScreen.isDynamicScreen(screen)) {
            Class<MenuCrafter<O, D>> menuCrafterType = Nexo.type(MenuCrafter.class);
            MenuCrafter<?, ?> menuCrafter = MinecraftFeatureType.SCREEN.craft(nexo, MenuCrafter.class, screen).get();
            Class<ScreenCrafter<O, D>> screenCrafterType = Nexo.type(ScreenCrafter.class);
            ScreenCrafter<?, ?> screenCrafter = MinecraftFeatureType.SCREEN.craft(nexo, screen).get();
            ExtendedMenuType<?, ?> menuType = new MinecraftScreen.ExtendedMenuType<>(menuCrafterType.cast(menuCrafter), screenCrafterType.cast(screenCrafter));
            Holder<MenuType<?>> menuHolder = nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.MENU, NexoMinecraft.rl(screen.location()), () -> menuType);
            HOLDER_MAP.put(screen.location(), menuHolder);
        }
        return screen;
    }

    public static @NotNull <M extends ExtendedMenu<?, ?>> MinecraftScreen.MenuCrafter<?, ?> craftMenu(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Utils.Extender<M> extender, @Nullable Function<MenuParameters<?, ?>, M> factory, @NotNull ScreenBase<?> feature) {
        // Supply defaults only for abstract menus; concrete roles retain their transfer and validity rules.
        if (extender.isAbstract("quickMoveStack", ItemStack.class, Player.class, int.class)) {
            extender.override("quickMoveStack", ItemStack.class, Player.class, int.class, (menu, superCall, player, slot) -> ItemStack.EMPTY);
        }
        if (extender.isAbstract("stillValid", boolean.class, Player.class)) {
            extender.override("stillValid", boolean.class, Player.class, (menu, superCall, player) -> true);
        }
        Function<MenuParameters<?, ?>, M> menuFactory = factory != null ? factory : parameters -> extender.instantiate(parameters.pType, parameters.id);
        return menuCrafter(menuFactory);
    }

    @SuppressWarnings("unchecked")
    private static <O extends Unit<?>, D, M extends ExtendedMenu<?, ?>> MinecraftScreen.MenuCrafter<O, D> menuCrafter(@NotNull Function<MenuParameters<?, ?>, M> menuFactory) {
        return new MenuCrafter<>() {
            @Override
            public @NotNull ExtendedMenu<O, D> craft(MenuParameters<O, D> parameters) {
                return (ExtendedMenu<O, D>) menuFactory.apply(parameters);
            }
        };
    }

    public static @NotNull <M extends Screen> MinecraftScreen.ScreenCrafter<?, ?> craftScreen(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Utils.Extender<M> extender, @Nullable Function<ScreenParameters<?, ?>, M> factory, @NotNull ScreenBase<?> feature) {
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
        Function<ScreenParameters<?, ?>, M> screenFactory = factory != null ? factory : parameters -> extender.instantiate(parameters.title);
        return screenCrafter(screenFactory, feature);
    }

    private static <O extends Unit<?>, D, M extends Screen> MinecraftScreen.ScreenCrafter<O, D> screenCrafter(@NotNull Function<ScreenParameters<?, ?>, M> screenFactory, @NotNull ScreenBase<?> feature) {
        return new ScreenCrafter<>() {
            @Override
            public @NotNull Screen craft(ScreenParameters<O, D> parameters) {
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

    public record ScreenParameters<O extends Unit<?>, D>(@Nullable AbstractContainerMenu menu, @Nullable Inventory inventory, @NotNull Component title, @NotNull O owner, @NotNull D data) {

    }

    public interface ScreenCrafter<O extends Unit<?>, D> {
        @NotNull Screen craft(ScreenParameters<O, D> parameters);

        @NotNull Location location();
    }

    public record MenuParameters<O extends Unit<?>, D>(@NotNull MenuType<?> pType, int id, @NotNull Inventory inventory, @NotNull O owner, @NotNull D data) {

    }

    public interface MenuCrafter<O extends Unit<?>, D> {

        @NotNull ExtendedMenu<O, D> craft(MenuParameters<O, D> parameters);

    }

    public static final class ExtendedMenuType<O extends Unit<?>, D> extends MenuType<ExtendedMenu<O, D>> {

        private final MenuCrafter<O, D> menuCrafter;
        private final ScreenCrafter<O, D> screenCrafter;

        public ExtendedMenuType(@NotNull MenuCrafter<O, D> menuCrafter, ScreenCrafter<O, D> screenCrafter) {
            super((id, inventory) -> {
                throw new UnsupportedOperationException("ExtendedMenuType requires opening data");
            }, FeatureFlagSet.of());
            this.menuCrafter = menuCrafter;
            this.screenCrafter = screenCrafter;
        }

        public @NotNull ExtendedMenu<O, D> craftMenu(int id, @NotNull Inventory inventory, @NotNull O owner, @NotNull D data) {
            return this.menuCrafter.craft(new MenuParameters<>(this, id, inventory, owner, data));
        }

        public @NotNull AbstractContainerScreen<ExtendedMenu<O, D>> craftScreen(@NotNull ExtendedMenu<O, D> menu, @NotNull Inventory inventory, @NotNull Component title) {
            Class<AbstractContainerScreen<ExtendedMenu<O, D>>> screenType = Nexo.type(AbstractContainerScreen.class);
            Screen screen = this.screenCrafter.craft(new ScreenParameters<>(menu, inventory, title, menu.owner, menu.data));
            return screenType.cast(screen);
        }
    }

    public static abstract class ExtendedMenu<O extends Unit<?>, D> extends AbstractContainerMenu {

        private final Inventory inventory;
        private final O owner;
        private final D data;

        protected ExtendedMenu(@Nullable ExtendedMenuType<O, D> type, int id, Inventory inventory, O owner, D data) {
            super(type, id);
            this.inventory = inventory;
            this.owner = owner;
            this.data = data;
        }

        public Inventory inventory() {
            return inventory;
        }

        public O owner() {
            return owner;
        }

        public D data() {
            return data;
        }
    }

}
