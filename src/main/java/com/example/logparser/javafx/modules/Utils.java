package com.example.logparser.javafx.modules;

import com.example.logparser.LogParserApplication;
import com.example.logparser.modules.Constants;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Utils {

    public static void openAlertDialog(Constants.ACTION_TYPES a) {
        switch (a) {
            case SPLIT, SORT ->
                    openAlertDialog("Unable to Complet Action!", "Please select a type to " + a.name().toLowerCase() + " by.\nClick Refresh button if list is empty.");
            case NO_FILE -> openAlertDialog("No File Chosen!", "Please choose a file to load.");
            default -> openAlertDialog("Alert!", "Contact developer.");
        }
    }

    public static void openAlertDialog(String title, String message) {
        Stage owner = LogParserApplication.getStage();
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);

        // Create a VBox to center the content text
        VBox contentBox = new VBox();
        contentBox.setAlignment(Pos.CENTER); // Centers the children nodes
        contentBox.setSpacing(10); // If you need spacing between nodes

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        contentBox.getChildren().add(messageLabel);

        dialog.getDialogPane().setContent(contentBox);

        // Create a GaussianBlur effect
        GaussianBlur blur = new GaussianBlur(10); // 10 is the radius

        // When the dialog is shown, apply the blur effect to the owner
        dialog.setOnShown(event -> owner.getScene().getRoot().setEffect(blur));

        // When the dialog is hidden (closed), remove the blur effect from the owner
        dialog.setOnHidden(event -> owner.getScene().getRoot().setEffect(null));

        dialog.initOwner(owner);
        dialog.getDialogPane().setPrefSize(300,175);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

        dialog.show();
    }
}
