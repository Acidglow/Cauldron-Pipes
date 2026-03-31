package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FullTextSearchTree<T> extends IdSearchTree<T> {
    private final SearchTree<T> plainTextSearchTree;

    public FullTextSearchTree(Function<T, Stream<String>> filter, Function<T, Stream<Identifier>> idGetter, List<T> contents) {
        super(idGetter, contents);
        this.plainTextSearchTree = SearchTree.plainText(contents, filter);
    }

    @Override
    protected List<T> searchPlainText(String p_235160_) {
        return this.plainTextSearchTree.search(p_235160_);
    }

    @Override
    protected List<T> searchIdentifier(String p_469066_, String p_469775_) {
        List<T> list = this.identifierSearchTree.searchNamespace(p_469066_);
        List<T> list1 = this.identifierSearchTree.searchPath(p_469775_);
        List<T> list2 = this.plainTextSearchTree.search(p_469775_);
        Iterator<T> iterator = new MergingUniqueIterator<>(list1.iterator(), list2.iterator(), this.additionOrder);
        return ImmutableList.copyOf(new IntersectionIterator<>(list.iterator(), iterator, this.additionOrder));
    }
}
