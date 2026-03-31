package net.minecraft.client.searchtree;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IdentifierSearchTree<T> {
    static <T> IdentifierSearchTree<T> empty() {
        return new IdentifierSearchTree<T>() {
            @Override
            public List<T> searchNamespace(String p_468794_) {
                return List.of();
            }

            @Override
            public List<T> searchPath(String p_468832_) {
                return List.of();
            }
        };
    }

    static <T> IdentifierSearchTree<T> create(List<T> values, Function<T, Stream<Identifier>> idGetter) {
        if (values.isEmpty()) {
            return empty();
        } else {
            final SuffixArray<T> suffixarray = new SuffixArray<>();
            final SuffixArray<T> suffixarray1 = new SuffixArray<>();

            for (T t : values) {
                idGetter.apply(t).forEach(p_467663_ -> {
                    suffixarray.add(t, p_467663_.getNamespace().toLowerCase(Locale.ROOT));
                    suffixarray1.add(t, p_467663_.getPath().toLowerCase(Locale.ROOT));
                });
            }

            suffixarray.generate();
            suffixarray1.generate();
            return new IdentifierSearchTree<T>() {
                @Override
                public List<T> searchNamespace(String p_468101_) {
                    return suffixarray.search(p_468101_);
                }

                @Override
                public List<T> searchPath(String p_467610_) {
                    return suffixarray1.search(p_467610_);
                }
            };
        }
    }

    List<T> searchNamespace(String namespace);

    List<T> searchPath(String path);
}
