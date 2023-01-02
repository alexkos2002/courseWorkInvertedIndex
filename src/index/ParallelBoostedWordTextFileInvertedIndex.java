package index;

import concurrent.list.CustomConcurrentArrayList;
import concurrent.map.CustomConcurrentHashMap;
import concurrent.map.CustomConcurrentMap;
import index.building.strategy.InvertedIndexBuildingStrategy;

public class ParallelBoostedWordTextFileInvertedIndex implements InvertedIndex<String, String, String>{

    private CustomConcurrentMap<String, CustomConcurrentArrayList<String>> indexMap;

    @Override
    public void build(InvertedIndexBuildingStrategy strategy, String filesLocationDirPath) {
        indexMap = new CustomConcurrentHashMap<>(8, 0.75);
        strategy.populateTargetStore(filesLocationDirPath, indexMap);
    }

    @Override
    public CustomConcurrentArrayList<String> getSourcesList(String word) {
        return indexMap.get(word);
    }
}
