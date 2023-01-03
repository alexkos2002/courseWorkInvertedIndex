package utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreProcUtility {

    private static final String WORD_SEARCHING_REGEX = "[\\w]+";

    private static final String WORDS_FILES_EXTENSION = ".txt";

    public static Set<String> getAllWordsFromDirFiles(String filesLocationDirPath) {
        Set<String> allWordsFromFiles = new HashSet<>();
        File[] filesToIndex = new File(filesLocationDirPath).listFiles();
        String curFilePath = null;
        String curFileText;
        String curWordFromFile;
        Pattern wordSearchingPattern = Pattern.compile(WORD_SEARCHING_REGEX);
        Matcher curMatcher;
        for (File curFile : filesToIndex) {
            try {
                curFilePath = curFile.getPath();
                curFileText = IOUtility.readTextFromFile(curFile.getPath());
                curMatcher = wordSearchingPattern.matcher(curFileText);
                while (curMatcher.find()) {
                    curWordFromFile = curFileText.substring(curMatcher.start(), curMatcher.end());
                    allWordsFromFiles.add(curWordFromFile);
                }
            } catch (IOException ioe) {
                System.out.println(String.format("Error throughout reading words from the file %s.",
                        curFilePath != null ? curFilePath : "File doesn't exist"));
                break;
            }
        }
        return allWordsFromFiles;
    }

    // can be used for writing words' quantities to files
    public static void writeAllStringsToNewDirFiles(Set<String> stringSet, int newDirFilesNum, String newFilesDirPath,
                                                    String newDirFilesPrefix) {
        int stringsNum = stringSet.size();
        int stringsPerFileNum = stringsNum / newDirFilesNum;
        List<String> strings = new ArrayList();
        stringSet.stream().forEach(string -> strings.add(string));
        File curNewDirFile = null;
        int i, j;
        for (i = 0; i < newDirFilesNum; i++) {
             curNewDirFile = new File(newFilesDirPath != null && !newFilesDirPath.isEmpty() ?
                     newFilesDirPath + System.lineSeparator() + newDirFilesPrefix + i + WORDS_FILES_EXTENSION :
                     newDirFilesPrefix + i + WORDS_FILES_EXTENSION);
             try (FileWriter fw = new FileWriter(curNewDirFile)) {
                 for (j = i * stringsPerFileNum; j < (i + 1) * stringsPerFileNum; j++) {
                    fw.write(strings.get(j) + "\n");
                 }
                 fw.flush();
             } catch (IOException e) {
                 System.out.println(String.format("Error throughout writing words to the file %s.",
                         newFilesDirPath != null ? curNewDirFile.getPath() : "File doesn't exist"));
             }
        }
        try (FileWriter fw = new FileWriter(curNewDirFile, true)) {
            for (j = i * stringsPerFileNum; j < stringsNum; j++) {
                fw.write(strings.get(j) + "\n");
            }
        } catch (IOException e) {
            System.out.println(String.format("Error throughout writing words to the file %s.",
                    newFilesDirPath != null ? curNewDirFile.getPath() : "File doesn't exist"));
        }
    }

}
