package index.building;

import utility.IOUtility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class AbstractInvertedIndexBuilder implements Runnable{
    protected File[] filesToIndex;
    protected int firstFileToIndex;
    protected int lastFileToIndex;

    public AbstractInvertedIndexBuilder(File[] filesToIndex, int firstFileToIndex, int lastFileToIndex) {
        this.filesToIndex = filesToIndex;
        this.firstFileToIndex = firstFileToIndex;
        this.lastFileToIndex = lastFileToIndex;
    }

    protected String readTextFromFile(String path) throws IOException {
        return IOUtility.readTextFromFile(path);
    }

}
