package com.example.logparser;

import com.example.logparser.controllers.LogFileController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.example.logparser.models.LogFile;
import com.example.logparser.modules.JSONUtils;
import org.json.JSONException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class LogParserApplication extends Application {
    private static Stage _stage;

    private static String _path;
    private static String _splitType;
    public static boolean jsonError = false;

    @Override
    public void start(Stage stage) throws IOException {
        JSONUtils.readExternalJSON();

        FXMLLoader fxmlLoader = new FXMLLoader(LogParserApplication.class.getResource("main-view.fxml"));
        Scene scene = fxmlLoader.load();

        setStage(stage);
        _stage.setTitle("CData Log Analyzer");
        _stage.getIcons().add(new Image("file:./com/example/logparser/taskbar_icon.png"));
        _stage.setScene(scene);
        _stage.show();
    }


    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (s.equalsIgnoreCase("-f")) {
                i++;
                _path = args[i];
            } else if (s.equalsIgnoreCase("-s")) {
                i++;
                _splitType = args[i];
            } else if (s.equalsIgnoreCase("-h")) {
                System.out.println("Options and arguments (and corresponding environment variables):");
                System.out.println("-f\t:\tAllows a specified file to be loaded to the application.\n\t\tThis doesn't do anything right now except with -s.");
                System.out.println("-s\t:\tSpecifies the split type.\n\t\tNote: supplying this argument will split a file without loading the GUI.\n\t\tAvailable split types are HTTP and Q-Id.\n\t\tSplitting a file will create new files in the directory of the source.");
                return;
            } else {
                System.out.println("Unknown argument \""+s+"\". Please use -h for available arguments.");
                return;
            }

        }

        if (_splitType != null) {launchSplitType(); return;}
        launch();
    }

    private static void launchSplitType() {
        if (_path == null) {
            System.out.println("File path was not specified.");
            return;
        }

        LogFileController logFileController = new LogFileController();
        logFileController.addLogFile(Paths.get(_path),true);

        LogFile l = logFileController.getLogFileByIndex(0);

        System.out.print("Waiting for file to load.");
        do {
            System.out.print(".");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (Exception ignored) {}
        } while (l.isLoading());
        System.out.println();

        logFileController.splitLogsByType(_splitType,true, true, null);
    }

    public static Stage getStage() { return _stage; }
    public static void setStage(Stage stage) { _stage = stage; }

}
