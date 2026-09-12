package dev.lemomax.treephysics;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class TreePhysics implements ModInitializer {
    private static final int MAX_LOGS = 256;
    private static final int MAX_DISTANCE = 24;

    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.AFTER.register(TreePhysics::onLogBroken);
    }

    private static void onLogBroken(
            World world,
            PlayerEntity player,
            BlockPos brokenPos,
            BlockState brokenState,
            net.minecraft.block.entity.BlockEntity blockEntity
    ) {
        if (world.isClient() || !brokenState.isIn(BlockTags.LOGS)) {
            return;
        }

        ServerWorld serverWorld = (ServerWorld) world;
        Set<BlockPos> logs = collectConnectedLogs(serverWorld, brokenPos);

        // A single isolated log is not treated as a tree.
        if (logs.size() < 2) {
            return;
        }

        Vec3d push = calculateFallDirection(player, brokenPos);

        for (BlockPos pos : logs) {
            if (pos.equals(brokenPos)) {
                continue;
            }

            BlockState state = serverWorld.getBlockState(pos);

            if (!state.isIn(BlockTags.LOGS)) {
                continue;
            }

            serverWorld.setBlockState(
                    pos,
                    net.minecraft.block.Blocks.AIR.getDefaultState(),
                    3
            );

            FallingBlockEntity falling =
                    FallingBlockEntity.spawnFromBlock(serverWorld, pos, state);

            falling.setVelocity(push);
        }
    }

    private static Set<BlockPos> collectConnectedLogs(
            ServerWorld world,
            BlockPos start
    ) {
        Set<BlockPos> result = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        queue.add(start.toImmutable());

        int minX = start.getX() - MAX_DISTANCE;
        int maxX = start.getX() + MAX_DISTANCE;
        int minY = Math.max(
                world.getBottomY(),
                start.getY() - MAX_DISTANCE
        );
        int maxY = Math.min(
                world.getTopYInclusive(),
                start.getY() + MAX_DISTANCE
        );
        int minZ = start.getZ() - MAX_DISTANCE;
        int maxZ = start.getZ() + MAX_DISTANCE;

        while (!queue.isEmpty() && result.size() < MAX_LOGS) {
            BlockPos pos = queue.removeFirst();

            if (result.contains(pos)) {
                continue;
            }

            if (pos.getX() < minX
                    || pos.getX() > maxX
                    || pos.getY() < minY
                    || pos.getY() > maxY
                    || pos.getZ() < minZ
                    || pos.getZ() > maxZ) {
                continue;
            }

            if (!world.getBlockState(pos).isIn(BlockTags.LOGS)
                    && !pos.equals(start)) {
                continue;
            }

            result.add(pos);

            for (Direction direction : Direction.values()) {
                BlockPos next = pos.offset(direction);

                if (!result.contains(next)
                        && world.getBlockState(next).isIn(BlockTags.LOGS)) {
                    queue.addLast(next.toImmutable());
                }
            }
        }

        return result;
    }

    private static Vec3d calculateFallDirection(
            PlayerEntity player,
            BlockPos brokenPos
    ) {
        Vec3d look = new Vec3d(
                brokenPos.getX() + 0.5,
                brokenPos.getY() + 0.5,
                brokenPos.getZ() + 0.5
        ).subtract(player.getEntityPos());

        Vec3d horizontal = new Vec3d(
                look.x,
                0.0,
                look.z
        );

        if (horizontal.lengthSquared() < 0.0001) {
            return Vec3d.ZERO;
        }

        horizontal = horizontal.normalize().multiply(0.06);

        return new Vec3d(
                horizontal.x,
                -0.02,
                horizontal.z
        );
    }
}
