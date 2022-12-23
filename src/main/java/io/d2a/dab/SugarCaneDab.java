package io.d2a.dab;

import com.github.nyuppo.HotbarCycleClient;
import io.d2a.dab.util.BlockUtils;
import io.d2a.dab.util.EveryWith;
import io.d2a.dab.util.RunnableWith;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
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

        private BlockPos getBottomBlock(final Block type, final ClientWorld world, final BlockPos start) {
            if (!world.getBlockState(start).isOf(type)) {
                return null;
            }
            // get most bottom sugar cane
            BlockPos bottom = start;
            while (world.getBlockState(bottom.down(1)).isOf(type)) {
                bottom = bottom.down(1);
            }

            // ignore if no sugar cane above bottom
            final BlockPos breakingBlock = bottom.up(1);
            if (!world.getBlockState(breakingBlock).isOf(type)) {
                return null;
            }
            return breakingBlock;
        }

        private BlockPos getBottomBlockOfAny(final ClientWorld world, final BlockPos start, final Block... types) {
            for (final Block type : types) {
                final BlockPos pos = this.getBottomBlock(type, world, start);
                if (pos != null) {
                    return pos;
                }
            }
            return null;
        }

        @Override
        public void run(@NotNull ClientWorld clientWorld) {
            final long pollStart = System.currentTimeMillis();
            final ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) {
                return;
            }
            if (!HotbarCycleClient.getConfig().isAutoFarm()) {
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
                final BlockPos breakingBlock = this.getBottomBlockOfAny(
                        clientWorld, pos, Blocks.SUGAR_CANE, Blocks.BAMBOO
                );
                if (breakingBlock == null) {
                    continue;
                }
                // check distance
                if (breakingBlock.getSquaredDistance(standingPos) > radiusSq) {
                    continue;
                }
                // mark as harvestable
                breakers.add(breakingBlock);
            }
            final long dur = System.currentTimeMillis() - pollStart;
            System.out.println("[Collect] Collection took " + dur + "ms");
        }
    }

    private class BreakTask implements RunnableWith<ClientWorld> {
        @Override
        public void run(@NotNull ClientWorld clientWorld) {
            final long pollStart = System.currentTimeMillis();
            final MinecraftClient minecraft = MinecraftClient.getInstance();
            final ClientPlayerInteractionManager interactionManager = minecraft.interactionManager;
            if (interactionManager == null) {
                breakers.clear();
                return;
            }
            if (minecraft.player == null || interactionManager.isBreakingBlock()) {
                return;
            }

            // poll next breaking block
            final Iterator<BlockPos> it = breakers.iterator();
            if (!it.hasNext()) {
                return;
            }
            final BlockPos pos = it.next();
            it.remove();

            // check if player can break block instantly
            if (clientWorld.getBlockState(pos).calcBlockBreakingDelta(minecraft.player, clientWorld, pos) < 1.0) {
                return;
            }

            // try to break block at {pos}
            if (interactionManager.attackBlock(pos, Direction.EAST)) {
                minecraft.player.swingHand(Hand.MAIN_HAND);
            }
            final long dur = System.currentTimeMillis() - pollStart;
            System.out.println("[Break] Breaking took " + dur + "ms");
        }
    }

}
