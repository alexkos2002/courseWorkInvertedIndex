import index.ParallelBoostedWordTextFileInvertedIndex;
import index.ParallelWordTextFileInvertedIndex;
import index.SequentialWordTextFileInvertedIndex;
import index.building.strategy.ParallelWordTextFileInvertedIndexBuildingStrategy;
import index.building.strategy.ParallelWordTextFileInvertedIndexWithQueueBuildingStrategy;
import index.building.strategy.SequentialWordTextFileInvertedIndexBuildingStrategy;
import utility.IOUtility;
import utility.PreProcUtility;

import java.util.Set;

@SuppressWarnings("unchecked")
public class Main {

    private static final String FILES_LOCATION_DIRECTORY_PATH = "C:/University/Parallel Computing/Course Work/files_dataset_var6";
    private static final String WORDS_LOCATION_DIRECTORY_PATH = "";

    private static final String WORDS_PREFIX = "words";

    private static final String WORD_SEARCHING_REGEX = "[\\w]+";

    public static void main(String[] args) throws InterruptedException {

        Set<String> allWordsFromFiles = PreProcUtility.getAllWordsFromDirFiles(FILES_LOCATION_DIRECTORY_PATH);
        System.out.println(allWordsFromFiles.size());
        PreProcUtility.writeAllStringsToNewDirFiles(allWordsFromFiles, 5, WORDS_LOCATION_DIRECTORY_PATH, WORDS_PREFIX);

        SequentialWordTextFileInvertedIndex sequentialInvertedIndex = new SequentialWordTextFileInvertedIndex();
        long startTime = System.currentTimeMillis();
        sequentialInvertedIndex.build(new SequentialWordTextFileInvertedIndexBuildingStrategy(), FILES_LOCATION_DIRECTORY_PATH);
        System.out.println("Time elapsed: " + (System.currentTimeMillis() - startTime));
        System.out.println(sequentialInvertedIndex.getSourcesList("all").size());

        ParallelWordTextFileInvertedIndex parallelInvertedIndex = new ParallelWordTextFileInvertedIndex();
        startTime = System.currentTimeMillis();
        parallelInvertedIndex.build(new ParallelWordTextFileInvertedIndexWithQueueBuildingStrategy(3), FILES_LOCATION_DIRECTORY_PATH);
        System.out.println("Time elapsed: " + (System.currentTimeMillis() - startTime));
        System.out.println(parallelInvertedIndex.getSourcesList("all").size());

        ParallelBoostedWordTextFileInvertedIndex parallelBoostedInvertedIndex = new ParallelBoostedWordTextFileInvertedIndex();
        startTime = System.currentTimeMillis();
        parallelBoostedInvertedIndex.build(new ParallelWordTextFileInvertedIndexBuildingStrategy(3), FILES_LOCATION_DIRECTORY_PATH);
        System.out.println("Time elapsed: " + (System.currentTimeMillis() - startTime));
        System.out.println(parallelBoostedInvertedIndex.getSourcesList("all").size());

        System.out.println("Finish");
    }
}