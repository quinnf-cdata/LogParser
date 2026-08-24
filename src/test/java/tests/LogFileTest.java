package tests;

import com.example.logparser.models.LogFile;
import com.example.logparser.modules.LogHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class LogFileTest {
    private final String testFileName = "test_file.log";
    private final String testFileDirectory = "./src/test/java/tests/";
    private final String testFile = testFileDirectory + "/" + testFileName;
    private final Path path = Paths.get(testFile);
    private LogFile logFile = new LogFile(testFile, true);

    private boolean initiateLogFile(boolean waitForIndex) {
        if (logFile == null) {
            logFile = new LogFile(testFile, true);
        }

        logFile.setAllowIndex(true);
        do {
            if (waitForIndex) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception ignored) {}
            }
        } while (logFile.indexNotReady());

        return true;
    }

    @Test
    void createLogFileString() {
        LogFile logFile = new LogFile(testFile);
        assertEquals(logFile.getFileName(),testFileName);
    }

    @Test
    void createLogFilePath() {
        LogFile logFile = new LogFile(path);
        assertEquals(logFile.getFileName(),testFileName);
    }

    @Test
    void testLogFileIndexCreation() {
        initiateLogFile(true);
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (Exception ignored) {}

        assertNotEquals(null,logFile.getIndex());
    }

    @Test
    void metadataIdTest() {
        initiateLogFile(false);
        // This should be -1 as metadata ID is used for JavaFX Tabs
        assertEquals(-1,logFile.getMetadataID());

        logFile.setMetadataID(1);

        assertEquals(1,logFile.getMetadataID());
    }

    @Test
    void indexTest() {
        initiateLogFile(true);
        assertNotEquals(null,logFile.getIndex());

        logFile.setAllowIndex(false);
        assertEquals(null,logFile.getIndex());
    }

    @Test
    void isMasterTest() {
        initiateLogFile(false);

        // All files are not set to master by default
        assertEquals(false, logFile.isMaster());

        logFile.setMaster(true);
        assertEquals(true,logFile.isMaster());
    }

    @Test
    void parentFileTest() {
        initiateLogFile(false);
        assertEquals(null,logFile.getParentFile());

        logFile.setParentFile(testFile);
        assertEquals(testFile,logFile.getParentFile());
    }

    @Test
    void readLinesTest() {
        initiateLogFile(false);
        assertEquals(
                "2024-02-29T13:58:41.176-0500\t1\t[2|Q-Id]\t[INFO|Connec] Closed Excel connection\n" +
                "2024-02-29T13:58:41.196-0500\t4\t[3|Q-Id]\t[INFO|Connec] Success: (0 ms)\n",
                logFile.readLinesToString(BigInteger.valueOf(22),BigInteger.valueOf(24),false));

        assertEquals(
                "203\t2024-02-29T13:58:42.943-0500\t2\t[6|Q-Id]\t[EXEC|Messag] Executed query: [SELECT * FROM [Excel].[Dispatched_Delivered_Report_Raw] LIMIT 1] Success: 1 results (1,697 ms)\n",
                logFile.readLinesToString(BigInteger.valueOf(203),BigInteger.valueOf(204),true));

        assertEquals("", logFile.readLinesToString(BigInteger.valueOf(203),BigInteger.valueOf(10),true));
    }

    @Test
    void maxLineNumberTest() {
        initiateLogFile(false);
        assertEquals(BigInteger.valueOf(248),logFile.getMaxLineNumberFromRead(BigInteger.valueOf(500)));
    }

    @Test
    void hexTest() {
        initiateLogFile(false);
        assertEquals("Hello world",logFile.hexToAscii("48 65 6c 6c 6f 20 77 6f 72 6c 64"));
        assertEquals("Hello world",logFile.hexToAscii("0x48 0x65 0x6c 0x6c 0x6f 0x20 0x77 0x6f 0x72 0x6c 0x64"));
        assertEquals("Hex String: Hello world",logFile.hexToAscii("Hex String: 0x48 0x65 0x6c 0x6c 0x6f 0x20 0x77 0x6f 0x72 0x6c 0x64"));
        assertEquals("My String: Hello world",logFile.hexToAscii("My String: 48 65 6c 6c 6f 20 77 6f 72 6c 64"));
        assertEquals("Numeric Value: 156",logFile.hexToAscii("Numeric Value: 156"));
        assertEquals("Numeric Value: 48",logFile.hexToAscii("Numeric Value: 48"));
        assertEquals("Numeric Value: 65.48",logFile.hexToAscii("Numeric Value: 65.48"));
        assertEquals("48. Title",logFile.hexToAscii("48. Title"));
        assertEquals("65 Title",logFile.hexToAscii("65 Title"));
    }

    @Test
    void queryIndexToHierarchyTest() {
        /*logFile = null;
        initiateLogFile(true);
        ArrayList<String> indexHierarchy = logFile.queryIndexToHierarchy("","");
        assertTrue(indexHierarchy.contains("[3|Q-Id]/Connection String/mfatoken=*****;"));


        indexHierarchy = logFile.queryIndexToHierarchy("","Closed Excel connection");
        // This will return false because the string is not valid for the index.
        assertFalse(indexHierarchy.contains("Closed Excel connection"));
*/
    }

    @Test
    void getDocHitCountTest() {
        initiateLogFile(true);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            logFile.getDocHitCount("","[8|Q-Id]",false);
        });

        String expectedStartMessage = "org.apache.lucene.queryparser.classic.ParseException: Cannot parse '[8|Q-Id]': Encountered \" \"]\" \"] \"\" at line 1, column 7.";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.startsWith(expectedStartMessage));

        // Docs hit does not mean actual hits. Results will be greater
//        assertEquals(45,logFile.getDocHitCount("CONTENT","*[8|Q-Id]",true));
    }

    @Test
    void getHitLinesTest() {
        initiateLogFile(true);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            logFile.getHitLines("","[8|Q-Id]",false);
        });

        String expectedStartMessage = "org.apache.lucene.queryparser.classic.ParseException: Cannot parse '[8|Q-Id]': Encountered \" \"]\" \"] \"\" at line 1, column 7.";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.startsWith(expectedStartMessage));

        LogHelper.setExactWordSearch(true);
        ArrayList<Long> arrayList = logFile.getHitLines("","[META|Schema]",false);
        assertEquals(74,arrayList.size());

        LogHelper.setExactWordSearch(false);
        arrayList = logFile.getHitLines("","[META|Schema]",true);
        assertEquals(100,arrayList.size());
    }

    @Test
    void getLineCountTest() {
        initiateLogFile(false);
        assertEquals(BigInteger.valueOf(248),logFile.getLineCount());
    }
}
