package tests;

import com.example.logparser.controllers.LogFileController;
import com.example.logparser.modules.Constants;
import com.example.logparser.models.LogFile;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LogFileControllerTest {
    private final String testFile = "C:/Users/QuinnFreas/Logs/JDBC_log.log";
    @Test
    void splitLogByType() {
        LogFileController logFileController = new LogFileController();

        logFileController.addLogFile(Paths.get(testFile),true,true);
        LogFile logFile;

        do {
            System.out.print(".");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (Exception ignored) {}
            logFile = logFileController.getLogFileByIndex(0);
            // Keep checking till complete.
        } while (logFile.isLoading());
        logFileController.splitLogsByType(Constants.LOG_CATEGORIES.QID.name(), true,true, null);
    }

/*    @Test
    void combineAndDelete() {
        LogFileController logFileController = new LogFileController();

        logFileController.addLogFile(Paths.get(testFile),true,true);
        LogFile logFile;

        do {
            System.out.print(".");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (Exception ignored) {}
            logFile = logFileController.getLogFileByIndex(0);
            // Keep checking till complete.
        } while (logFile.isLoading());
        logFileController.splitLogsByType(Constants.LOG_CATEGORIES.QID.name(), true,true, null);
        logFileController.combineAndDelete();
    }*/

    @Test
    void getParent() {
    }

    private void createFile(Path path) {
        try {
            File f = new File(path.toString());

            if (!f.createNewFile()) {
                f.delete();
                createFile(path);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}