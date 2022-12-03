package io.d2a.dab;

import io.d2a.dab.util.BlockUtils;
import io.d2a.dab.util.EveryWith;
import io.d2a.dab.util.RunnableWith;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

public class SugarCaneDab {

    private final LinkedHashSet<BlockPos> breakers;

    private final EveryWith<ClientWorld> collectTask;
    private final EveryWith<ClientWorld> breakTask;

    public SugarCaneDab() {
        this.breakers = new LinkedHashSet<>();

        this.collectTask = new EveryWith<>(3, new CollectTask());
        this.breakTask = new EveryWith<>(1, new BreakTask());

        ClientTickEvents.END_WORLD_TICK.register(world -> {
            SugarCaneDab.this.collectTask.tick(world);
            SugarCaneDab.this.breakTask.tick(world);
        });
    }

    private class CollectTask implements RunnableWith<ClientWorld> {
        @Override
        public void run(@NotNull ClientWorld clientWorld) {
            final ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) {
                return;
            }

            final ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
            if (interactionManager == null) {
                return;
            }

            final double radius = interactionManager.getReachDistance();
            final double radiusSq = radius * radius;

            final int yOffset = player.hasVehicle() ? 1 : 0;

            final BlockPos standingPos = player.getBlockPos();
            final List<BlockPos> check = BlockUtils.getAllInBox(
                    standingPos.add(radius, yOffset, radius),
                    standingPos.add(-radius, 0, -radius)
            );
            for (final BlockPos pos : check) {
                // check if in range
                if (pos.getSquaredDistance(standingPos) > radiusSq) {
                    continue;
                }
                if (!clientWorld.getBlockState(pos).isOf(Blocks.SUGAR_CANE)) {
                    continue;
                }
                // get most bottom sugar cane
                BlockPos bottom = pos;
                while (clientWorld.getBlockState(bottom.down(1)).isOf(Blocks.SUGAR_CANE)) {
                    bottom = bottom.down(1);
                }

                // ignore if no sugar cane above bottom
                final BlockPos breakingBlock = bottom.up(1);
                if (!clientWorld.getBlockState(breakingBlock).isOf(Blocks.SUGAR_CANE)) {
                    continue;
                }

                // check if bottom block in range
                if (bottom.getSquaredDistance(standingPos) > radiusSq) {
                    continue;
                }

                // mark as harvestable
                breakers.add(breakingBlock);
            }
        }
    }

    private class BreakTask implements RunnableWith<ClientWorld> {
        @Override
        public void run(@NotNull ClientWorld clientWorld) {
            final MinecraftClient minecraft = MinecraftClient.getInstance();
            final ClientPlayerInteractionManager interactionManager = minecraft.interactionManager;
            if (interactionManager == null) {
                breakers.clear();
                return;
            }

            if (interactionManager.isBreakingBlock()) {
                return;
            }

            // poll next breaking block
            final Iterator<BlockPos> it = breakers.iterator();
            if (!it.hasNext()) {
                return;
            }
            final BlockPos pos = it.next();
            it.remove();

            // try to break block at {pos}
            if (interactionManager.attackBlock(pos, Direction.EAST)) {
                minecraft.particleManager.addBlockBreakingParticles(pos, Direction.EAST);
                if (minecraft.player != null) {
                    minecraft.player.swingHand(Hand.MAIN_HAND);
                }
            }
        }
    }

}
