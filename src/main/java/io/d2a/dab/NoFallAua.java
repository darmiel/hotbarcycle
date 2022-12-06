package io.d2a.dab;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFallAua {

    public NoFallAua() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            final ClientPlayerEntity player = client.player;
            if (player == null) {
                return;
            }
            // reset fall damage every 10 blocks
            if (player.fallDistance > player.getSafeFallDistance()) {
                if (player.hasVehicle()) {
                    return;
                }

                final ClientPlayNetworkHandler net = client.getNetworkHandler();
                if (net == null) {
                    return;
                }

                double x = player.getX(),
                        y = player.getY() + .1,
                        z = player.getZ();

                player.setPos(x, y, z);
                player.fallDistance = 0;

                net.sendPacket( new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, player.isOnGround()) );
            }
        });
    }

}
