package index.building.strategy;

import concurrent.list.CustomConcurrentArrayList;
import concurrent.map.CustomConcurrentMap;
import concurrent.queue.CustomConcurrentArrayPriorityQueue;
import concurrent.queue.CustomConcurrentPriorityQueue;
import entity.FilePointer;
import index.building.InvertedIndexWithQueueBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelWordTextFileInvertedIndexWithQueueBuildingStrategy implements
        InvertedIndexBuildingStrategy<String, CustomConcurrentMap<String, CustomConcurrentArrayList<FilePointer>>> {
    /**
     * Queue of sources from which the inverted index is built.
     */

    public static final String WORD_SEARCHING_REGEX = "[\\w]+";
    private CustomConcurrentPriorityQueue<FilePointer> queue;

    private Thread[] builders;

    private int buildersNum;

    private AtomicInteger buildersFinishedAddToQueueNum;

    private volatile boolean errorFlag;

    public ParallelWordTextFileInvertedIndexWithQueueBuildingStrategy(int buildersNum) {
        this.buildersNum = buildersNum;
        builders = new Thread[buildersNum];
        this.errorFlag = false;
        this.buildersFinishedAddToQueueNum = new AtomicInteger(0);
    }

    @Override
    public void populateTargetStore(String filesLocationDirPath, CustomConcurrentMap<String,
            CustomConcurrentArrayList<FilePointer>> indexMap) {
        File[] filesToIndex = new File(filesLocationDirPath).listFiles();
        int filesToIndexNum = filesToIndex.length;
        queue = new CustomConcurrentArrayPriorityQueue<>(filesToIndexNum);
        int filesPerThreadNum = filesToIndexNum / buildersNum;
        int notDistributedFilesNum = filesToIndexNum % buildersNum;
        for (int i = 0; i < buildersNum; i++) {
            builders[i] = new Thread(new InvertedIndexWithQueueBuilder(indexMap, queue, filesToIndex,
                    i * filesPerThreadNum, i + 1 == buildersNum ?
                    (i + 1) * filesPerThreadNum + notDistributedFilesNum :
                    (i + 1) * filesPerThreadNum, buildersFinishedAddToQueueNum, errorFlag));
            (builders[i]).start();
        }
        while (buildersFinishedAddToQueueNum.get() < buildersNum || !queue.isEmpty()) {
            if (errorFlag) {
                stopAllBuilders();
                break;
            }
        }
        System.out.println(queue);
        System.out.println("Priority queue size: " + queue.size());
        queue.setAllPushesFinished(true);
        queue.notifyAllPopPerformers();
    }

    private void stopAllBuilders() {
        for (int i = 0; i < buildersNum; i++) {
            (builders[i]).interrupt();
        }
    }
}
