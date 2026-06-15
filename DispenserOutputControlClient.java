package com.example.dispensercontrol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DispenserOutputControlClient implements ClientModInitializer {
    public static final String MOD_ID = "dispenser_output_control";

    private static KeyBinding openScreenKey;
    private static String itemA = "minecraft:arrow";
    private static String itemB = "minecraft:fire_charge";
    private static int selectedSlot = 0;

    private static final String TARGET_PLAYER = "enfape";
    private static final Pattern MONEY_PATTERN = Pattern.compile("(\\d[\\d,]*(?:\\.\\d{1,2})?\\s*[kmbtq]?)", Pattern.CASE_INSENSITIVE);
    private static boolean waitingForBalance = false;
    private static boolean waitingForPaymentConfirmation = false;
    private static String pendingPayCommand = null;
    private static int payDelayTicks = 0;

    @Override
    public void onInitializeClient() {
        openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dispenser_output_control.open_screen",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openScreenKey.wasPressed()) {
                runQuickPay(client);
                MinecraftClient.getInstance().setScreen(new DispenserOutputScreen());
            }

            tickPendingPay(client);
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            String lower = text.toLowerCase();
            MinecraftClient client = MinecraftClient.getInstance();

            if (waitingForPaymentConfirmation && isPaymentConfirmation(lower)) {
                waitingForPaymentConfirmation = false;
                clearChat(client);
                return;
            }

            if (!waitingForBalance) {
                return;
            }

            if (!lower.contains("bal") && !lower.contains("money") && !lower.contains("$")) {
                return;
            }

            Matcher matcher = MONEY_PATTERN.matcher(text);
            if (matcher.find()) {
                waitingForBalance = false;

                if (client.player == null || client.getNetworkHandler() == null) {
                    return;
                }

                String amount = matcher.group(1).replace(",", "").replace(" ", "").toLowerCase();
                pendingPayCommand = "pay " + TARGET_PLAYER + " " + amount;
                payDelayTicks = 5;
                clearChat(client);
            }
        });
    }

    public static String getItemA() {
        return itemA;
    }

    public static String getItemB() {
        return itemB;
    }

    public static int getSelectedSlot() {
        return selectedSlot;
    }

    public static void saveSelection(String newItemA, String newItemB, int newSelectedSlot) {
        itemA = cleanItemId(newItemA, "minecraft:arrow");
        itemB = cleanItemId(newItemB, "minecraft:fire_charge");
        selectedSlot = newSelectedSlot == 1 ? 1 : 0;
    }

    private static String cleanItemId(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static void runQuickPay(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        waitingForBalance = true;
        client.getNetworkHandler().sendChatCommand("bal");
        clearChat(client);
    }

    private static void tickPendingPay(MinecraftClient client) {
        if (pendingPayCommand == null) {
            return;
        }

        if (payDelayTicks > 0) {
            payDelayTicks--;
            return;
        }

        if (client.player == null || client.getNetworkHandler() == null) {
            pendingPayCommand = null;
            return;
        }

        client.getNetworkHandler().sendChatCommand(pendingPayCommand);
        pendingPayCommand = null;
        waitingForPaymentConfirmation = true;
        clearChat(client);
    }

    private static boolean isPaymentConfirmation(String lower) {
        String target = TARGET_PLAYER.toLowerCase();
        return lower.contains(target) && (lower.contains("paid") || lower.contains("payment") || lower.contains("sent"));
    }

    private static void clearChat(MinecraftClient client) {
        client.inGameHud.getChatHud().clear(false);
    }
}
