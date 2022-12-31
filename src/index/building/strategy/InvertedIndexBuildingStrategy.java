package index.building.strategy;

public interface InvertedIndexBuildingStrategy<SOURCE_STORE, TARGET_STORE> {

    /**
     * Populates an inverted index's target store with token-sources relations data retrieved from source store.
     * @param sourceStore - store of sources of which the inverted index should be built.
     * @param targetStore - store which is a part of inverted index.
     */
    void populateTargetStore(SOURCE_STORE sourceStore, TARGET_STORE targetStore);

}
