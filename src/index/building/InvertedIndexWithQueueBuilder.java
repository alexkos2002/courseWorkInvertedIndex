package index.building;

import concurrent.list.CustomConcurrentArrayList;
import concurrent.map.CustomConcurrentMap;
import concurrent.queue.CustomConcurrentPriorityQueue;
import entity.FilePointer;

import static index.building.strategy.ParallelWordTextFileInvertedIndexWithQueueBuildingStrategy.WORD_SEARCHING_REGEX;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InvertedIndexWithQueueBuilder extends AbstractInvertedIndexBuilder {

    private CustomConcurrentMap<String, CustomConcurrentArrayList<FilePointer>> indexMap;
    private CustomConcurrentPriorityQueue<FilePointer> queue;

    private AtomicInteger buildersFinishedAddToQueueNum;

    private volatile boolean errorFlag;

    public InvertedIndexWithQueueBuilder(CustomConcurrentMap<String, CustomConcurrentArrayList<FilePointer>> indexMap,
                                         CustomConcurrentPriorityQueue<FilePointer> queue, File[] filesToIndex,
                                         int firstFileToIndex, int lastFileToIndex,
                                         AtomicInteger buildersFinishedAddToQueueNum, boolean errorFlag) {
        super(filesToIndex, firstFileToIndex, lastFileToIndex);
        this.indexMap = indexMap;
        this.queue = queue;
        this.buildersFinishedAddToQueueNum = buildersFinishedAddToQueueNum;
        this.errorFlag = errorFlag;
    }

    @Override
    public void run() {
        try {
            addFilePointersToQueue();
            indexFiles();
        } catch (IOException | InterruptedException e) {
            errorFlag = true;
        }
    }

    private void addFilePointersToQueue() throws InterruptedException {
        File curFile;
        FilePointer curFilePointerToAdd;
        for (int i = firstFileToIndex; i < lastFileToIndex; i++) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            curFile = filesToIndex[i];
            curFilePointerToAdd = new FilePointer(curFile.getPath(), curFile.length());
            try {
                queue.push(curFilePointerToAdd);
            } catch (InterruptedException e) {
                System.out.println(String.format("Failed to push the pointer to the file %s to " +
                        "the queue.\n Reason: %s.", curFilePointerToAdd.getPath(), e.getMessage()));
                throw e;
            }
        }
        buildersFinishedAddToQueueNum.incrementAndGet();
    }

    private void indexFiles() throws InterruptedException, IOException {
        FilePointer curFilePointer =  null;
        String curFileText;
        String curWordFromFile;
        Set<String> uniqueWordsFromFile = new HashSet<>();
        Pattern wordSearchingPattern = Pattern.compile(WORD_SEARCHING_REGEX);
        Matcher curMatcher;
        CustomConcurrentArrayList<FilePointer> curFilesContainWordList;
        try {
            while ((curFilePointer = queue.pop()) != null) {
                //System.out.println(curFilePointer.getPath());
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {
                    curFileText = readTextFromFile(curFilePointer.getPath());
                    curMatcher = wordSearchingPattern.matcher(curFileText);
                    while (curMatcher.find()) {
                        curWordFromFile = curFileText.substring(curMatcher.start(), curMatcher.end());
                        //System.out.println(curWordFromFile);
                        if (!uniqueWordsFromFile.contains(curWordFromFile)) {
                            uniqueWordsFromFile.add(curWordFromFile);
                            int bucketLockIdx = indexMap.calculateExternalBucketLockIdxForKey(curWordFromFile);
                            indexMap.acquireExternalBucketLock(bucketLockIdx);
                            curFilesContainWordList = indexMap.get(curWordFromFile);
                            if (curFilesContainWordList == null) {
                                curFilesContainWordList = new CustomConcurrentArrayList<>();
                                indexMap.putWithExternalBucketLock(curWordFromFile, curFilesContainWordList);
                                indexMap.releaseExternalBucketLock(bucketLockIdx);
                                curFilesContainWordList.add(curFilePointer);
                            } else {
                                indexMap.releaseExternalBucketLock(bucketLockIdx);
                                curFilesContainWordList.add(curFilePointer);
                            }
                        }
                    }
                    uniqueWordsFromFile.clear();
                } catch (IOException ioe) {
                    System.out.println(String.format("Error throughout reading words from the file %s.",
                            curFilePointer != null ? curFilePointer.getPath() : "File doesn't exist"));
                    throw ioe;
                }
            }
        } catch (InterruptedException ie) {
            System.out.println(String.format("Failed to pop the pointer to the file %s from " +
                    "the queue.\n Reason: %s.", curFilePointer == null ? "File doesn't exist"
                    : curFilePointer.getPath(), ie.getMessage()));
            throw ie;
        }
    }

}
