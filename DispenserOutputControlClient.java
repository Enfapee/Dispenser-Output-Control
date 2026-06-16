package com.example.dispensercontrol;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
                MinecraftClient.getInstance().setScreen(new DispenserOutputScreen());
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
}
