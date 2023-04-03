package tests;

import controllers.LogFileController;
import models.LogFile;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LogFileControllerTest {
    @Test
    void logFileController() {
        LogFileController l = new LogFileController();
        assertNotEquals(null,l);
    }

    @Test
    void appendLogFileContents() {
        LogFileController l = new LogFileController();
        l.addLogFile("log/file/path",null);
        l.appendLogFileContents("Hello world",1,"log/file/path");

        assertEquals("Hello world",l.getLogFile(0).getLogData().get(1));
    }

    @Test
    void getLogFiles() {
        LogFileController l = new LogFileController();

        assertEquals(0,l.getLogFiles().size());

        l.addLogFile("log/file/path",null);

        assertEquals(1,l.getLogFiles().size());
    }

    @Test
    void getLogFile() {
        LogFileController l = new LogFileController();

        l.addLogFile("log/file/path",null);
        l.addLogFile("log/file/path/2",null);

        assertEquals("log\\file\\path\\2",l.getLogFile(1).getLocalPath());
        assertEquals("log\\file\\path",l.getLogFile(0).getLocalPath());
    }

    @Test
    void getLogFileIdx() {
        LogFileController l = new LogFileController();

        l.addLogFile("log/file/path",null);
        l.addLogFile("log/file/path/2",null);

        assertEquals(1,l.getLogFileIdx("log/file/path/2"));
        assertEquals(0,l.getLogFileIdx("log/file/path"));
    }

    @Test
    void addLogFile() {
        LogFileController l = new LogFileController();

        l.addLogFile("testFiles/ADO_log.log",null);

        assertNotEquals(null,l.getLogFile(0));
    }

    @Test
    void testAddLogFile() {
        LogFileController l = new LogFileController();

        l.addLogFile("testFiles/ADO_log.log",null,"Friendly Name");

        assertNotEquals(null,l.getLogFile(0));
    }

    @Test
    void splitLogByType() {
        LogFileController l = new LogFileController();
        HashMap<Integer,String> m = new HashMap<>();

        m.put(1,"2023-02-10T16:35:54.838-05:00	2	[3|Q-Id]	[HTTP|Req: 0] HTTP/1.1 200 OK, 90 Bytes Transferred");
        m.put(2,"2023-02-10T16:35:54.896-05:00	2	[3|Q-Id]	[HTTP|Res: 0] Request completed in 792 ms.");
        m.put(3,"2023-02-10T16:35:55.025-05:00	2	[3|Q-Id]	[HTTP|Req: 1] POST http://localhost:8765");
        m.put(4,"2023-02-10T16:35:55.028-05:00	5	[3|Q-Id]	[HTTP|Res: 1] [Reused]");

        l.addLogFile("testFiles/splitLogTest.log",m,"splitLogTest");
        l.indexLogData(0);

        assertEquals(1,l.getLogFiles().size());

        l.splitLogByType("HTTP_REQUEST");

        assertEquals(3,l.getLogFiles().size());
    }

    @Test
    void testSplitLogByType() {
    }

    @Test
    void storeMemoryFilesToDisk() {
        LogFileController l = new LogFileController();

        HashMap<Integer,String> m = new HashMap<>();
        m.put(1,System.currentTimeMillis()+ ": File saved to disk");

        l.addLogFile("testFiles/ADO_log.log",m,"toDiskTest");
        int idx = l.getLogFileIdx("testFiles/ADO_log.log");

        l.storeMemoryFilesToDisk(idx);

        idx = l.getLogFileIdx("testFiles/toDiskTest.log"); // New file name

        assertEquals(m.get(1),l.getLogFile(idx).getLogData().get(1));

    }

    @Test
    void loadFilesToMemory() {
        LogFileController l = new LogFileController();

        l.addLogFile("testFiles/ADO_log.log",null);
        int idx = l.getLogFileIdx("testFiles/ADO_log.log");

        assertEquals(0,l.getLogFile(idx).getLogData().size());

        l.loadFilesToMemory();

        do { // Need to give the file time to load
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (Exception e) {}
        } while (l.getLogFile(idx).isLoading());

        assertNotEquals(0,l.getLogFile(idx).getLogData().size());
    }

    @Test
    void decodeHexData() {
        LogFileController l = new LogFileController();

        HashMap<Integer,String> m = new HashMap<>();
        m.put(1,"48 65 6C 6C 6F 20 77 6F 72 6C 64"); // Hello world

        l.addLogFile("path/to/file",m);
        int idx = l.getLogFileIdx("path/to/file");

        assertEquals(m.get(1),l.getLogFile(idx).getLogData().get(1));

        l.decodeHexData(idx);
        assertEquals("Hello world",l.getLogFile(idx).getLogData(1));
    }

    @Test
    void clear() {
        LogFileController l = new LogFileController();

        l.addLogFile("testFiles/ADO_log.log",null,"Friendly Name");

        assertEquals(1,l.getLogFiles().size());

        l.clear();
        assertEquals(0,l.getLogFiles().size());
    }
}