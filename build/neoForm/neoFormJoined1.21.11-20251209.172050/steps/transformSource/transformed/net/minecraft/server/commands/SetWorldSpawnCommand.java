package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec2;

public class SetWorldSpawnCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("setworldspawn")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(
                    p_450800_ -> setSpawn(p_450800_.getSource(), BlockPos.containing(p_450800_.getSource().getPosition()), WorldCoordinates.ZERO_ROTATION)
                )
                .then(
                    Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(
                            p_450801_ -> setSpawn(p_450801_.getSource(), BlockPosArgument.getSpawnablePos(p_450801_, "pos"), WorldCoordinates.ZERO_ROTATION)
                        )
                        .then(
                            Commands.argument("rotation", RotationArgument.rotation())
                                .executes(
                                    p_450799_ -> setSpawn(
                                        p_450799_.getSource(),
                                        BlockPosArgument.getSpawnablePos(p_450799_, "pos"),
                                        RotationArgument.getRotation(p_450799_, "rotation")
                                    )
                                )
                        )
                )
        );
    }

    private static int setSpawn(CommandSourceStack source, BlockPos pos, Coordinates rotation) {
        ServerLevel serverlevel = source.getLevel();
        Vec2 vec2 = rotation.getRotation(source);
        float f = vec2.y;
        float f1 = vec2.x;
        LevelData.RespawnData leveldata$respawndata = LevelData.RespawnData.of(serverlevel.dimension(), pos, f, f1);
        serverlevel.setRespawnData(leveldata$respawndata);
        source.sendSuccess(
            () -> Component.translatable(
                "commands.setworldspawn.success",
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                leveldata$respawndata.yaw(),
                leveldata$respawndata.pitch(),
                serverlevel.dimension().identifier().toString()
            ),
            true
        );
        return 1;
    }
}
