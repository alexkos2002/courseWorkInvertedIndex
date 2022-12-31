package index;

import index.building.strategy.InvertedIndexBuildingStrategy;

import java.util.Collection;

public interface InvertedIndex<TOKEN, SOURCE, SOURCE_STORE> {


    /**
     * Builds an inverted index by applying the inverted index building algorithm from the strategy.
     * @param strategy - strategy which encapsulates the algorithm for building the inverted index.
     */
    void build(InvertedIndexBuildingStrategy strategy, SOURCE_STORE sourceStore);

    /**
     * Gets all sources which contain a token(one or more times) as a part of their tokenized content from inverted
     * index and returns them as a collection.
     * @param token - the token sources including entries of which are searched and returned.
     * @return - the collection of sources containing the token.
     */
    Collection<SOURCE> getSourcesList(TOKEN token);
}
