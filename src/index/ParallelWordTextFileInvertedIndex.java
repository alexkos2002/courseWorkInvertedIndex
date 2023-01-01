package index;

import concurrent.list.CustomConcurrentArrayList;
import concurrent.map.CustomConcurrentHashMap;
import concurrent.map.CustomConcurrentMap;
import entity.FilePointer;
import index.building.strategy.InvertedIndexBuildingStrategy;

import java.util.ArrayList;

public class ParallelWordTextFileInvertedIndex implements InvertedIndex<String, FilePointer, String>{
    private CustomConcurrentMap<String, CustomConcurrentArrayList<FilePointer>> indexMap;

    @Override
    public void build(InvertedIndexBuildingStrategy strategy, String filesLocationDirPath) {
        indexMap = new CustomConcurrentHashMap<>(8, 0.75);
        strategy.populateTargetStore(filesLocationDirPath, indexMap);
    }

    @Override
    public CustomConcurrentArrayList<FilePointer> getSourcesList(String word) {
        return indexMap.get(word);
    }
}
