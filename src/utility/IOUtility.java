package utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IOUtility {

    private static final String FILE_LIST_SEARCHING_REGEX = "\\[.*\\]";

    public IOUtility() {
    }

    public static String readTextFromFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    public static boolean comparePathsContainWordFiles(String path1, String path2) {
        try (BufferedReader bf1 = Files.newBufferedReader(Path.of(path1));
             BufferedReader bf2 = Files.newBufferedReader(Path.of(path2))) {

            Pattern wordSearchingPattern = Pattern.compile(FILE_LIST_SEARCHING_REGEX);
            Set<String> file1PathsContainCurWordSet = new TreeSet<>();
            Set<String> file2PathsContainCurWordSet = new TreeSet<>();
            String file1Text = readTextFromFile(path1);
            String file2Text = readTextFromFile(path2);
            Matcher file1Matcher = wordSearchingPattern.matcher(file1Text);
            Matcher file2Matcher = wordSearchingPattern.matcher(file2Text);
            String[] file1PathsContainCurWord;
            String[] file2PathsContainCurWord;
            boolean isPathsFoundForFile1Word;
            boolean isPathsFoundForFile2Word;

            while ((isPathsFoundForFile1Word = file1Matcher.find()) & (isPathsFoundForFile2Word = file2Matcher.find())) {
                file1PathsContainCurWord = file1Text.substring(file1Matcher.start() + 1, file1Matcher.end() - 1).split(",");
                file2PathsContainCurWord = file2Text.substring(file2Matcher.start() + 1, file2Matcher.end() - 1).split(",");
                file1PathsContainCurWord = replaceAllSpecialCharsInPaths(file1PathsContainCurWord);
                file2PathsContainCurWord = replaceAllSpecialCharsInPaths(file2PathsContainCurWord);
                Arrays.sort(file1PathsContainCurWord);
                Arrays.sort(file2PathsContainCurWord);
                Arrays.stream(file1PathsContainCurWord).forEach(curWordFilePath -> file1PathsContainCurWordSet.add(curWordFilePath));
                Arrays.stream(file2PathsContainCurWord).forEach(curWordFilePath -> file2PathsContainCurWordSet.add(curWordFilePath));
                if (!(Arrays.compare(file1PathsContainCurWord, file2PathsContainCurWord) == 0)) {
                    System.out.println("Not matching paths from file1: " + Arrays.toString(file1PathsContainCurWord));
                    System.out.println("Not matching paths from file2: " + Arrays.toString(file2PathsContainCurWord));
                    return false;
                }
            }

            if (isPathsFoundForFile1Word == isPathsFoundForFile2Word) {
                return true;
            } else {
                return false;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String[] replaceAllSpecialCharsInPaths(String[] paths) {
        int pathsLength = paths.length;
        String[] result = new String[pathsLength];
        String curPath;
        for (int i = 0; i < pathsLength; i++) {
            curPath = (paths[i]).replaceAll("\n", "");
            curPath = curPath.replaceAll(" ", "");
            result[i] = curPath;
        }
        return result;
    }
}
