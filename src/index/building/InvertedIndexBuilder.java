package index.building;

import concurrent.list.CustomConcurrentArrayList;
import concurrent.map.CustomConcurrentMap;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static index.building.strategy.ParallelWordTextFileInvertedIndexBuildingStrategy.WORD_SEARCHING_REGEX;

public class InvertedIndexBuilder extends AbstractInvertedIndexBuilder {

    private CustomConcurrentMap<String, CustomConcurrentArrayList<String>> indexMap;

    private AtomicInteger buildersFinishedIndexationNum;

    private volatile boolean errorFlag;

    public InvertedIndexBuilder(CustomConcurrentMap<String, CustomConcurrentArrayList<String>> indexMap,
                                File[] filesToIndex, int firstFileToIndex, int lastFileToIndex,
                                AtomicInteger buildersFinishedIndexationNum, boolean errorFlag) {
        super(filesToIndex, firstFileToIndex, lastFileToIndex);
        this.indexMap = indexMap;
        this.buildersFinishedIndexationNum = buildersFinishedIndexationNum;
        this.errorFlag = errorFlag;
    }

    @Override
    public void run() {
        try {
            indexFiles();
        } catch (IOException | InterruptedException e) {
            errorFlag = true;
        }
    }

    private void indexFiles() throws IOException, InterruptedException {
        String curFilePath = null;
        String curFileText;
        String curWordFromFile;
        Set<String> uniqueWordsFromFile = new HashSet<>();
        Pattern wordSearchingPattern = Pattern.compile(WORD_SEARCHING_REGEX);
        Matcher curMatcher;
        CustomConcurrentArrayList<String> curFilesContainWordList;
        for (int i = firstFileToIndex; i < lastFileToIndex; i++) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            try {
                curFilePath = (filesToIndex[i]).getPath();
                //System.out.println(curFilePath);
                curFileText = readTextFromFile((filesToIndex[i]).getPath());
                curMatcher = wordSearchingPattern.matcher(curFileText);
                while (curMatcher.find()) {
                    curWordFromFile = curFileText.substring(curMatcher.start(), curMatcher.end());
                    if (!uniqueWordsFromFile.contains(curWordFromFile)) {
                        uniqueWordsFromFile.add(curWordFromFile);
                        int bucketLockIdx = indexMap.calculateExternalBucketLockIdxForKey(curWordFromFile);
                        indexMap.acquireExternalBucketLock(bucketLockIdx);
                        curFilesContainWordList = indexMap.get(curWordFromFile);
                        if (curFilesContainWordList == null) {
                            curFilesContainWordList = new CustomConcurrentArrayList<>();
                            indexMap.putWithExternalBucketLock(curWordFromFile, curFilesContainWordList);
                            indexMap.releaseExternalBucketLock(bucketLockIdx);
                            curFilesContainWordList.add(curFilePath);
                        } else {
                            indexMap.releaseExternalBucketLock(bucketLockIdx);
                            curFilesContainWordList.add(curFilePath);
                        }
                    }
                }
                uniqueWordsFromFile.clear();
            } catch (IOException ioe) {
                System.out.println(String.format("Error throughout reading words from the file %s.",
                        curFilePath != null ? curFilePath : "File doesn't exist"));
                throw ioe;
            } catch (InterruptedException ie) {
                System.out.println(String.format("Error throughout indexing words from file %s",
                        curFilePath != null ? curFilePath : "File doesn't exist"));
                throw ie;
            }
        }
        buildersFinishedIndexationNum.incrementAndGet();
    }
}
