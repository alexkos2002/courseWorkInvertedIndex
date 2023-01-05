import org.junit.Before;
import org.junit.Test;
import utility.IOUtility;
import utility.PreProcUtility;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class SequentialAndParallelWordToFilePathsEqualityTest {

    private String[] sequentialResultFiles;
    private String[] parallelResultFiles;

    @Before
    public void init() {
        this.sequentialResultFiles = new String[]{"seq_results0.txt", "seq_results1.txt", "seq_results2.txt",
                "seq_results3.txt", "seq_results4.txt"};
        this.parallelResultFiles = new String[]{"par_results0.txt", "par_results1.txt", "par_results2.txt",
                "par_results3.txt", "par_results4.txt"};
    }

    @Test
    public void shouldSequentialAndParallelResultFilesBeEqual () {
        assertEquals(sequentialResultFiles.length, parallelResultFiles.length);
        int resultFilesPathsLength = sequentialResultFiles.length;
        boolean isAllFilePairsEqualFlag = true;
        for (int i = 0; i < resultFilesPathsLength; i++) {
            if (!IOUtility.comparePathsContainWordFiles(sequentialResultFiles[i], parallelResultFiles[i])) {
                isAllFilePairsEqualFlag = false;
                break;
            }
        }
        assertTrue(isAllFilePairsEqualFlag);
    }

}
