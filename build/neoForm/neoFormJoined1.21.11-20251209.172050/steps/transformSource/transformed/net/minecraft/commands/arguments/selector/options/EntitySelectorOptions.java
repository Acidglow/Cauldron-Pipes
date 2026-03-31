package net.minecraft.commands.arguments.selector.options;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;

public class EntitySelectorOptions {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, EntitySelectorOptions.Option> OPTIONS = Maps.newHashMap();
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION = new DynamicCommandExceptionType(
        p_304141_ -> Component.translatableEscape("argument.entity.options.unknown", p_304141_)
    );
    public static final DynamicCommandExceptionType ERROR_INAPPLICABLE_OPTION = new DynamicCommandExceptionType(
        p_304138_ -> Component.translatableEscape("argument.entity.options.inapplicable", p_304138_)
    );
    public static final SimpleCommandExceptionType ERROR_RANGE_NEGATIVE = new SimpleCommandExceptionType(
        Component.translatable("argument.entity.options.distance.negative")
    );
    public static final SimpleCommandExceptionType ERROR_LEVEL_NEGATIVE = new SimpleCommandExceptionType(
        Component.translatable("argument.entity.options.level.negative")
    );
    public static final SimpleCommandExceptionType ERROR_LIMIT_TOO_SMALL = new SimpleCommandExceptionType(
        Component.translatable("argument.entity.options.limit.toosmall")
    );
    public static final DynamicCommandExceptionType ERROR_SORT_UNKNOWN = new DynamicCommandExceptionType(
        p_304139_ -> Component.translatableEscape("argument.entity.options.sort.irreversible", p_304139_)
    );
    public static final DynamicCommandExceptionType ERROR_GAME_MODE_INVALID = new DynamicCommandExceptionType(
        p_304140_ -> Component.translatableEscape("argument.entity.options.mode.invalid", p_304140_)
    );
    public static final DynamicCommandExceptionType ERROR_ENTITY_TYPE_INVALID = new DynamicCommandExceptionType(
        p_304137_ -> Component.translatableEscape("argument.entity.options.type.invalid", p_304137_)
    );

    public static void register(String id, EntitySelectorOptions.Modifier handler, Predicate<EntitySelectorParser> predicate, Component tooltip) {
        OPTIONS.put(id, new EntitySelectorOptions.Option(handler, predicate, tooltip));
    }

    public static void bootStrap() {
        if (OPTIONS.isEmpty()) {
            register("name", p_360473_ -> {
                int i = p_360473_.getReader().getCursor();
                boolean flag = p_360473_.shouldInvertValue();
                String s = p_360473_.getReader().readString();
                if (p_360473_.hasNameNotEquals() && !flag) {
                    p_360473_.getReader().setCursor(i);
                    throw ERROR_INAPPLICABLE_OPTION.createWithContext(p_360473_.getReader(), "name");
                } else {
                    if (flag) {
                        p_360473_.setHasNameNotEquals(true);
                    } else {
                        p_360473_.setHasNameEquals(true);
                    }

                    p_360473_.addPredicate(p_465884_ -> p_465884_.getPlainTextName().equals(s) != flag);
                }
            }, p_121423_ -> !p_121423_.hasNameEquals(), Component.translatable("argument.entity.options.name.description"));
            register(
                "distance",
                p_465870_ -> {
                    int i = p_465870_.getReader().getCursor();
                    MinMaxBounds.Doubles minmaxbounds$doubles = MinMaxBounds.Doubles.fromReader(p_465870_.getReader());
                    if ((!minmaxbounds$doubles.min().isPresent() || !((Double)minmaxbounds$doubles.min().get() < 0.0))
                        && (!minmaxbounds$doubles.max().isPresent() || !((Double)minmaxbounds$doubles.max().get() < 0.0))) {
                        p_465870_.setDistance(minmaxbounds$doubles);
                        p_465870_.setWorldLimited();
                    } else {
                        p_465870_.getReader().setCursor(i);
                        throw ERROR_RANGE_NEGATIVE.createWithContext(p_465870_.getReader());
                    }
                },
                p_465873_ -> p_465873_.getDistance() == null,
                Component.translatable("argument.entity.options.distance.description")
            );
            register(
                "level",
                p_465874_ -> {
                    int i = p_465874_.getReader().getCursor();
                    MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromReader(p_465874_.getReader());
                    if ((!minmaxbounds$ints.min().isPresent() || (Integer)minmaxbounds$ints.min().get() >= 0)
                        && (!minmaxbounds$ints.max().isPresent() || (Integer)minmaxbounds$ints.max().get() >= 0)) {
                        p_465874_.setLevel(minmaxbounds$ints);
                        p_465874_.setIncludesEntities(false);
                    } else {
                        p_465874_.getReader().setCursor(i);
                        throw ERROR_LEVEL_NEGATIVE.createWithContext(p_465874_.getReader());
                    }
                },
                p_465887_ -> p_465887_.getLevel() == null,
                Component.translatable("argument.entity.options.level.description")
            );
            register("x", p_121413_ -> {
                p_121413_.setWorldLimited();
                p_121413_.setX(p_121413_.getReader().readDouble());
            }, p_121411_ -> p_121411_.getX() == null, Component.translatable("argument.entity.options.x.description"));
            register("y", p_121409_ -> {
                p_121409_.setWorldLimited();
                p_121409_.setY(p_121409_.getReader().readDouble());
            }, p_121407_ -> p_121407_.getY() == null, Component.translatable("argument.entity.options.y.description"));
            register("z", p_121405_ -> {
                p_121405_.setWorldLimited();
                p_121405_.setZ(p_121405_.getReader().readDouble());
            }, p_121403_ -> p_121403_.getZ() == null, Component.translatable("argument.entity.options.z.description"));
            register("dx", p_121401_ -> {
                p_121401_.setWorldLimited();
                p_121401_.setDeltaX(p_121401_.getReader().readDouble());
            }, p_121399_ -> p_121399_.getDeltaX() == null, Component.translatable("argument.entity.options.dx.description"));
            register("dy", p_121397_ -> {
                p_121397_.setWorldLimited();
                p_121397_.setDeltaY(p_121397_.getReader().readDouble());
            }, p_121395_ -> p_121395_.getDeltaY() == null, Component.translatable("argument.entity.options.dy.description"));
            register("dz", p_121562_ -> {
                p_121562_.setWorldLimited();
                p_121562_.setDeltaZ(p_121562_.getReader().readDouble());
            }, p_121560_ -> p_121560_.getDeltaZ() == null, Component.translatable("argument.entity.options.dz.description"));
            register(
                "x_rotation",
                p_465891_ -> p_465891_.setRotX(MinMaxBounds.FloatDegrees.fromReader(p_465891_.getReader())),
                p_465881_ -> p_465881_.getRotX() == null,
                Component.translatable("argument.entity.options.x_rotation.description")
            );
            register(
                "y_rotation",
                p_465892_ -> p_465892_.setRotY(MinMaxBounds.FloatDegrees.fromReader(p_465892_.getReader())),
                p_465888_ -> p_465888_.getRotY() == null,
                Component.translatable("argument.entity.options.y_rotation.description")
            );
            register("limit", p_121550_ -> {
                int i = p_121550_.getReader().getCursor();
                int j = p_121550_.getReader().readInt();
                if (j < 1) {
                    p_121550_.getReader().setCursor(i);
                    throw ERROR_LIMIT_TOO_SMALL.createWithContext(p_121550_.getReader());
                } else {
                    p_121550_.setMaxResults(j);
                    p_121550_.setLimited(true);
                }
            }, p_121548_ -> !p_121548_.isCurrentEntity() && !p_121548_.isLimited(), Component.translatable("argument.entity.options.limit.description"));
            register(
                "sort",
                p_247983_ -> {
                    int i = p_247983_.getReader().getCursor();
                    String s = p_247983_.getReader().readUnquotedString();
                    p_247983_.setSuggestions(
                        (p_175153_, p_175154_) -> SharedSuggestionProvider.suggest(Arrays.asList("nearest", "furthest", "random", "arbitrary"), p_175153_)
                    );

                    p_247983_.setOrder(switch (s) {
                        case "nearest" -> EntitySelectorParser.ORDER_NEAREST;
                        case "furthest" -> EntitySelectorParser.ORDER_FURTHEST;
                        case "random" -> EntitySelectorParser.ORDER_RANDOM;
                        case "arbitrary" -> EntitySelector.ORDER_ARBITRARY;
                        default -> {
                            p_247983_.getReader().setCursor(i);
                            throw ERROR_SORT_UNKNOWN.createWithContext(p_247983_.getReader(), s);
                        }
                    });
                    p_247983_.setSorted(true);
                },
                p_121544_ -> !p_121544_.isCurrentEntity() && !p_121544_.isSorted(),
                Component.translatable("argument.entity.options.sort.description")
            );
            register("gamemode", p_412865_ -> {
                p_412865_.setSuggestions((p_175193_, p_175194_) -> {
                    String s1 = p_175193_.getRemaining().toLowerCase(Locale.ROOT);
                    boolean flag1 = !p_412865_.hasGamemodeNotEquals();
                    boolean flag2 = true;
                    if (!s1.isEmpty()) {
                        if (s1.charAt(0) == '!') {
                            flag1 = false;
                            s1 = s1.substring(1);
                        } else {
                            flag2 = false;
                        }
                    }

                    for (GameType gametype1 : GameType.values()) {
                        if (gametype1.getName().toLowerCase(Locale.ROOT).startsWith(s1)) {
                            if (flag2) {
                                p_175193_.suggest("!" + gametype1.getName());
                            }

                            if (flag1) {
                                p_175193_.suggest(gametype1.getName());
                            }
                        }
                    }

                    return p_175193_.buildFuture();
                });
                int i = p_412865_.getReader().getCursor();
                boolean flag = p_412865_.shouldInvertValue();
                if (p_412865_.hasGamemodeNotEquals() && !flag) {
                    p_412865_.getReader().setCursor(i);
                    throw ERROR_INAPPLICABLE_OPTION.createWithContext(p_412865_.getReader(), "gamemode");
                } else {
                    String s = p_412865_.getReader().readUnquotedString();
                    GameType gametype = GameType.byName(s, null);
                    if (gametype == null) {
                        p_412865_.getReader().setCursor(i);
                        throw ERROR_GAME_MODE_INVALID.createWithContext(p_412865_.getReader(), s);
                    } else {
                        p_412865_.setIncludesEntities(false);
                        p_412865_.addPredicate(p_412864_ -> {
                            if (p_412864_ instanceof ServerPlayer serverplayer) {
                                GameType gametype1 = serverplayer.gameMode();
                                return gametype1 == gametype ^ flag;
                            } else {
                                return false;
                            }
                        });
                        if (flag) {
                            p_412865_.setHasGamemodeNotEquals(true);
                        } else {
                            p_412865_.setHasGamemodeEquals(true);
                        }
                    }
                }
            }, p_121540_ -> !p_121540_.hasGamemodeEquals(), Component.translatable("argument.entity.options.gamemode.description"));
            register("team", p_121538_ -> {
                boolean flag = p_121538_.shouldInvertValue();
                String s = p_121538_.getReader().readUnquotedString();
                p_121538_.addPredicate(p_400873_ -> {
                    Team team = p_400873_.getTeam();
                    String s1 = team == null ? "" : team.getName();
                    return s1.equals(s) != flag;
                });
                if (flag) {
                    p_121538_.setHasTeamNotEquals(true);
                } else {
                    p_121538_.setHasTeamEquals(true);
                }
            }, p_121536_ -> !p_121536_.hasTeamEquals(), Component.translatable("argument.entity.options.team.description"));
            register(
                "type",
                p_465871_ -> {
                    p_465871_.setSuggestions(
                        (p_367798_, p_367799_) -> {
                            SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), p_367798_, String.valueOf('!'));
                            SharedSuggestionProvider.suggestResource(
                                BuiltInRegistries.ENTITY_TYPE.getTags().map(p_465889_ -> p_465889_.key().location()), p_367798_, "!#"
                            );
                            if (!p_465871_.isTypeLimitedInversely()) {
                                SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), p_367798_);
                                SharedSuggestionProvider.suggestResource(
                                    BuiltInRegistries.ENTITY_TYPE.getTags().map(p_465890_ -> p_465890_.key().location()), p_367798_, String.valueOf('#')
                                );
                            }

                            return p_367798_.buildFuture();
                        }
                    );
                    int i = p_465871_.getReader().getCursor();
                    boolean flag = p_465871_.shouldInvertValue();
                    if (p_465871_.isTypeLimitedInversely() && !flag) {
                        p_465871_.getReader().setCursor(i);
                        throw ERROR_INAPPLICABLE_OPTION.createWithContext(p_465871_.getReader(), "type");
                    } else {
                        if (flag) {
                            p_465871_.setTypeLimitedInversely();
                        }

                        if (p_465871_.isTag()) {
                            TagKey<EntityType<?>> tagkey = TagKey.create(Registries.ENTITY_TYPE, Identifier.read(p_465871_.getReader()));
                            p_465871_.addPredicate(p_205691_ -> p_205691_.getType().is(tagkey) != flag);
                        } else {
                            Identifier identifier = Identifier.read(p_465871_.getReader());
                            EntityType<?> entitytype = BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElseThrow(() -> {
                                p_465871_.getReader().setCursor(i);
                                return ERROR_ENTITY_TYPE_INVALID.createWithContext(p_465871_.getReader(), identifier.toString());
                            });
                            if (Objects.equals(EntityType.PLAYER, entitytype) && !flag) {
                                p_465871_.setIncludesEntities(false);
                            }

                            p_465871_.addPredicate(p_175151_ -> Objects.equals(entitytype, p_175151_.getType()) != flag);
                            if (!flag) {
                                p_465871_.limitToType(entitytype);
                            }
                        }
                    }
                },
                p_121532_ -> !p_121532_.isTypeLimited(),
                Component.translatable("argument.entity.options.type.description")
            );
            register("tag", p_121530_ -> {
                boolean flag = p_121530_.shouldInvertValue();
                String s = p_121530_.getReader().readUnquotedString();
                p_121530_.addPredicate(p_175166_ -> "".equals(s) ? p_175166_.getTags().isEmpty() != flag : p_175166_.getTags().contains(s) != flag);
            }, p_121528_ -> true, Component.translatable("argument.entity.options.tag.description"));
            register(
                "nbt",
                p_399365_ -> {
                    boolean flag = p_399365_.shouldInvertValue();
                    CompoundTag compoundtag = TagParser.parseCompoundAsArgument(p_399365_.getReader());
                    p_399365_.addPredicate(
                        p_421298_ -> {
                            boolean flag1;
                            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
                                    p_421298_.problemPath(), LOGGER
                                )) {
                                TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_421298_.registryAccess());
                                p_421298_.saveWithoutId(tagvalueoutput);
                                if (p_421298_ instanceof ServerPlayer serverplayer) {
                                    ItemStack itemstack = serverplayer.getInventory().getSelectedItem();
                                    if (!itemstack.isEmpty()) {
                                        tagvalueoutput.store("SelectedItem", ItemStack.CODEC, itemstack);
                                    }
                                }

                                flag1 = NbtUtils.compareNbt(compoundtag, tagvalueoutput.buildResult(), true) != flag;
                            }

                            return flag1;
                        }
                    );
                },
                p_121524_ -> true,
                Component.translatable("argument.entity.options.nbt.description")
            );
            register("scores", p_465872_ -> {
                StringReader stringreader = p_465872_.getReader();
                Map<String, MinMaxBounds.Ints> map = Maps.newHashMap();
                stringreader.expect('{');
                stringreader.skipWhitespace();

                while (stringreader.canRead() && stringreader.peek() != '}') {
                    stringreader.skipWhitespace();
                    String s = stringreader.readUnquotedString();
                    stringreader.skipWhitespace();
                    stringreader.expect('=');
                    stringreader.skipWhitespace();
                    MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromReader(stringreader);
                    map.put(s, minmaxbounds$ints);
                    stringreader.skipWhitespace();
                    if (stringreader.canRead() && stringreader.peek() == ',') {
                        stringreader.skip();
                    }
                }

                stringreader.expect('}');
                if (!map.isEmpty()) {
                    p_465872_.addPredicate(p_465880_ -> {
                        Scoreboard scoreboard = p_465880_.level().getServer().getScoreboard();

                        for (Entry<String, MinMaxBounds.Ints> entry : map.entrySet()) {
                            Objective objective = scoreboard.getObjective(entry.getKey());
                            if (objective == null) {
                                return false;
                            }

                            ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(p_465880_, objective);
                            if (readonlyscoreinfo == null) {
                                return false;
                            }

                            if (!entry.getValue().matches(readonlyscoreinfo.value())) {
                                return false;
                            }
                        }

                        return true;
                    });
                }

                p_465872_.setHasScores(true);
            }, p_121518_ -> !p_121518_.hasScores(), Component.translatable("argument.entity.options.scores.description"));
            register("advancements", p_465893_ -> {
                StringReader stringreader = p_465893_.getReader();
                Map<Identifier, Predicate<AdvancementProgress>> map = Maps.newHashMap();
                stringreader.expect('{');
                stringreader.skipWhitespace();

                while (stringreader.canRead() && stringreader.peek() != '}') {
                    stringreader.skipWhitespace();
                    Identifier identifier = Identifier.read(stringreader);
                    stringreader.skipWhitespace();
                    stringreader.expect('=');
                    stringreader.skipWhitespace();
                    if (stringreader.canRead() && stringreader.peek() == '{') {
                        Map<String, Predicate<CriterionProgress>> map1 = Maps.newHashMap();
                        stringreader.skipWhitespace();
                        stringreader.expect('{');
                        stringreader.skipWhitespace();

                        while (stringreader.canRead() && stringreader.peek() != '}') {
                            stringreader.skipWhitespace();
                            String s = stringreader.readUnquotedString();
                            stringreader.skipWhitespace();
                            stringreader.expect('=');
                            stringreader.skipWhitespace();
                            boolean flag1 = stringreader.readBoolean();
                            map1.put(s, p_175186_ -> p_175186_.isDone() == flag1);
                            stringreader.skipWhitespace();
                            if (stringreader.canRead() && stringreader.peek() == ',') {
                                stringreader.skip();
                            }
                        }

                        stringreader.skipWhitespace();
                        stringreader.expect('}');
                        stringreader.skipWhitespace();
                        map.put(identifier, p_175169_ -> {
                            for (Entry<String, Predicate<CriterionProgress>> entry : map1.entrySet()) {
                                CriterionProgress criterionprogress = p_175169_.getCriterion(entry.getKey());
                                if (criterionprogress == null || !entry.getValue().test(criterionprogress)) {
                                    return false;
                                }
                            }

                            return true;
                        });
                    } else {
                        boolean flag = stringreader.readBoolean();
                        map.put(identifier, p_175183_ -> p_175183_.isDone() == flag);
                    }

                    stringreader.skipWhitespace();
                    if (stringreader.canRead() && stringreader.peek() == ',') {
                        stringreader.skip();
                    }
                }

                stringreader.expect('}');
                if (!map.isEmpty()) {
                    p_465893_.addPredicate(p_465886_ -> {
                        if (!(p_465886_ instanceof ServerPlayer serverplayer)) {
                            return false;
                        } else {
                            PlayerAdvancements $$4 = serverplayer.getAdvancements();
                            ServerAdvancementManager $$5x = serverplayer.level().getServer().getAdvancements();

                            for (Entry<Identifier, Predicate<AdvancementProgress>> entry : map.entrySet()) {
                                AdvancementHolder advancementholder = $$5x.get(entry.getKey());
                                if (advancementholder == null || !entry.getValue().test($$4.getOrStartProgress(advancementholder))) {
                                    return false;
                                }
                            }

                            return true;
                        }
                    });
                    p_465893_.setIncludesEntities(false);
                }

                p_465893_.setHasAdvancements(true);
            }, p_121506_ -> !p_121506_.hasAdvancements(), Component.translatable("argument.entity.options.advancements.description"));
            register(
                "predicate",
                p_465878_ -> {
                    boolean flag = p_465878_.shouldInvertValue();
                    ResourceKey<LootItemCondition> resourcekey = ResourceKey.create(Registries.PREDICATE, Identifier.read(p_465878_.getReader()));
                    p_465878_.addPredicate(
                        p_445263_ -> {
                            if (p_445263_.level() instanceof ServerLevel serverlevel) {
                                Optional<LootItemCondition> optional = serverlevel.getServer()
                                    .reloadableRegistries()
                                    .lookup()
                                    .get(resourcekey)
                                    .map(Holder::value);
                                if (optional.isEmpty()) {
                                    return false;
                                } else {
                                    LootParams lootparams = new LootParams.Builder(serverlevel)
                                        .withParameter(LootContextParams.THIS_ENTITY, p_445263_)
                                        .withParameter(LootContextParams.ORIGIN, p_445263_.position())
                                        .create(LootContextParamSets.SELECTOR);
                                    LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
                                    lootcontext.pushVisitedElement(LootContext.createVisitedEntry(optional.get()));
                                    return flag ^ optional.get().test(lootcontext);
                                }
                            } else {
                                return false;
                            }
                        }
                    );
                },
                p_121435_ -> true,
                Component.translatable("argument.entity.options.predicate.description")
            );
        }
    }

    public static EntitySelectorOptions.Modifier get(EntitySelectorParser parser, String id, int cursor) throws CommandSyntaxException {
        EntitySelectorOptions.Option entityselectoroptions$option = OPTIONS.get(id);
        if (entityselectoroptions$option != null) {
            if (entityselectoroptions$option.canUse.test(parser)) {
                return entityselectoroptions$option.modifier;
            } else {
                throw ERROR_INAPPLICABLE_OPTION.createWithContext(parser.getReader(), id);
            }
        } else {
            parser.getReader().setCursor(cursor);
            throw ERROR_UNKNOWN_OPTION.createWithContext(parser.getReader(), id);
        }
    }

    public static void suggestNames(EntitySelectorParser parser, SuggestionsBuilder builder) {
        String s = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (Entry<String, EntitySelectorOptions.Option> entry : OPTIONS.entrySet()) {
            if (entry.getValue().canUse.test(parser) && entry.getKey().toLowerCase(Locale.ROOT).startsWith(s)) {
                builder.suggest(entry.getKey() + "=", entry.getValue().description);
            }
        }
    }

    @FunctionalInterface
    public interface Modifier {
        void handle(EntitySelectorParser parser) throws CommandSyntaxException;
    }

    record Option(EntitySelectorOptions.Modifier modifier, Predicate<EntitySelectorParser> canUse, Component description) {
    }
}
