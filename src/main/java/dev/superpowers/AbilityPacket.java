package dev.superpowers;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import java.util.function.Supplier;

public class AbilityPacket {
    public final int id;

    public AbilityPacket(int id) {
        this.id = id;
    }

    public static void encode(AbilityPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.id);
    }

    public static AbilityPacket decode(FriendlyByteBuf buf) {
        return new AbilityPacket(buf.readVarInt());
    }

    public static void handle(AbilityPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = c.getSender();
            if (player != null) AbilityHandler.use(player, msg.id);
        });
        c.setPacketHandled(true);
    }
}
