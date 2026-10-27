package dev.superpowers;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;

@Mod(SuperPowers.MOD_ID)
public class SuperPowers {
    public static final String MOD_ID = "superpowers";
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public SuperPowers() {
        CHANNEL.registerMessage(0, AbilityPacket.class,
                AbilityPacket::encode, AbilityPacket::decode, AbilityPacket::handle);
        MinecraftForge.EVENT_BUS.addListener(this::onLogout);
    }

    private void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AbilityHandler.clear(event.getPlayer().getUUID());
    }
}
