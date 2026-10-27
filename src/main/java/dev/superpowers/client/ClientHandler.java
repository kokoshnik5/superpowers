package dev.superpowers.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.superpowers.Ability;
import dev.superpowers.AbilityPacket;
import dev.superpowers.SuperPowers;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fmlclient.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class ClientHandler {
    private static final String CATEGORY = "key.categories.superpowers";
    // Same order as the Ability enum: DASH, PUNCH, SLAM, FLIGHT
    private static final int[] DEFAULT_KEYS = {GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B};
    private static final KeyMapping[] KEYS = new KeyMapping[Ability.values().length];

    private ClientHandler() {}

    @Mod.EventBusSubscriber(modid = SuperPowers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            Ability[] abilities = Ability.values();
            for (int i = 0; i < abilities.length; i++) {
                KEYS[i] = new KeyMapping(
                        "key.superpowers." + abilities[i].name().toLowerCase(Locale.ROOT),
                        InputConstants.Type.KEYSYM,
                        DEFAULT_KEYS[i],
                        CATEGORY);
                ClientRegistry.registerKeyBinding(KEYS[i]);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = SuperPowers.MOD_ID, value = Dist.CLIENT)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onKey(InputEvent.KeyInputEvent event) {
            if (Minecraft.getInstance().player == null) return;
            for (int i = 0; i < KEYS.length; i++) {
                while (KEYS[i] != null && KEYS[i].consumeClick()) {
                    SuperPowers.CHANNEL.sendToServer(new AbilityPacket(i));
                }
            }
        }
    }
}
