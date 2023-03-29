package com.example.logparser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class LogParserApplication extends Application {
    private static Stage _stage;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(LogParserApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);

        setStage(stage);
        _stage.setTitle("Hello!");
        _stage.setScene(scene);
        _stage.show();
    }


    public static void main(String[] args) {
        launch();
    }

    public static Stage getStage() { return _stage; }
    public static void setStage(Stage stage) { _stage = stage; }

}