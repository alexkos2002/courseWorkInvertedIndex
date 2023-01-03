package utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreProcUtility {
    private static final String WORD_SEARCHING_REGEX = "[\\w]+";
    private static final String WORDS_FILES_EXTENSION = ".txt";

    public PreProcUtility() {
    }

    public static Set<String> getAllWordsFromDirFiles(String filesLocationDirPath) {
        HashSet allWordsSet = new HashSet();
        File[] dirFiles = (new File(filesLocationDirPath)).listFiles();
        String curFilePath = null;
        Pattern wordSearchingPattern = Pattern.compile(WORD_SEARCHING_REGEX);
        int dirFilesNum = dirFiles.length;

        for (int i = 0; i < dirFilesNum; i++) {
            File curFile = dirFiles[i];

            try {
                curFilePath = curFile.getPath();
                String curFileText = IOUtility.readTextFromFile(curFile.getPath());
                Matcher curFileWordMatcher = wordSearchingPattern.matcher(curFileText);
                while (curFileWordMatcher.find()) {
                    String curWord = curFileText.substring(curFileWordMatcher.start(), curFileWordMatcher.end());
                    allWordsSet.add(curWord);
                }
            } catch (IOException e) {
                System.out.println(String.format("Error throughout reading words from the file %s.", curFilePath != null
                        ? curFilePath : "File doesn't exist"));
                break;
            }
        }

        return allWordsSet;
    }

    public static void writeAllStringsToNewDirFiles(Set<String> allStringsSet, int newDirFilesNum, String newFileDirPath,
                                                    String newDirFilesPrefix) {
        int allStringsNum = allStringsSet.size();
        int stringsPerFileNum = allStringsNum / newDirFilesNum;
        ArrayList allStringsList = new ArrayList();
        allStringsSet.stream().forEach(string -> allStringsList.add(string));
        File curNewFile = null;

        int i;
        int j;
        FileWriter curNewFileWriter;
        Object curNewFileWord;
        for (i = 0; i < newDirFilesNum; ++i) {
            curNewFile = new File(newFileDirPath != null && !newFileDirPath.isEmpty() ?
                    newFileDirPath + System.lineSeparator() + newDirFilesPrefix + i + ".txt" : newDirFilesPrefix + i + ".txt");

            try {
                curNewFileWriter = new FileWriter(curNewFile);
                j = i * stringsPerFileNum;
                while (true) {
                    if (j >= (i + 1) * stringsPerFileNum) {
                        curNewFileWriter.flush();
                        break;
                    }
                    curNewFileWord = allStringsList.get(j);
                    curNewFileWriter.write((String) curNewFileWord + "\n");
                    j++;
                }
                curNewFileWriter.close();
            } catch (IOException e) {
                System.out.println(String.format("Error throughout writing words to the file %s.", newFileDirPath != null
                        ? curNewFile.getPath() : "File doesn't exist"));
            }
        }

        try {
            curNewFileWriter = new FileWriter(curNewFile, true);
            for (j = i * stringsPerFileNum; j < allStringsNum; ++j) {
                curNewFileWord = allStringsList.get(j);
                curNewFileWriter.write((String) curNewFileWord + "\n");
            }
            curNewFileWriter.close();
        } catch (IOException e) {
            System.out.println(String.format("Error throughout writing words to the file %s.", newFileDirPath != null
                    ? curNewFile.getPath() : "File doesn't exist"));
        }

    }
}
