package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

public class StopSoundCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> requiredargumentbuilder = Commands.argument("targets", EntityArgument.players())
            .executes(p_466298_ -> stopSound(p_466298_.getSource(), EntityArgument.getPlayers(p_466298_, "targets"), null, null))
            .then(
                Commands.literal("*")
                    .then(
                        Commands.argument("sound", IdentifierArgument.id())
                            .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                            .executes(
                                p_466297_ -> stopSound(
                                    p_466297_.getSource(), EntityArgument.getPlayers(p_466297_, "targets"), null, IdentifierArgument.getId(p_466297_, "sound")
                                )
                            )
                    )
            );

        for (SoundSource soundsource : SoundSource.values()) {
            requiredargumentbuilder.then(
                Commands.literal(soundsource.getName())
                    .executes(p_466305_ -> stopSound(p_466305_.getSource(), EntityArgument.getPlayers(p_466305_, "targets"), soundsource, null))
                    .then(
                        Commands.argument("sound", IdentifierArgument.id())
                            .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                            .executes(
                                p_466303_ -> stopSound(
                                    p_466303_.getSource(),
                                    EntityArgument.getPlayers(p_466303_, "targets"),
                                    soundsource,
                                    IdentifierArgument.getId(p_466303_, "sound")
                                )
                            )
                    )
            );
        }

        dispatcher.register(Commands.literal("stopsound").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(requiredargumentbuilder));
    }

    private static int stopSound(
        CommandSourceStack source, Collection<ServerPlayer> targets, @Nullable SoundSource category, @Nullable Identifier sound
    ) {
        ClientboundStopSoundPacket clientboundstopsoundpacket = new ClientboundStopSoundPacket(sound, category);

        for (ServerPlayer serverplayer : targets) {
            serverplayer.connection.send(clientboundstopsoundpacket);
        }

        if (category != null) {
            if (sound != null) {
                source.sendSuccess(
                    () -> Component.translatable("commands.stopsound.success.source.sound", Component.translationArg(sound), category.getName()), true
                );
            } else {
                source.sendSuccess(() -> Component.translatable("commands.stopsound.success.source.any", category.getName()), true);
            }
        } else if (sound != null) {
            source.sendSuccess(() -> Component.translatable("commands.stopsound.success.sourceless.sound", Component.translationArg(sound)), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.stopsound.success.sourceless.any"), true);
        }

        return targets.size();
    }
}
