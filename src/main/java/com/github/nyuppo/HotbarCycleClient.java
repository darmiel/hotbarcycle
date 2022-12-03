package com.github.nyuppo;

import com.github.nyuppo.compat.Clicker;
import com.github.nyuppo.compat.IPNClicker;
import com.github.nyuppo.compat.VanillaClicker;
import com.github.nyuppo.config.ClothConfigHotbarCycleConfig;
import com.github.nyuppo.config.DefaultHotbarCycleConfig;
import com.github.nyuppo.config.HotbarCycleConfig;
import io.d2a.dab.SugarCaneDab;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class HotbarCycleClient implements ClientModInitializer {
    private static KeyBinding cycleKeyBinding;
    private static KeyBinding singleCycleKeyBinding;

    private static final HotbarCycleConfig CONFIG;

    private static Clicker clicker;

    public static final Logger LOGGER = LoggerFactory.getLogger("hotbarcycle");

    public static HotbarCycleConfig getConfig() {
        return CONFIG;
    }

    public static KeyBinding getCycleKeyBinding() {
        return cycleKeyBinding;
    }

    public static KeyBinding getSingleCycleKeyBinding() {
        return singleCycleKeyBinding;
    }

    @Override
    public void onInitializeClient() {
        clicker = getClicker();

        cycleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hotbarcycle.cycle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.hotbarcycle.keybinds"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (cycleKeyBinding.wasPressed()) {
                if (client.player != null && !CONFIG.getHoldAndScroll()) {
                    shiftRows(client, Direction.DOWN);
                }
            }
        });

        final KeyBinding travelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hotbarcycle.travel",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.hotbarcycle.keybinds"
        ));

        final AtomicInteger tick = new AtomicInteger();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (travelKey.wasPressed()) {
                final ClientPlayerEntity player = client.player;
                if (player == null) {
                    return;
                }
                final ClientPlayNetworkHandler net = client.getNetworkHandler();
                if (net == null) {
                    return;
                }
                final double multiplier = 8.5;
                final double distance = !player.isSneaking() ? 100 : 400;

                if (client.getCameraEntity() == null) {
                    return;
                }

                final int howManyTeleports = (int) Math.ceil(distance / multiplier);
                for (int i = 0; i < howManyTeleports; i++) {
                    if (tick.incrementAndGet() % 4 == 0) {
                        System.out.println("  Sleeping...");
                        try {
                            Thread.sleep((long) Math.ceil((1.0 / 20.0) * 1000));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    System.out.println("Teleport " + (i + 1) + "/" + howManyTeleports);
                    double x = player.getX(), y = player.getY(), z = player.getZ();
                    switch (client.getCameraEntity().getHorizontalFacing()) {
                        case NORTH -> z -= multiplier;
                        case EAST -> x += multiplier;
                        case SOUTH -> z += multiplier;
                        case WEST -> x -= multiplier;
                    }

                    net.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                            x, y, z, player.isOnGround()
                    ));
                    player.setPos(x, y, z);
                }
            }
        });

        final KeyBinding elytraPitchKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hotbarcycle.elytapitch",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                "category.hotbarcycle.keybinds"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (elytraPitchKeyBinding.wasPressed()) {
                final ClientPlayerEntity player = client.player;
                if (player == null) {
                    return;
                }
                final ClientPlayNetworkHandler net = client.getNetworkHandler();
                if (net == null) {
                    return;
                }

                float pitch = MathHelper.wrapDegrees(player.getPitch());
                if (pitch < 0) {
                    pitch = 45;
                } else {
                    pitch = -45;
                }

                // set pitch
                player.setPitch(pitch);
                net.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(player.getYaw(), pitch, player.isOnGround()));
            }
        });


        singleCycleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hotbarcycle.single_cycle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.hotbarcycle.keybinds"
        ));

        // Dab Start
        new SugarCaneDab();
        // Dab End

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (singleCycleKeyBinding.wasPressed()) {
                if (client.player != null && client.player.getInventory() != null && !CONFIG.getHoldAndScroll()) {
                    shiftSingle(client, client.player.getInventory().selectedSlot, Direction.DOWN);
                }
            }
        });
    }

    public enum Direction {
        UP,
        DOWN;

        public Direction reverse(final boolean reversed) {
            return switch (this) {
                case UP -> !reversed ? UP : DOWN;
                case DOWN -> !reversed ? DOWN : UP;
            };
        }
    }

    public static void shiftRows(MinecraftClient client, final Direction requestedDirection) {
        // invert direction if reverse cycle is enabled
        final Direction direction = requestedDirection.reverse(CONFIG.getReverseCycleDirection());

        @SuppressWarnings("resource")
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (interactionManager == null || client.player == null) {
            return;
        }

        int i;
        if (direction != Direction.DOWN ? CONFIG.getEnableRow1() : CONFIG.getEnableRow3()) {
            for (i = 0; i < 9; i++) {
                if (isColumnEnabled(i)) {
                    clicker.swap(client, (direction == Direction.DOWN ? 9 : 27) + i, i);
                }
            }
        }

        if (CONFIG.getEnableRow2()) {
            for (i = 0; i < 9; i++) {
                if (isColumnEnabled(i)) {
                    clicker.swap(client, 18 + i, i);
                }
            }
        }

        if (direction != Direction.DOWN ? CONFIG.getEnableRow3() : CONFIG.getEnableRow1()) {
            for (i = 0; i < 9; i++) {
                if (isColumnEnabled(i)) {
                    clicker.swap(client, (direction == Direction.DOWN ? 27 : 9) + i, i);
                }
            }
        }

        if (CONFIG.getPlaySound()) {
            client.player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER, 0.5f, 1.5f);
        }
    }

    public static void shiftSingle(MinecraftClient client, int hotbarSlot, final Direction requestedDirection) {
        // invert direction if reverse cycle is enabled
        final Direction direction = requestedDirection.reverse(CONFIG.getReverseCycleDirection());

        @SuppressWarnings("resource")
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (interactionManager == null || client.player == null) {
            return;
        }

        if (direction == Direction.DOWN ? CONFIG.getEnableRow1() : CONFIG.getEnableRow3()) {
            clicker.swap(client, (direction != Direction.DOWN ? 9 : 27) + hotbarSlot, hotbarSlot);
        }

        if (CONFIG.getEnableRow2()) {
            clicker.swap(client, 18 + hotbarSlot, hotbarSlot);
        }

        if (direction == Direction.DOWN ? CONFIG.getEnableRow3() : CONFIG.getEnableRow1()) {
            clicker.swap(client, (direction != Direction.DOWN ? 27 : 9) + hotbarSlot, hotbarSlot);
        }

        if (CONFIG.getPlaySound()) {
            client.player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER, 0.5f, 1.8f);
        }
    }

    private static Clicker getClicker() {
        if (FabricLoader.getInstance().isModLoaded("inventoryprofilesnext")) {
            LOGGER.info("Inventory Profiles Next was found, switching to compatible clicker!");
            return new IPNClicker();
        }

        return new VanillaClicker();
    }

    private static boolean isColumnEnabled(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> CONFIG.getEnableColumn0();
            case 1 -> CONFIG.getEnableColumn1();
            case 2 -> CONFIG.getEnableColumn2();
            case 3 -> CONFIG.getEnableColumn3();
            case 4 -> CONFIG.getEnableColumn4();
            case 5 -> CONFIG.getEnableColumn5();
            case 6 -> CONFIG.getEnableColumn6();
            case 7 -> CONFIG.getEnableColumn7();
            case 8 -> CONFIG.getEnableColumn8();
            default -> false;
        };
    }

    static {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            CONFIG = AutoConfig.register(ClothConfigHotbarCycleConfig.class, GsonConfigSerializer::new).getConfig();
        } else {
            CONFIG = new DefaultHotbarCycleConfig();
        }

    }
}
