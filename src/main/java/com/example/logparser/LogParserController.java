package com.example.logparser;

import controllers.LogFileController;
import controllers.SearchController;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import models.FileIndex;
import models.LogFile;
import models.TabMetadata;
import org.fxmisc.richtext.*;
import org.fxmisc.flowless.*;


public class LogParserController {
    private LogFileController logFileController = new LogFileController();
    private TabMetadata tabMetadata = new TabMetadata();

    @FXML
    private TextField filePath;

    @FXML
    protected void onFileChooserButtonClick() {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(LogParserApplication.getStage());

        if (selectedFile != null) {
            filePath.setText(selectedFile.getPath());
        } else {
            filePath.setText("");
        }
    }

    @FXML
    private CodeArea logOutput;

    @FXML
    private CodeArea foundConnections;

    @FXML
    protected void onLoadFileButtonClick() {
        if (filePath.getText().isEmpty()) {
//            logOutput.insertText(0,"No file specified...");
        } else {
            String[] paths = filePath.getText().split(";");

            for (String p : paths) {
                Path path = Paths.get(p);
                logFileController.addLogFile(path.toString(), null, path.getFileName().toString());
                logFileController.loadFilesToMemory();
            }
            getTabs();
            compileIndex();
        }
    }

    @FXML
    protected void onDecodeHexButtonClick() {
        List<LogFile> lf = logFileController.getLogFiles();
        int size = lf.size();

        for (int i = 0; i < size; i++) {
            logFileController.decodeHexData(i);
        }
    }

    @FXML
    private TabPane indexTabPane;

    @FXML
    private CheckBox groupByType;

    private void compileIndex() {
        if (!(indexTabPane.getTabs() == null)) {
            indexTabPane.getTabs().clear();
        }
        List<FileIndex.Classification> classifications = Arrays.asList(FileIndex.Classification.values());
        List<String> indexTabs = new ArrayList<>();

        for (LogFile l : logFileController.getLogFiles()) {
            for (FileIndex.Classification cc : classifications) {
                String indexString;
                String tabTitle = "";

                if (groupByType.selectedProperty().getValue() && !cc.groupOnly) {
                    indexString = l.toString(cc);
                    tabTitle = cc.toString();
                } else {
                    indexString = l.toString(cc.group);
                    for (FileIndex.GroupName gn : FileIndex.GroupName.values()) {
                        if (cc.group == gn.groupID) {
                            tabTitle = gn.friendlyName;
                            break;
                        }
                    }
                }

                if (!indexTabs.contains(tabTitle)) {
                    indexTabs.add(tabTitle);

                    if (!indexString.isEmpty() && l.searchIndexByClassification(cc).size() >= 1) {
                        indexTabPane.getTabs().add(new Tab(tabTitle, new VirtualizedScrollPane(new CodeArea(indexString))));
                    }
                }
            }
        }
    }

    @FXML
    private ComboBox fileSplitType;

    private void addFileSplitTypes() {
        if (!fileSplitType.getItems().isEmpty()) {
            fileSplitType.getItems().clear();
        }

        for (LogFile l : logFileController.getLogFiles()) {
            for (FileIndex.Classification c : FileIndex.Classification.values()) {
                if (!l.searchClassificationIndex(c).isEmpty()) {
                    fileSplitType.getItems().add(c.toString());
                }
            }
        }
    }

    @FXML
    protected void onSplitByConnectionButtonClick() {
        logFileController.splitLogByType(fileSplitType.getValue().toString());
        logFileController.loadFilesToMemory();
        getTabs();
    }

    @FXML
    private TabPane logTabPane;


    private void getTabs() {
        List<LogFile> lf = logFileController.getLogFiles();
        int size = lf.size();

        for (int i = 0; i < size; i++) {
            LogFile l = lf.get(i);
            int recordCount = l.getLogData().size();

            if (logTabPane.getTabs().size() <= i) {
                tabMetadata.addTabMetadata(i);
                logTabPane.getTabs().add(
                        new Tab(l.getFriendlyName(), getLogDisplaySet(l, i))
                );
            } else {
                logTabPane.getTabs().get(i).setContent(
                        getLogDisplaySet(l, i)
                );
            }

            tabMetadata.calculateTabPagesAvailable(i, recordCount);
        }
    }

    @FXML
    protected void onRefreshButtonClick() {
        refreshPage();
    }

    @FXML
    protected void onResetButtonClick() {
        logFileController.clear();
        logFileController = new LogFileController();
        tabMetadata.clear();
        logTabPane.getTabs().clear();
        System.gc();
        refreshPage();
    }

    @FXML
    protected void onStoreMemoryToDiskButtonClick() {
        int size = logFileController.getLogFiles().size();

        for (int s = 0; s < size; s++) {
            logFileController.storeMemoryFilesToDisk(s);
        }
    }

    @FXML
    protected void onNextPageButtonClick() {
        int activeTabIdx = logTabPane.getSelectionModel().getSelectedIndex();
        tabMetadata.setTabCurrentPage(activeTabIdx, tabMetadata.getTabCurrentPage(activeTabIdx) + 1);
        getTabs();
    }

    @FXML
    protected void onPreviousPageButtonClick() {
        int activeTabIdx = logTabPane.getSelectionModel().getSelectedIndex();
        tabMetadata.setTabCurrentPage(activeTabIdx, tabMetadata.getTabCurrentPage(activeTabIdx) - 1);
        getTabs();
    }

    private void refreshPage() {
        getTabs();
        compileIndex();
        addFileSplitTypes();
    }

    private VBox getLogDisplaySet(LogFile l, int tabIndex) {
        CodeArea ca = new CodeArea(l.getContentsInRange(tabMetadata.getTabPageStart(tabIndex), tabMetadata.getTabPageEnd(tabIndex)));

        VirtualizedScrollPane vsp = new VirtualizedScrollPane(ca);
        vsp.prefHeightProperty().bind(LogParserApplication.getStage().heightProperty().multiply(0.80));

        VBox v = new VBox(vsp,
                new HBox(getLogNavButtonSet(tabIndex), getLogSearchButtonSet(tabIndex,ca,vsp)));
        v.prefHeightProperty().bind(LogParserApplication.getStage().heightProperty().multiply(0.80));

        return v;
    }

    private HBox getLogSearchButtonSet(int uniqueID, CodeArea codeArea, VirtualizedScrollPane virtualizedScrollPane) {
        Button searchBtn = new Button("Search");
        TextField searchTF = new TextField();

        searchBtn.setOnAction((ActionEvent e) -> {
            SearchController s = new SearchController(searchTF.getText());
            s.execSearch(logFileController.getLogFile(uniqueID).getLogData());
            List<Integer> resultIndex = s.getResultList();

            // Search logic
            boolean research = false;
            String text = searchTF.getText();
            do {
                int fromIndex = codeArea.getCaretPosition();

                if (fromIndex >= codeArea.getText().length() || research) {
                    fromIndex = 0;
                }

                research = false;

                int index;
                index = codeArea.getText().toLowerCase(Locale.ROOT).indexOf(text.toLowerCase(), fromIndex);

                if (index >= 0) {
                    int length = text.length();
                    codeArea.selectRange(index, index + length);
                    codeArea.requestFocus();
                    virtualizedScrollPane.scrollToPixel(0,codeArea.getCurrentParagraph()*Math.floor(codeArea.getCurrentParagraph()));
                } else {
                    for (Integer i : resultIndex) {
                        int nextPage = tabMetadata.getPageOfLine(uniqueID,i);
                        int currentPage = tabMetadata.getTabCurrentPage(uniqueID);
                        if (currentPage < nextPage) {
                            tabMetadata.setTabCurrentPage(uniqueID,nextPage);

                            research = true;
                            break;
                        }
                    }
                    if (!research) {
                        tabMetadata.setTabCurrentPage(uniqueID,1);
                    }
                    codeArea.selectRange(0,0);
                    codeArea.replaceText(logFileController.getLogFile(uniqueID).getContentsInRange(tabMetadata.getTabPageStart(uniqueID),tabMetadata.getTabPageEnd(uniqueID)));
                }
            } while (research);
        });

        HBox h = new HBox(searchTF, searchBtn);
        h.setSpacing(1.5);

        return h;
    }

    private HBox getLogNavButtonSet(int uniqueID) {
        Button backButton = new Button("Back");
        Button nextButton = new Button("Next");
        TextField pageJumpTF = new TextField();
        Button pageJumpButton = new Button("Go");
        Text availablePages = new Text(String.valueOf(tabMetadata.getTabPagesAvailable(uniqueID)));
        availablePages.setTextAlignment(TextAlignment.CENTER);

        pageJumpTF.setMaxWidth(50);
        pageJumpTF.promptTextProperty().bindBidirectional(new SimpleStringProperty(String.valueOf(tabMetadata.getTabCurrentPage(uniqueID))));

        backButton.setOnAction((ActionEvent e) -> {
            onPreviousPageButtonClick();
        });

        nextButton.setOnAction((ActionEvent e) -> {
            onNextPageButtonClick();
        });

        pageJumpButton.setOnAction((ActionEvent e) -> {
            tabMetadata.setTabCurrentPage(uniqueID, Integer.valueOf(pageJumpTF.getText()));
            getTabs();
        });

        HBox h = new HBox(backButton, nextButton, pageJumpTF, pageJumpButton, availablePages);
        h.setSpacing(1.5);

        return h;
    }
}