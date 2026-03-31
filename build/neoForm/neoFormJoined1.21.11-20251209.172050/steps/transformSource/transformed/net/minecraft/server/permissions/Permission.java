package net.minecraft.server.permissions;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public interface Permission {
    Codec<Permission> FULL_CODEC = BuiltInRegistries.PERMISSION_TYPE.byNameCodec().dispatch(Permission::codec, p_454889_ -> p_454889_);
    Codec<Permission> CODEC = Codec.either(FULL_CODEC, Identifier.CODEC)
        .xmap(
            p_454729_ -> p_454729_.map(p_454888_ -> (Permission)p_454888_, Permission.Atom::create),
            p_466391_ -> p_466391_ instanceof Permission.Atom permission$atom ? Either.right(permission$atom.id()) : Either.left(p_466391_)
        );

    MapCodec<? extends Permission> codec();

    public record Atom(Identifier id) implements Permission {
        public static final MapCodec<Permission.Atom> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_466392_ -> p_466392_.group(Identifier.CODEC.fieldOf("id").forGetter(Permission.Atom::id)).apply(p_466392_, Permission.Atom::new)
        );

        @Override
        public MapCodec<Permission.Atom> codec() {
            return MAP_CODEC;
        }

        public static Permission.Atom create(String id) {
            return create(Identifier.withDefaultNamespace(id));
        }

        public static Permission.Atom create(Identifier id) {
            return new Permission.Atom(id);
        }
    }

    public record HasCommandLevel(PermissionLevel level) implements Permission {
        public static final MapCodec<Permission.HasCommandLevel> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_455900_ -> p_455900_.group(PermissionLevel.CODEC.fieldOf("level").forGetter(Permission.HasCommandLevel::level))
                .apply(p_455900_, Permission.HasCommandLevel::new)
        );

        @Override
        public MapCodec<Permission.HasCommandLevel> codec() {
            return MAP_CODEC;
        }
    }
}
