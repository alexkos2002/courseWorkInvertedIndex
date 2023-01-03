package index.building.strategy;

import entity.FilePointer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SequentialWordTextFileInvertedIndexBuildingStrategy implements
        InvertedIndexBuildingStrategy<String, HashMap<String, HashSet<String>>> {

    public static final String WORD_SEARCHING_REGEX = "[\\w]+";

    @Override
    public void populateTargetStore(String filesLocationDirPath, HashMap<String, HashSet<String>> indexMap) {
        File[] filesToIndex = new File(filesLocationDirPath).listFiles();
        String curFilePath = null;
        String curFileText;
        String curWordFromFile;
        Pattern wordSearchingPattern = Pattern.compile(WORD_SEARCHING_REGEX);
        Matcher curMatcher;
        HashSet<String> curFilesContainWordSet;
        for (File curFile : filesToIndex) {
            try {
                curFilePath = curFile.getPath();
                curFileText = readTextFromFile(curFile.getPath());
                curMatcher = wordSearchingPattern.matcher(curFileText);
                while (curMatcher.find()) {
                    curWordFromFile = curFileText.substring(curMatcher.start(), curMatcher.end());
                    curFilesContainWordSet = indexMap.get(curWordFromFile);
                    if (curFilesContainWordSet == null) {
                        curFilesContainWordSet = new HashSet<>();
                        curFilesContainWordSet.add(curFilePath);
                        indexMap.put(curWordFromFile, curFilesContainWordSet);
                    } else {
                        curFilesContainWordSet.add(curFilePath);
                    }
                }
            } catch (IOException ioe) {
                System.out.println(String.format("Error throughout reading words from the file %s.",
                        curFilePath != null ? curFilePath : "File doesn't exist"));
                break;
            }
        }
    }

    private String readTextFromFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }
}
