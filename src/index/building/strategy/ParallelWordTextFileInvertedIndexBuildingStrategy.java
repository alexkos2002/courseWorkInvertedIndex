package index.building.strategy;

import concurrent.list.CustomConcurrentArrayList;
import concurrent.map.CustomConcurrentMap;
import index.building.InvertedIndexBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelWordTextFileInvertedIndexBuildingStrategy
        implements InvertedIndexBuildingStrategy<String, CustomConcurrentMap<String, CustomConcurrentArrayList<String>>> {

    public static final String WORD_SEARCHING_REGEX = "[\\w]+";

    private Thread[] builders;

    private int buildersNum;

    private AtomicInteger buildersFinishedIndexationNum;

    private volatile boolean errorFlag;

    public ParallelWordTextFileInvertedIndexBuildingStrategy(int buildersNum) {
        this.buildersNum = buildersNum;
        builders = new Thread[buildersNum];
        errorFlag = false;
        buildersFinishedIndexationNum = new AtomicInteger(0);
    }

    @Override
    public void populateTargetStore(String filesLocationDirPath, CustomConcurrentMap<String,
            CustomConcurrentArrayList<String>> indexMap) {
        File[] filesToIndex = new File(filesLocationDirPath).listFiles();
        int filesToIndexNum = filesToIndex.length;
        int filesPerThreadNum = filesToIndexNum / buildersNum;
        int notDistributedFilesNum = filesToIndexNum % buildersNum;
        for (int i = 0; i < buildersNum; i++) {
            builders[i] = new Thread(new InvertedIndexBuilder(indexMap, filesToIndex,
                    i * filesPerThreadNum, i + 1 == buildersNum ?
                    (i + 1) * filesPerThreadNum + notDistributedFilesNum :
                    (i + 1) * filesPerThreadNum, buildersFinishedIndexationNum, errorFlag));
            (builders[i]).start();
        }
        while (buildersFinishedIndexationNum.get() < buildersNum) {
            if (errorFlag) {
                stopAllBuilders();
                break;
            }
        }
    }

    private void stopAllBuilders() {
        for (int i = 0; i < buildersNum; i++) {
            (builders[i]).interrupt();
        }
    }
}
