package index;

import entity.FilePointer;
import index.building.strategy.InvertedIndexBuildingStrategy;

import java.util.*;

public class SequentialWordTextFileInvertedIndex implements InvertedIndex<String, FilePointer, String>{
    private Map<String, HashSet<FilePointer>> indexMap;

    @Override
    public void build(InvertedIndexBuildingStrategy strategy, String filesLocationDirPath) {
        indexMap = new HashMap<>(8, 0.75f);
        strategy.populateTargetStore(filesLocationDirPath, indexMap);
    }

    @Override
    public HashSet<FilePointer> getSourcesList(String word) {
        return indexMap.get(word);
    }
}
