package com.example.logparser.controllers;

import com.example.logparser.LogParserApplication;
import com.example.logparser.javafx.modules.IndexTree;
import com.example.logparser.javafx.modules.Utils;
import com.example.logparser.modules.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.example.logparser.javafx.models.CustomTab;
import com.example.logparser.models.JSONConstant;
import com.example.logparser.models.LogFile;
import com.example.logparser.javafx.models.TabMetadata;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;


public class LogParserController {
    protected static LogFileController logFileController = new LogFileController();
    protected final static TabMetadata tabMetadata = new TabMetadata();
    private final Map<Integer, CodeArea> codeAreasByMetadataId = new HashMap<>();



    //UI Objects

    @FXML
    private TextField filePathTF;

    @FXML
    private TabPane indexTabPane;

    @FXML
    private CheckBox orderByTimestampChB;

    @FXML
    private CheckBox decodeHexChB;

    @FXML
    private CheckBox filterNonASCIIChB;

    @FXML
    private ComboBox<String> actionTypeCoB;

    @FXML
    public
    TabPane logTabPane = new TabPane();

    @FXML
    private CheckBox wrapTextChB;

    @FXML
    private GridPane logPaneFilters;

//UI Object Handlers

    @FXML
    protected void onFileChooserButtonClick() {
        FileChooser fileChooser = new FileChooser();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(LogParserApplication.getStage());


        if (selectedFiles != null) {
            StringBuilder filePaths = new StringBuilder();
            for (File f : selectedFiles) {
                filePaths.append(f.getPath());
                filePaths.append(";");
            }

            filePathTF.setText(filePaths.toString());
        } else {
            filePathTF.setText("");
        }
    }

    @FXML
    protected void onLoadFileButtonClick() {
        if (filePathTF.getText().isEmpty()) {
            Utils.openAlertDialog(Constants.ACTION_TYPES.NO_FILE);
        } else {
            // Get the file paths from text box
            // This application does not currently support multi-file selection
            String[] paths = filePathTF.getText().split(";");

            // Iterate through each file path
            for (String p : paths) {
                String fileExtension = Utilities.getExtensionByStringHandling(p).isPresent() ? Utilities.getExtensionByStringHandling(p).get() : "";

                if (Arrays.stream(Constants.INVALID_EXTENSIONS).anyMatch(e -> e.equalsIgnoreCase(fileExtension))) {
                    Utils.openAlertDialog("Invalid Extension!", "The file extension \"" + fileExtension + "\" is not permitted.");
                    continue;
                }

                Path path = Paths.get(p);
                // Create a log file object for each given path
                logFileController.addLogFile(path,true,true);
            }

            refreshPage();
        }
    }

    @FXML
    protected void onDecodeHexBoxChange() {
        boolean decodeBool = decodeHexChB.selectedProperty().getValue();
        logFileController.decodeHexData(decodeBool, filterNonASCIIChB.selectedProperty().getValue());
        filterNonASCIIChB.setDisable(!decodeBool);
        refreshLogPane();
    }

    @FXML
    protected void onFilterNonASCIIChange() {
        onDecodeHexBoxChange();
    }

    @FXML
    protected void onWrapTextChange() {
        refreshLogPane();
    }

    @FXML
    protected void onSplitByConnectionButtonClick() {
        if (Utilities.isNullOrEmpty(actionTypeCoB.getValue())) {
            Utils.openAlertDialog(Constants.ACTION_TYPES.SPLIT);
            return;
        }

        actionDialog(Constants.ACTION_TYPES.SPLIT);
    }

    private void splitProcessor(ArrayList<String> actionFiles) {
        // Get the select value from dropdown
        Object v = actionTypeCoB.getValue();

        if (!Utilities.isNullOrEmpty(v)) {
            Thread t = new Thread(() -> {
                // If value exists, split file by selected type
                logFileController.splitLogsByType(v.toString().toUpperCase(Locale.ROOT), orderByTimestampChB.selectedProperty().getValue(),false, actionFiles);
                // Generate tabs for loaded files
                Platform.runLater(this::loadTabs);
            });

            t.start();

            logFileController.setProcessing(true);
            openProgressDialog();
        }
    }

    @FXML
    protected void onSortButtonClick() {
        if (Utilities.isNullOrEmpty(actionTypeCoB.getValue())) {
            Utils.openAlertDialog(Constants.ACTION_TYPES.SORT);
            return;
        }

        actionDialog(Constants.ACTION_TYPES.SORT);
    }

    private void sortProcessor(ArrayList<String> actionFiles) throws IncompatibleFileError {
        Object v = actionTypeCoB.getValue();

        if (!Utilities.isNullOrEmpty(v)) {
            Thread t = new Thread(() -> {
                boolean orderByTimestamp = orderByTimestampChB.selectedProperty().getValue();
                String selectedSortBy = v.toString().toUpperCase(Locale.ROOT);

                try {

                    if (selectedSortBy.equalsIgnoreCase(Constants.LOG_CATEGORIES.HTTP.toString())) {
                        sortHelper(Constants.LOG_CATEGORIES.QID.getSearchName().toUpperCase(),orderByTimestamp, actionFiles);
                        orderByTimestamp = false;
                    }

                    sortHelper(selectedSortBy,orderByTimestamp, actionFiles);
                } catch (IncompatibleFileError e ) {
                    Platform.runLater(() -> {
                        Utils.openAlertDialog("Error",e.getMessage());
                    });

                }

                System.out.println("Sort process complete");
            });

            t.start();

            logFileController.setProcessing(true);
            openProgressDialog();
        }
    }

    private void actionDialog(Constants.ACTION_TYPES actionType) {
        // Create a new popup stage
        Stage popupStage = new Stage();

        // Create a new scene with the tree view
        Scene scene = new Scene(actionDialogVBox(popupStage, actionType), 300, 200);

        popupStage.setTitle("Files to Process");
        popupStage.setScene(scene);

        // Display the popup
        popupStage.show();
    }

    private VBox actionDialogVBox(Stage primaryStage, Constants.ACTION_TYPES actionType) {
        TreeView<String> treeView = actionDialogFileList();

        Button okayBtn = new Button("Okay");
        Button cancelBtn = new Button("Cancel");

        okayBtn.setOnAction(e -> {
            List<String> selectedValues = treeView.getSelectionModel().getSelectedItems()
                    .stream()
                    .map(TreeItem::getValue)
                    .toList();

            LogHelper.setTotalMaxRecords(BigInteger.valueOf(0));
            logFileController.getLogFiles()
                    .forEach(l -> LogHelper.addTotalMaxRecords(l.getLineCount()));

            switch (actionType) {
                case SPLIT -> splitProcessor(new ArrayList<>(selectedValues));
                case SORT -> {
                    try {
                        sortProcessor(new ArrayList<>(selectedValues));
                    } catch (IncompatibleFileError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

            primaryStage.close();
            loadTabs();
        });

        cancelBtn.setOnAction(e -> primaryStage.close());

        Text message = new Text("Select files to process.");

        return new VBox(message, treeView, new HBox(okayBtn, cancelBtn));
    }


    private TreeView<String> actionDialogFileList() {
        Set<TreeItem<String>> selectedFiles = new HashSet<>();
        TreeItem<String> root = new TreeItem<>();
        TreeView<String> treeView = new TreeView<>(root);

        treeView.setShowRoot(false);

        for (LogFile l : logFileController.getLogFiles()) {
            TreeItem<String> fileItem = new TreeItem<>(l.getFileName());
            root.getChildren().add(fileItem);
        }

        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        treeView.setOnMouseClicked(event -> {
            // Get the clicked row
            TreeItem<String> item = treeView.getSelectionModel().getSelectedItem();
            if (item != null) {
                if (treeView.getSelectionModel().isSelected(treeView.getRow(item))) {
                    if (selectedFiles.contains(item)) {
                        selectedFiles.remove(item);
                        treeView.getSelectionModel().clearSelection(treeView.getRow(item));
                    } else {
                        selectedFiles.add(item);
                    }
                }
            }

            for (TreeItem<String> t : selectedFiles) {
                treeView.getSelectionModel().select(treeView.getRow(t));
            }
        });

        return treeView;
    }

    private void sortHelper(String selectedSortBy, boolean orderByTimestampValue, ArrayList<String> actionFiles) throws IncompatibleFileError {
        logFileController.splitLogsByType(selectedSortBy, orderByTimestampValue,true, actionFiles);
        logFileController.combineAndDelete();
        logFileController.clearMemoryFiles();
    }

    @FXML
    protected void onExitButtonClick() {
        Platform.exit();
    }

    @FXML
    protected void onDocumentationButtonClick() {
        String fileContent;
        try (InputStream is = LogParserApplication.class.getResourceAsStream("help.html")) {
            if (is == null) {
                throw new IOException("Resource not found");
            }
            fileContent = new String(is.readAllBytes());
        } catch (IOException e) {
            e.printStackTrace();
            fileContent = "<html><body>Error reading documentation file!</body></html>";
        }

        Stage popupStage = new Stage();

        WebView webView = new WebView();
        webView.getEngine().loadContent(fileContent);
        VBox.setVgrow(webView, Priority.ALWAYS);

        VBox vbox = new VBox(webView);

        Scene scene = new Scene(vbox, 820, 400);

        popupStage.setTitle("Documentation");
        popupStage.setScene(scene);

        popupStage.show();
    }


    private void openProgressDialog() {
        Stage owner = LogParserApplication.getStage();
        Dialog<String> dialog = new Dialog<>();
        dialog.setContentText("Processing.\nThis may take several minutes...");

        // Create a GaussianBlur effect
        GaussianBlur blur = new GaussianBlur(10); // 10 is the radius

        // When the dialog is shown, apply the blur effect to the owner
        dialog.setOnShown(event -> owner.getScene().getRoot().setEffect(blur));

        // When the dialog is hidden (closed), remove the blur effect from the owner
        dialog.setOnHidden(event -> owner.getScene().getRoot().setEffect(null));

        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initOwner(owner);
        dialog.getDialogPane().setPrefSize(300,175);

        LogHelper.setTimeStartNow();
        dialog.show();
        Thread thread = new Thread(() -> {
            while (logFileController.isProcessing()) {
                try {
                    // Change content here
                    Platform.runLater(() -> {
                        String m = LogHelper.getMark();
                        if (!m.isEmpty()) {
                            int progressPercent = (int) LogHelper.getProgressPercentage();
                            dialog.setContentText("Processing.\nThis may take several minutes..." +
                                    "\nOn file " + LogHelper.getProcessingFile() + " of " + LogHelper.getFilesToProcess() +
                                    "\nCompiling data for: "+m +
//                                    "\n"+ LogHelper.getStatus() +
                                    "\nPercent complete: " + progressPercent +
                                    "\nTime elapsed: " + LogHelper.getElapsedTime());
                        }
                    });

                    Thread.sleep(100); // Sleep for a while before checking again
                } catch (InterruptedException e) {
                    // The thread got interrupted, close the dialog
                    Platform.runLater(() -> {
                        dialog.close();
                        owner.getScene().getRoot().setEffect(null); // remove the blur when interrupted
                    });
                    return; // Stop the thread
                }

            }

            // Once processing is done, close the dialog
            Platform.runLater(() -> {
                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
                dialog.close();
                refreshPage();
            });
        });

        // Start the thread
        thread.start();
    }

    @FXML
    protected void onRefreshButtonClick() {
        // Trigger UI refresh
        refreshPage();
    }

    @FXML
    protected void onResetButtonClick() {
        // Clear every loaded object
        logFileController.clear();
        logFileController = new LogFileController();
        tabMetadata.clear();
        logTabPane.getTabs().clear();
        decodeHexChB.selectedProperty().set(false);
        filterNonASCIIChB.setDisable(true);
        filterNonASCIIChB.selectedProperty().set(true);
        orderByTimestampChB.selectedProperty().set(false);
        logPaneFilters.getChildren().clear();
        indexTabPane.getTabs().clear();
        LogHelper.clearFilters();
        refreshPage();
    }

    @FXML
    protected void onNextPageButtonClick() {
        // Get focused tab
        int activeTabIdx = getMetadataIdOfActiveTab();
        // Update the tab's page by getting the current page + 1
        if (tabMetadata.setTabCurrentPage(activeTabIdx, tabMetadata.getTabCurrentPage(activeTabIdx).getValue() + 1)) {
            // Generate tabs for loaded files
            refreshLogPane();
        }
    }

    @FXML
    protected void onPreviousPageButtonClick() {
        // Get focused tab
        int activeTabIdx = getMetadataIdOfActiveTab();
        // Update the tab's page by getting the current page - 1
        if (tabMetadata.setTabCurrentPage(activeTabIdx, tabMetadata.getTabCurrentPage(activeTabIdx).getValue() - 1)) {
            // Generate tabs for loaded files
            loadTabs();
        }
    }

    @FXML
    protected void onReloadCurrentFileIndexClick() {
        reindexFile();
    }

    @FXML
    protected void onSaveAsButtonClick() {
        Tab t = logTabPane.getSelectionModel().getSelectedItem();

        if (Utilities.isNull(t)) {
            Utils.openAlertDialog(Constants.ACTION_TYPES.NO_FILE);
            return;
        }

        Stage stage = LogParserApplication.getStage();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Log","*.log"));

        File file = fileChooser.showSaveDialog(stage);
        LogFile l = logFileController.getLogFileByID(Integer.parseInt(t.getId()));

        logFileController.writeToDisk(file.getPath(),l);
    }


//Handler Helper Methods
    private void refreshPage() {
//        updateCheckedFilters();
        // Generate tabs for loaded files
        loadTabs();
        // Generate index tabs
        compileIndexPane();
        // Update split type dropdown field
        addFileSplitTypes();
        // Generate filters
        addLogFiltersToPane();
    }

    public void refreshLogPane() {
        // Generate tabs for loaded files
        loadTabs();
    }

    private void reindexFile() {
        LogFile l = logFileController.getLogFileByID(logTabPane.getSelectionModel().getSelectedItem().getId());
        l.setAllowIndex(false);
        l.setAllowIndex(true);
        compileIndexPane();
    }

    private void addFileSplitTypes() {
        // Clear the current field of all data
        actionTypeCoB.getItems().clear();

        ArrayList<LogFile> logFiles = logFileController.getLogFiles();

        if (logFiles.isEmpty()) {
            return;
        }

        Thread t = new Thread(() -> {
            HashSet<String> uniqueDisplayNames = new HashSet<>();
            for (LogFile logFile : logFiles) {
                while (logFile.indexNotReady()) {
                    // Just keep checking...
                }
                for (JSONConstant c : Objects.requireNonNull(JSONUtils.getJSONList(JSONUtils.JSON_LISTS.ACTIONTYPES))) {
                    if (logFile.getDocHitCount(c.getPattern(), c.getDisplayName().toLowerCase(), false) > 0) {
                        uniqueDisplayNames.add(c.getDisplayName());
                    }
                }
            }
            Platform.runLater(() -> {
                actionTypeCoB.getItems().addAll(uniqueDisplayNames);
            });
        });
        t.start();
    }

    private VBox getLogDisplaySet(LogFile logFile, CustomTab tab) {
        int metadataID = logFile.getMetadataID();

        BigInteger pageStart = tabMetadata.getTabPageStart(metadataID);
        BigInteger pageEnd   = tabMetadata.getTabPageEnd(metadataID);

        CodeArea codeArea = createCodeArea(
                logFile.readLinesToString(pageStart, pageEnd, false),
                pageStart.intValueExact(),
                pageEnd.intValueExact()
        );

        tab.setCodeArea(codeArea); // <-- store reference for future refreshes

        tabMetadata.setTabPageEnd(metadataID, logFile.getMaxLineNumberFromRead(pageEnd));

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        scrollPane.prefHeightProperty().bind(Bindings.multiply(0.80, LogParserApplication.getStage().heightProperty()));

        HBox navigationButtons = getLogNavButtonSet(metadataID);
        HBox searchButtons = createLogSearchButtonSet(metadataID, codeArea);

        VBox container = new VBox(scrollPane, new HBox(navigationButtons, searchButtons, createOtherButtonSet()));
        container.prefHeightProperty().bind(Bindings.multiply(0.80, LogParserApplication.getStage().heightProperty()));

        return container;
    }

    private void refreshTabCodeArea(CustomTab tab, LogFile logFile) {
        int metadataID = tab.getMetadataID();

        BigInteger pageStart = tabMetadata.getTabPageStart(metadataID);
        BigInteger pageEnd   = tabMetadata.getTabPageEnd(metadataID);

        CodeArea codeArea = tab.getCodeArea();
        if (codeArea == null) return;

        String text = logFile.readLinesToString(pageStart, pageEnd, false);
        codeArea.replaceText(text);
        codeArea.setWrapText(wrapTextChB.selectedProperty().getValue());

        // If your line-number gutter depends on pageStart, re-apply it here
        applyLineNumberGutter(codeArea, pageStart.intValueExact(), pageEnd.intValueExact());

        tabMetadata.setTabPageEnd(metadataID, logFile.getMaxLineNumberFromRead(pageEnd));
    }


    private CodeArea createCodeArea(String lines, int pageStart, int pageEnd) {
        CodeArea codeArea = new CodeArea(lines);

        applyLineNumberGutter(codeArea, pageStart, pageEnd);

        codeArea.setWrapText(wrapTextChB.selectedProperty().getValue());
        codeArea.setEditable(false);

        return codeArea;
    }

    private void applyLineNumberGutter(CodeArea codeArea, int pageStart, int pageEnd) {
        // If pageStart is 1-based (page 2 starts at 251), use:
        final int offset = pageStart; // absolute first line number of this page

        IntFunction<Node> graphicFactory = localLineIndex -> {
            int displayNumber = offset + localLineIndex; // 251 + 0 = 251

            // If you want to hide anything beyond pageEnd, do it safely:
            if (displayNumber > pageEnd-1) {
                return null;
            }

            Label label = new Label(Integer.toString(displayNumber));
            label.setMinWidth(55); // gutter width
            label.setAlignment(Pos.CENTER_LEFT);
            label.getStyleClass().add("line-number"); // optional CSS hook
            label.setTextFill(Color.GRAY);

            HBox hbox = new HBox(label);
            hbox.setSpacing(1);
            hbox.setAlignment(Pos.CENTER);

            if (localLineIndex == 0) {
                Rectangle rectangle = new Rectangle();
                rectangle.setFill(Color.WHITE);
                rectangle.widthProperty().bind(hbox.widthProperty().subtract(2));
                rectangle.heightProperty().bind(codeArea.heightProperty());
                StackPane.setAlignment(rectangle, Pos.TOP_LEFT);
                return new StackPane(rectangle, hbox);
            }

            return new StackPane(hbox);
        };

        codeArea.setParagraphGraphicFactory(graphicFactory);
    }


    private HBox createOtherButtonSet() {
        Button reloadIndex = createButton("Reload Index", e -> onReloadCurrentFileIndexClick());
        reloadIndex.setTooltip(new Tooltip("Recreates index for focused log file."));
        return new HBox(reloadIndex);
    }

    private HBox createLogSearchButtonSet(int uniqueID, CodeArea codeArea) {
        AtomicReference<String> word = new AtomicReference<>("");
        // Create search button
        Button searchButton = new Button("Search");
        // Create search field
        TextField searchField = new TextField(word.get());
        // Create found count
        Text foundCountText = new Text();
        // Create search position
        AtomicInteger searchPosition = new AtomicInteger(0);



        // On click...
        searchButton.setOnAction((ActionEvent e) -> {
            // Get the index results from the search
            LogFile logFile = logFileController.getLogFileByID(uniqueID);
            String keyword = searchField.getText();
            word.set(keyword);

//            keyword = "*"+keyword+"*"; // TODO: add whole word button

            /*int resultIndex = logFile.getLineHitCount("",keyword,false);

            if (resultIndex == 0) {
                resultIndex = logFile.getDocHitCount("",keyword, true);
            }*/

            logFile.setMaxLines(1000);
            ArrayList<Long> resultMarker = logFile.getHitLines("",keyword,false);

            // Create a boolean to indicate if code area needs to be searched again
            boolean restartSearch;
            // Search may need to run multiple times, depending if the code area changes
            do {
//                 Perform search with tab ID, code area, search word, search position, the list of found indexes, and the found count text
                restartSearch = performSearch(uniqueID, codeArea, searchField.getText(), searchPosition, resultMarker, foundCountText);
            } while (restartSearch);

            // Update the "found" field with the current search position and how many items were found
//            foundCountText.setText(searchPosition + "/" + resultIndex);
        });

        // Replace search buttons with an updated set
        HBox hbox = new HBox(searchField, searchButton, foundCountText);
        hbox.setSpacing(1.5);

        return hbox;
    }

    private String wholeWord(String input) {
        return "\""+input+"\"";
    }

    public boolean performSearch(int uniqueID, CodeArea codeArea, String searchText, AtomicInteger searchPosition,
                                 List<Long> resultIndex, Text foundCountText) {
        // Set bool to false, the application should not restart the search by default
        boolean restartSearch = false;
        // Get the current caret position from the code area
        int fromIndex = codeArea.getCaretPosition();

        // If the search has gone beyond the visible text of the code area, set the index to 0
        if (fromIndex >= codeArea.getText().length()) {
            fromIndex = 0;
        }

        // Find where the index of the search word is
        int index = codeArea.getText().toLowerCase(Locale.ROOT).indexOf(searchText.toLowerCase(), fromIndex);

        if (foundCountText.getText().equals(Constants.ACTION_TYPES.DBKCLK.name())) {
            index = codeArea.getText().toLowerCase(Locale.ROOT).indexOf(searchText.toLowerCase(), fromIndex);
        }

        // Check if anything was found in the code area
        if (index >= 0) {
            // Get the length of the search word
            int length = searchText.length();
            // Request that the visible scope follows the code area caret
            codeArea.requestFollowCaret();
            // Highlights the found text
            codeArea.selectRange(index, index + length);
            // Request that the code area remain in focus
            codeArea.requestFocus();
            // Increment the atomic search position/index
            if (searchPosition != null) {
                searchPosition.getAndIncrement();
            }
            // Update the search index and found results
//            foundCountText.setText(searchPosition + "/" + resultIndex.size());
        } else {
            // If no word was found in code area, go to the next known result
            for (Long resultPosition : resultIndex) {
                // Get the page of the next found value in the search
                int nextPage = tabMetadata.getPageOfLine(uniqueID, resultPosition);
                // Get the current page of the tab
                int currentPage = tabMetadata.getTabCurrentPage(uniqueID).getValue();

                // If the current page is less than the next page, set the tab to the next page and indicate the search needs to be restart
                if (currentPage < nextPage) {
                    tabMetadata.setTabCurrentPage(uniqueID, nextPage);
                    restartSearch = true;
                    break;
                }
            }

            // If restartSearch is false, there are no more search results and the tab should go back to the first page
            if (!restartSearch) {
                tabMetadata.setTabCurrentPage(uniqueID, 1);
                searchPosition.set(0);
            }
            // Since nothing was found, do not highlight any text
            codeArea.selectRange(0, 0);
            // Replace the existing text of the code area
            codeArea.replaceText(logFileController.getLogFileByID(uniqueID).readLinesToString(tabMetadata.getTabPageStart(uniqueID), tabMetadata.getTabPageEnd(uniqueID), false));
            refreshLogPane();
        }
        // Return boolean
        return restartSearch;
    }



    private HBox getLogNavButtonSet(int metadataID) {
        // Create back button
        Button backButton = createButton("Back", e -> onPreviousPageButtonClick());
        // Create next button
        Button nextButton = createButton("Next", e -> onNextPageButtonClick());
        // Create jump text field
        TextField pageJumpTF = createPageJumpTextField(metadataID);
        // Create jump button
        Button pageJumpButton = createButton("Go", e -> {
            // Jump handle sets the tab current page to the page in the text field
            String jumpStr = pageJumpTF.getText();
            if (!jumpStr.isEmpty()) {
                tabMetadata.setTabCurrentPage(metadataID, Integer.parseInt(jumpStr));
            }
            pageJumpTF.clear();
            // Generate tabs for loaded files
            loadTabs();
        });

                // Display the avaiable pages based on the loaded content
        Text availablePages = new Text(String.valueOf(tabMetadata.getTabPagesAvailable(metadataID)));
        // Set alignment
        availablePages.setTextAlignment(TextAlignment.CENTER);

        HBox jumperBox = new HBox(new Text("Page:"),pageJumpTF, new Text(" of: "), availablePages, pageJumpButton);
        jumperBox.setSpacing(1.5);
        jumperBox.setAlignment(Pos.CENTER);
        jumperBox.setPadding(new Insets(0,0,0,3));
        // Put everything in an HBox
        HBox h = new HBox(backButton, nextButton, jumperBox);
        h.setSpacing(1.5);
        h.setAlignment(Pos.CENTER);
        h.setPadding(new Insets(0,20,0,0));

        // Return HBox
        return h;
    }

    private Button createButton(String text, EventHandler<ActionEvent> eventHandler) {
        // Create generic button
        Button button = new Button(text);
        button.setOnAction(eventHandler);
        return button;
    }

    private TextField createPageJumpTextField(int metadataID) {
        TextField textField = new TextField();
        textField.setMaxWidth(50);

        textField.promptTextProperty().bind(
                tabMetadata.getTabCurrentPage(metadataID).asString()
        );

        return textField;
    }


    private void loadTabs() {
        ArrayList<LogFile> lf = logFileController.getLogFiles();
        ObservableList<Tab> logTabs = logTabPane.getTabs(); // pull once

        for (LogFile l : lf) {
            BigInteger recordCount = l.getLineCount();

            if (!tabMetadata.tabMetadataExists(l.getMetadataID())) {
                l.setMetadataID(tabMetadata.addTabMetadata());
            }

            tabMetadata.calculateTabPagesAvailable(
                    l.getMetadataID(),
                    recordCount.subtract(BigInteger.valueOf(l.getHiddenLines()))
            );

            String fileName = l.getLocalPath().getFileName().toString();

            // ---------- CREATE TAB IF IT DOESN'T EXIST ----------
            if (logTabs.stream().noneMatch(f -> f.getText().equalsIgnoreCase(fileName))) {

                // Create tab with placeholder content first (so we can pass the tab into getLogDisplaySet)
                CustomTab tab = new CustomTab(l.getMetadataID(), fileName, new VBox());

                // Build the UI ONCE and store the CodeArea reference on the tab inside getLogDisplaySet(...)
                tab.setContent(getLogDisplaySet(l, tab));

                tab.setOnClosed(event -> {
                    int idx = logFileController.getLogFileIdx(tab.getText());
                    logFileController.removeLogFile(idx);
                    refreshPage();
                });

                logTabs.add(tab);

            } else {
                // ---------- TAB EXISTS: REFRESH ONLY THE CODEAREA ----------
                for (Tab t : logTabs) {
                    if (!(t instanceof CustomTab x)) {
                        continue;
                    }

                    if (x.getMetadataID() == l.getMetadataID()) {
                        // DO NOT replace the whole VBox anymore:
                        // t.setContent(getLogDisplaySet(l));  <-- remove this

                        refreshTabCodeArea(x, l); // <-- updates codeArea.replaceText(...) only
                        break;
                    }
                }
            }
        }
    }


    private int getMetadataIdOfActiveTab() {
        CustomTab t = (CustomTab) logTabPane.getSelectionModel().getSelectedItem();
        return t.getMetadataID();
    }
    private int getIndexOfTabByMetaID(int metadataID) {
        ObservableList<Tab> tabs = logTabPane.getTabs();
        for (int i = 0; i < tabs.size(); i++) {
            CustomTab ct = (CustomTab) tabs.get(i);

            if (ct.getMetadataID() == metadataID) {
                return i;
            }
        }

        return -1;
    }

    private static IndexTree idxTree;

    private void compileIndexPane() {
        String category = "Index";
        int tabIndex = 0;

        if (indexTabPane.getTabs().stream().noneMatch(f -> f.getText().equals(category))) {
            Tab tab = new Tab(category,new Text("Generating index..."));
            tab.setClosable(false);
            indexTabPane.getTabs().add(tab);
            tabIndex = indexTabPane.getTabs().size() - 1;
        }

        int finalTabIndex = tabIndex;
        Thread t = new Thread(() -> {

            for (LogFile l : logFileController.getLogFiles()) {
                while (l.isLoading()) {
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException ignored) {}
                }
                l.setMaxLines(10000000);
            }

            idxTree = new IndexTree();
            idxTree.indexTree(this);
            Platform.runLater(() -> {
                Tab tab = new Tab(category,idxTree.getSearchableTree());
                tab.setClosable(false);
                indexTabPane.getTabs().set(finalTabIndex, tab);

                for (LogFile l : logFileController.getLogFiles()) {
                    l.setMaxLines(Constants.MAX_LINES_DEFAULT);
                }
            });
        });

        t.start();
    }

    public ObservableList<Tab> getTabsForIndex() {
        return logTabPane.getTabs();
    }

    /*START FILTER PANE CODE*/
    private void addLogFiltersToPane() {
        HashMap<String,String> littleFilters = new HashMap<>(); // pattern : name

        for (JSONConstant constant : Objects.requireNonNull(JSONUtils.getJSONList(JSONUtils.JSON_LISTS.FILTERS))) {
            littleFilters.put(constant.getPattern(),Utilities.capitalizeFirstLetter(constant.getDisplayName()));
        }

        Set<String> filtersToAdd = new HashSet<>();

        Thread t = new Thread(() -> {
            for (LogFile l : logFileController.getLogFiles()) {
                while (l.indexNotReady()) {/*Do nothing*/}
                Platform.runLater(() -> {
                    updateFiltersBasedOnLog(l, littleFilters, filtersToAdd);
                });
            }

            Platform.runLater(() -> {
                for (String s : filtersToAdd) {
                    addFilterCheckBox(s);
                }
                addUtilityCheckBox();
            });
        });

        t.start();
    }

    private void updateFiltersBasedOnLog(LogFile l, HashMap<String,String> littleFilters, Set<String> filtersToAdd) {
        int maxLinesHold = l.getMaxLines();
        l.setMaxLines(1);
        for (Map.Entry<String, String> m : littleFilters.entrySet()) {
            String x = wholeWord(m.getKey());
            int hitCount = l.getHitLines("", x, false).size();

            if (hitCount > 0 && filtersToAdd.add(m.getValue())) {
                addFilterCheckBox(m.getValue());
            }
        }

        l.setMaxLines(maxLinesHold);
    }

    private void addFilterCheckBox(String s) {
        if (checkBoxExists(s)) {
            return; // If checkbox with this string already exists, don't add a new one
        }

        CheckBox checkBox = new CheckBox(s);

        boolean propValue = LogHelper.getFilters().stream().noneMatch(f -> f.equalsIgnoreCase(s));
        checkBox.selectedProperty().set(propValue);

        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                LogHelper.addFilter(s);
            } else {
                LogHelper.removeFilter(s);
            }
            refreshLogPane();
        });

        // Add the CheckBox to the GridPane
        addCheckBoxToPane(checkBox);
    }

    private void addCheckBoxToPane(CheckBox checkBox) {
        if (JSONUtils.isJsonError()) {
            logPaneFilters.add(new Text("Failed to load JSON filters. Please check file.\n" + JSONUtils.getJsonErrorMessage()),0,0);
            return;
        }

        ObservableList<Node> existingCheckboxes = logPaneFilters.getChildren();

        int column;
        int row;
        int count = 0;


        for (int i = 0; i < existingCheckboxes.size(); i++) {
            Node n = existingCheckboxes.get(i);
            if (n instanceof CheckBox c) {
                if (c.getText().equalsIgnoreCase(Constants.UTILITY_CHECKBOX_NAME)) {
                    logPaneFilters.getChildren().remove(i);
                    i--;
                } else if (c.getText().equalsIgnoreCase(checkBox.getText())) {
                    return;
                } else {
                    count++;
                }
            }
        }

        // Calculate position of new checkbox based on count
        column = count / 3;
        row = count % 3;

        logPaneFilters.add(checkBox, column, row);
    }

    private boolean checkBoxExists(String text) {
        for (Node n : logPaneFilters.getChildren()) {
            if (n instanceof CheckBox c) {
                if (c.getText().equals(text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addUtilityCheckBox() {
        String text = Constants.UTILITY_CHECKBOX_NAME;

        if (!checkBoxExists(text)) {
            CheckBox b = new CheckBox(text);
            b.setOnAction(e -> {
                for (Node n : logPaneFilters.getChildren()) {
                    if (!(n instanceof CheckBox c)) continue;
                    c.selectedProperty().set(b.selectedProperty().getValue());
                }
            });
            b.setSelected(true);
            addCheckBoxToPane(b);
        }
    }
    /*END FILTER PANE CODE*/
}