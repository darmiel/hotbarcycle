package io.d2a.dab.util;


import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

public class BlockUtils {

    public static ArrayList<BlockPos> getAllInBox(final BlockPos from, final BlockPos to) {
        final ArrayList<BlockPos> blocks = new ArrayList<>();

        final BlockPos min = new BlockPos(
                Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ())
        );
        final BlockPos max = new BlockPos(
                Math.max(from.getX(), to.getX()),
                Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ())
        );

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    blocks.add(new BlockPos(x, y, z));
                }
            }
        }

        return blocks;
    }

}