package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.stream.Stream;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeCommand {
    private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType(
        p_304174_ -> Component.translatableEscape("commands.attribute.failed.entity", p_304174_)
    );
    private static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ATTRIBUTE = new Dynamic2CommandExceptionType(
        (p_304185_, p_304186_) -> Component.translatableEscape("commands.attribute.failed.no_attribute", p_304185_, p_304186_)
    );
    private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType(
        (p_304182_, p_304183_, p_304184_) -> Component.translatableEscape("commands.attribute.failed.no_modifier", p_304183_, p_304182_, p_304184_)
    );
    private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType(
        (p_304187_, p_304188_, p_304189_) -> Component.translatableEscape("commands.attribute.failed.modifier_already_present", p_304189_, p_304188_, p_304187_)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("attribute")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.argument("target", EntityArgument.entity())
                        .then(
                            Commands.argument("attribute", ResourceArgument.resource(context, Registries.ATTRIBUTE))
                                .then(
                                    Commands.literal("get")
                                        .executes(
                                            p_248109_ -> getAttributeValue(
                                                p_248109_.getSource(),
                                                EntityArgument.getEntity(p_248109_, "target"),
                                                ResourceArgument.getAttribute(p_248109_, "attribute"),
                                                1.0
                                            )
                                        )
                                        .then(
                                            Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                .executes(
                                                    p_248104_ -> getAttributeValue(
                                                        p_248104_.getSource(),
                                                        EntityArgument.getEntity(p_248104_, "target"),
                                                        ResourceArgument.getAttribute(p_248104_, "attribute"),
                                                        DoubleArgumentType.getDouble(p_248104_, "scale")
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("base")
                                        .then(
                                            Commands.literal("set")
                                                .then(
                                                    Commands.argument("value", DoubleArgumentType.doubleArg())
                                                        .executes(
                                                            p_248102_ -> setAttributeBase(
                                                                p_248102_.getSource(),
                                                                EntityArgument.getEntity(p_248102_, "target"),
                                                                ResourceArgument.getAttribute(p_248102_, "attribute"),
                                                                DoubleArgumentType.getDouble(p_248102_, "value")
                                                            )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.literal("get")
                                                .executes(
                                                    p_248112_ -> getAttributeBase(
                                                        p_248112_.getSource(),
                                                        EntityArgument.getEntity(p_248112_, "target"),
                                                        ResourceArgument.getAttribute(p_248112_, "attribute"),
                                                        1.0
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                        .executes(
                                                            p_248106_ -> getAttributeBase(
                                                                p_248106_.getSource(),
                                                                EntityArgument.getEntity(p_248106_, "target"),
                                                                ResourceArgument.getAttribute(p_248106_, "attribute"),
                                                                DoubleArgumentType.getDouble(p_248106_, "scale")
                                                            )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.literal("reset")
                                                .executes(
                                                    p_382664_ -> resetAttributeBase(
                                                        p_382664_.getSource(),
                                                        EntityArgument.getEntity(p_382664_, "target"),
                                                        ResourceArgument.getAttribute(p_382664_, "attribute")
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("modifier")
                                        .then(
                                            Commands.literal("add")
                                                .then(
                                                    Commands.argument("id", IdentifierArgument.id())
                                                        .then(
                                                            Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .then(
                                                                    Commands.literal("add_value")
                                                                        .executes(
                                                                            p_466197_ -> addModifier(
                                                                                p_466197_.getSource(),
                                                                                EntityArgument.getEntity(p_466197_, "target"),
                                                                                ResourceArgument.getAttribute(p_466197_, "attribute"),
                                                                                IdentifierArgument.getId(p_466197_, "id"),
                                                                                DoubleArgumentType.getDouble(p_466197_, "value"),
                                                                                AttributeModifier.Operation.ADD_VALUE
                                                                            )
                                                                        )
                                                                )
                                                                .then(
                                                                    Commands.literal("add_multiplied_base")
                                                                        .executes(
                                                                            p_466191_ -> addModifier(
                                                                                p_466191_.getSource(),
                                                                                EntityArgument.getEntity(p_466191_, "target"),
                                                                                ResourceArgument.getAttribute(p_466191_, "attribute"),
                                                                                IdentifierArgument.getId(p_466191_, "id"),
                                                                                DoubleArgumentType.getDouble(p_466191_, "value"),
                                                                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                                                            )
                                                                        )
                                                                )
                                                                .then(
                                                                    Commands.literal("add_multiplied_total")
                                                                        .executes(
                                                                            p_466185_ -> addModifier(
                                                                                p_466185_.getSource(),
                                                                                EntityArgument.getEntity(p_466185_, "target"),
                                                                                ResourceArgument.getAttribute(p_466185_, "attribute"),
                                                                                IdentifierArgument.getId(p_466185_, "id"),
                                                                                DoubleArgumentType.getDouble(p_466185_, "value"),
                                                                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                                                                            )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.literal("remove")
                                                .then(
                                                    Commands.argument("id", IdentifierArgument.id())
                                                        .suggests(
                                                            (p_382668_, p_382669_) -> SharedSuggestionProvider.suggestResource(
                                                                getAttributeModifiers(
                                                                    EntityArgument.getEntity(p_382668_, "target"),
                                                                    ResourceArgument.getAttribute(p_382668_, "attribute")
                                                                ),
                                                                p_382669_
                                                            )
                                                        )
                                                        .executes(
                                                            p_466193_ -> removeModifier(
                                                                p_466193_.getSource(),
                                                                EntityArgument.getEntity(p_466193_, "target"),
                                                                ResourceArgument.getAttribute(p_466193_, "attribute"),
                                                                IdentifierArgument.getId(p_466193_, "id")
                                                            )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.literal("value")
                                                .then(
                                                    Commands.literal("get")
                                                        .then(
                                                            Commands.argument("id", IdentifierArgument.id())
                                                                .suggests(
                                                                    (p_382670_, p_382671_) -> SharedSuggestionProvider.suggestResource(
                                                                        getAttributeModifiers(
                                                                            EntityArgument.getEntity(p_382670_, "target"),
                                                                            ResourceArgument.getAttribute(p_382670_, "attribute")
                                                                        ),
                                                                        p_382671_
                                                                    )
                                                                )
                                                                .executes(
                                                                    p_466190_ -> getAttributeModifier(
                                                                        p_466190_.getSource(),
                                                                        EntityArgument.getEntity(p_466190_, "target"),
                                                                        ResourceArgument.getAttribute(p_466190_, "attribute"),
                                                                        IdentifierArgument.getId(p_466190_, "id"),
                                                                        1.0
                                                                    )
                                                                )
                                                                .then(
                                                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                                        .executes(
                                                                            p_466192_ -> getAttributeModifier(
                                                                                p_466192_.getSource(),
                                                                                EntityArgument.getEntity(p_466192_, "target"),
                                                                                ResourceArgument.getAttribute(p_466192_, "attribute"),
                                                                                IdentifierArgument.getId(p_466192_, "id"),
                                                                                DoubleArgumentType.getDouble(p_466192_, "scale")
                                                                            )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static AttributeInstance getAttributeInstance(Entity entity, Holder<Attribute> attribute) throws CommandSyntaxException {
        AttributeInstance attributeinstance = getLivingEntity(entity).getAttributes().getInstance(attribute);
        if (attributeinstance == null) {
            throw ERROR_NO_SUCH_ATTRIBUTE.create(entity.getName(), getAttributeDescription(attribute));
        } else {
            return attributeinstance;
        }
    }

    private static LivingEntity getLivingEntity(Entity target) throws CommandSyntaxException {
        if (!(target instanceof LivingEntity)) {
            throw ERROR_NOT_LIVING_ENTITY.create(target.getName());
        } else {
            return (LivingEntity)target;
        }
    }

    private static LivingEntity getEntityWithAttribute(Entity entity, Holder<Attribute> attribute) throws CommandSyntaxException {
        LivingEntity livingentity = getLivingEntity(entity);
        if (!livingentity.getAttributes().hasAttribute(attribute)) {
            throw ERROR_NO_SUCH_ATTRIBUTE.create(entity.getName(), getAttributeDescription(attribute));
        } else {
            return livingentity;
        }
    }

    private static int getAttributeValue(CommandSourceStack source, Entity entity, Holder<Attribute> attribute, double scale) throws CommandSyntaxException {
        LivingEntity livingentity = getEntityWithAttribute(entity, attribute);
        double d0 = livingentity.getAttributeValue(attribute);
        source.sendSuccess(
            () -> Component.translatable("commands.attribute.value.get.success", getAttributeDescription(attribute), entity.getName(), d0), false
        );
        return (int)(d0 * scale);
    }

    private static int getAttributeBase(CommandSourceStack source, Entity entity, Holder<Attribute> attribute, double scale) throws CommandSyntaxException {
        LivingEntity livingentity = getEntityWithAttribute(entity, attribute);
        double d0 = livingentity.getAttributeBaseValue(attribute);
        source.sendSuccess(
            () -> Component.translatable("commands.attribute.base_value.get.success", getAttributeDescription(attribute), entity.getName(), d0), false
        );
        return (int)(d0 * scale);
    }

    private static int getAttributeModifier(CommandSourceStack source, Entity entity, Holder<Attribute> attribute, Identifier id, double scale) throws CommandSyntaxException {
        LivingEntity livingentity = getEntityWithAttribute(entity, attribute);
        AttributeMap attributemap = livingentity.getAttributes();
        if (!attributemap.hasModifier(attribute, id)) {
            throw ERROR_NO_SUCH_MODIFIER.create(entity.getName(), getAttributeDescription(attribute), id);
        } else {
            double d0 = attributemap.getModifierValue(attribute, id);
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.attribute.modifier.value.get.success",
                    Component.translationArg(id),
                    getAttributeDescription(attribute),
                    entity.getName(),
                    d0
                ),
                false
            );
            return (int)(d0 * scale);
        }
    }

    private static Stream<Identifier> getAttributeModifiers(Entity entity, Holder<Attribute> attribute) throws CommandSyntaxException {
        AttributeInstance attributeinstance = getAttributeInstance(entity, attribute);
        return attributeinstance.getModifiers().stream().map(AttributeModifier::id);
    }

    private static int setAttributeBase(CommandSourceStack source, Entity entity, Holder<Attribute> attribute, double value) throws CommandSyntaxException {
        getAttributeInstance(entity, attribute).setBaseValue(value);
        source.sendSuccess(
            () -> Component.translatable("commands.attribute.base_value.set.success", getAttributeDescription(attribute), entity.getName(), value),
            false
        );
        return 1;
    }

    private static int resetAttributeBase(CommandSourceStack source, Entity entity, Holder<Attribute> attribute) throws CommandSyntaxException {
        LivingEntity livingentity = getLivingEntity(entity);
        if (!livingentity.getAttributes().resetBaseValue(attribute)) {
            throw ERROR_NO_SUCH_ATTRIBUTE.create(entity.getName(), getAttributeDescription(attribute));
        } else {
            double d0 = livingentity.getAttributeBaseValue(attribute);
            source.sendSuccess(
                () -> Component.translatable("commands.attribute.base_value.reset.success", getAttributeDescription(attribute), entity.getName(), d0), false
            );
            return 1;
        }
    }

    private static int addModifier(
        CommandSourceStack source,
        Entity entity,
        Holder<Attribute> attribute,
        Identifier id,
        double amount,
        AttributeModifier.Operation operation
    ) throws CommandSyntaxException {
        AttributeInstance attributeinstance = getAttributeInstance(entity, attribute);
        AttributeModifier attributemodifier = new AttributeModifier(id, amount, operation);
        if (attributeinstance.hasModifier(id)) {
            throw ERROR_MODIFIER_ALREADY_PRESENT.create(entity.getName(), getAttributeDescription(attribute), id);
        } else {
            attributeinstance.addPermanentModifier(attributemodifier);
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.attribute.modifier.add.success", Component.translationArg(id), getAttributeDescription(attribute), entity.getName()
                ),
                false
            );
            return 1;
        }
    }

    private static int removeModifier(CommandSourceStack source, Entity entity, Holder<Attribute> attribute, Identifier id) throws CommandSyntaxException {
        AttributeInstance attributeinstance = getAttributeInstance(entity, attribute);
        if (attributeinstance.removeModifier(id)) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.attribute.modifier.remove.success", Component.translationArg(id), getAttributeDescription(attribute), entity.getName()
                ),
                false
            );
            return 1;
        } else {
            throw ERROR_NO_SUCH_MODIFIER.create(entity.getName(), getAttributeDescription(attribute), id);
        }
    }

    private static Component getAttributeDescription(Holder<Attribute> attribute) {
        return Component.translatable(attribute.value().getDescriptionId());
    }
}
