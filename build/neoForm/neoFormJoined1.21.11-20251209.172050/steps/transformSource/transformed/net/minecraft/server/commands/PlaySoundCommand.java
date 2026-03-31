package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PlaySoundCommand {
    private static final SimpleCommandExceptionType ERROR_TOO_FAR = new SimpleCommandExceptionType(Component.translatable("commands.playsound.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, Identifier> requiredargumentbuilder = Commands.argument("sound", IdentifierArgument.id())
            .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
            .executes(
                p_466272_ -> playSound(
                    p_466272_.getSource(),
                    getCallingPlayerAsCollection(p_466272_.getSource().getPlayer()),
                    IdentifierArgument.getId(p_466272_, "sound"),
                    SoundSource.MASTER,
                    p_466272_.getSource().getPosition(),
                    1.0F,
                    1.0F,
                    0.0F
                )
            );

        for (SoundSource soundsource : SoundSource.values()) {
            requiredargumentbuilder.then(source(soundsource));
        }

        dispatcher.register(Commands.literal("playsound").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(requiredargumentbuilder));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> source(SoundSource category) {
        return Commands.literal(category.getName())
            .executes(
                p_466261_ -> playSound(
                    p_466261_.getSource(),
                    getCallingPlayerAsCollection(p_466261_.getSource().getPlayer()),
                    IdentifierArgument.getId(p_466261_, "sound"),
                    category,
                    p_466261_.getSource().getPosition(),
                    1.0F,
                    1.0F,
                    0.0F
                )
            )
            .then(
                Commands.argument("targets", EntityArgument.players())
                    .executes(
                        p_466263_ -> playSound(
                            p_466263_.getSource(),
                            EntityArgument.getPlayers(p_466263_, "targets"),
                            IdentifierArgument.getId(p_466263_, "sound"),
                            category,
                            p_466263_.getSource().getPosition(),
                            1.0F,
                            1.0F,
                            0.0F
                        )
                    )
                    .then(
                        Commands.argument("pos", Vec3Argument.vec3())
                            .executes(
                                p_466265_ -> playSound(
                                    p_466265_.getSource(),
                                    EntityArgument.getPlayers(p_466265_, "targets"),
                                    IdentifierArgument.getId(p_466265_, "sound"),
                                    category,
                                    Vec3Argument.getVec3(p_466265_, "pos"),
                                    1.0F,
                                    1.0F,
                                    0.0F
                                )
                            )
                            .then(
                                Commands.argument("volume", FloatArgumentType.floatArg(0.0F))
                                    .executes(
                                        p_466274_ -> playSound(
                                            p_466274_.getSource(),
                                            EntityArgument.getPlayers(p_466274_, "targets"),
                                            IdentifierArgument.getId(p_466274_, "sound"),
                                            category,
                                            Vec3Argument.getVec3(p_466274_, "pos"),
                                            p_466274_.getArgument("volume", Float.class),
                                            1.0F,
                                            0.0F
                                        )
                                    )
                                    .then(
                                        Commands.argument("pitch", FloatArgumentType.floatArg(0.0F, 2.0F))
                                            .executes(
                                                p_466259_ -> playSound(
                                                    p_466259_.getSource(),
                                                    EntityArgument.getPlayers(p_466259_, "targets"),
                                                    IdentifierArgument.getId(p_466259_, "sound"),
                                                    category,
                                                    Vec3Argument.getVec3(p_466259_, "pos"),
                                                    p_466259_.getArgument("volume", Float.class),
                                                    p_466259_.getArgument("pitch", Float.class),
                                                    0.0F
                                                )
                                            )
                                            .then(
                                                Commands.argument("minVolume", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                    .executes(
                                                        p_466269_ -> playSound(
                                                            p_466269_.getSource(),
                                                            EntityArgument.getPlayers(p_466269_, "targets"),
                                                            IdentifierArgument.getId(p_466269_, "sound"),
                                                            category,
                                                            Vec3Argument.getVec3(p_466269_, "pos"),
                                                            p_466269_.getArgument("volume", Float.class),
                                                            p_466269_.getArgument("pitch", Float.class),
                                                            p_466269_.getArgument("minVolume", Float.class)
                                                        )
                                                    )
                                            )
                                    )
                            )
                    )
            );
    }

    private static Collection<ServerPlayer> getCallingPlayerAsCollection(@Nullable ServerPlayer player) {
        return player != null ? List.of(player) : List.of();
    }

    private static int playSound(
        CommandSourceStack source,
        Collection<ServerPlayer> targets,
        Identifier sound,
        SoundSource category,
        Vec3 pos,
        float volume,
        float pitch,
        float minVolume
    ) throws CommandSyntaxException {
        Holder<SoundEvent> holder = Holder.direct(SoundEvent.createVariableRangeEvent(sound));
        double d0 = Mth.square(holder.value().getRange(volume));
        ServerLevel serverlevel = source.getLevel();
        long i = serverlevel.getRandom().nextLong();
        List<ServerPlayer> list = new ArrayList<>();

        for (ServerPlayer serverplayer : targets) {
            if (serverplayer.level() == serverlevel) {
                double d1 = pos.x - serverplayer.getX();
                double d2 = pos.y - serverplayer.getY();
                double d3 = pos.z - serverplayer.getZ();
                double d4 = d1 * d1 + d2 * d2 + d3 * d3;
                Vec3 vec3 = pos;
                float f = volume;
                if (d4 > d0) {
                    if (minVolume <= 0.0F) {
                        continue;
                    }

                    double d5 = Math.sqrt(d4);
                    vec3 = new Vec3(serverplayer.getX() + d1 / d5 * 2.0, serverplayer.getY() + d2 / d5 * 2.0, serverplayer.getZ() + d3 / d5 * 2.0);
                    f = minVolume;
                }

                serverplayer.connection.send(new ClientboundSoundPacket(holder, category, vec3.x(), vec3.y(), vec3.z(), f, pitch, i));
                list.add(serverplayer);
            }
        }

        int j = list.size();
        if (j == 0) {
            throw ERROR_TOO_FAR.create();
        } else {
            if (j == 1) {
                source.sendSuccess(
                    () -> Component.translatable("commands.playsound.success.single", Component.translationArg(sound), list.getFirst().getDisplayName()),
                    true
                );
            } else {
                source.sendSuccess(() -> Component.translatable("commands.playsound.success.multiple", Component.translationArg(sound), j), true);
            }

            return j;
        }
    }
}
