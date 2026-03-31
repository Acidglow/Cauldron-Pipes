package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IdSearchTree<T> implements SearchTree<T> {
    protected final Comparator<T> additionOrder;
    protected final IdentifierSearchTree<T> identifierSearchTree;

    public IdSearchTree(Function<T, Stream<Identifier>> idGetter, List<T> contents) {
        ToIntFunction<T> tointfunction = Util.createIndexLookup(contents);
        this.additionOrder = Comparator.comparingInt(tointfunction);
        this.identifierSearchTree = IdentifierSearchTree.create(contents, idGetter);
    }

    @Override
    public List<T> search(String p_235173_) {
        int i = p_235173_.indexOf(58);
        return i == -1 ? this.searchPlainText(p_235173_) : this.searchIdentifier(p_235173_.substring(0, i).trim(), p_235173_.substring(i + 1).trim());
    }

    protected List<T> searchPlainText(String query) {
        return this.identifierSearchTree.searchPath(query);
    }

    protected List<T> searchIdentifier(String namespace, String path) {
        List<T> list = this.identifierSearchTree.searchNamespace(namespace);
        List<T> list1 = this.identifierSearchTree.searchPath(path);
        return ImmutableList.copyOf(new IntersectionIterator<>(list.iterator(), list1.iterator(), this.additionOrder));
    }
}
