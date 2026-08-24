package com.example.logparser.javafx.modules;

import com.example.logparser.controllers.LogParserController;
import com.example.logparser.modules.Constants;
import com.example.logparser.modules.Utilities;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import com.example.logparser.models.LogFile;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndexTree extends LogParserController {
    /*START INDEX TREE CODE*/
//    private LogFileController logFileController;
    private TreeItem<String> root;
    private TreeView<String> treeView;
    private HashMap<String, HashMap<String, Integer>> masterHierarchy = null;
    private TextField searchField;
    private LogParserController logParserController;

    public void indexTree(LogParserController logParserController) {
//        this.logFileController = logFileController;
        this.root = new TreeItem<>();
        this.treeView = new TreeView<>(root);
        this.searchField = new TextField();
        this.logParserController = logParserController;
        if (masterHierarchy == null) {
            masterHierarchy = new HashMap<>();
        }

        buildTree();
        setupSearchField();
        setupNodeDoubleClickHandler();
    }

    private void buildTree() {
        treeView.setShowRoot(false);

        for (LogFile l : logFileController.getLogFiles()) {
            HashMap<String, Integer> hierarchy;

            TreeItem<String> fileTop = new TreeItem<>(l.getFileName());
            hierarchy = l.queryIndexToHierarchy("", "");

            if (hierarchy == null || hierarchy.isEmpty()) {
                continue;
            }


            for (Map.Entry<String, Integer> e : hierarchy.entrySet()) {
                String s = e.getKey();
                String[] ss = s.split("/");
                TreeItem<String> currentParent = fileTop;

                for (String n : ss) {
                    currentParent = findOrCreateChild(currentParent, n);
                }
            }

            sortTreeItemAlphabetically(fileTop);

            Platform.runLater(() -> {
                Iterator<TreeItem<String>> it = root.getChildren().iterator();
                while (it.hasNext()) {
                    String s = it.next().getValue();
                    if (s.equals(fileTop.getValue())) {
                        it.remove();
                    }
                }

                root.getChildren().add(fileTop);
                sortTreeItemAlphabetically(root);
            });

            masterHierarchy.put(l.getFileName(),hierarchy);
        }
    }

    private void setupSearchField() {
        Platform.runLater(() -> {
            searchField.setPromptText("Search...");
            searchField.textProperty().addListener((observable, oldValue, newValue) -> filter(newValue));
        });
    }

    private void filter(String query) {
        if (query == null || query.isEmpty()) {
            treeView.setRoot(root);
        } else {
            TreeItem<String> filteredRoot = createFilteredTree(root, query);
            treeView.setRoot(filteredRoot);
        }

        if (treeView.getRoot() != null) {
            treeView.getRoot().setExpanded(true);
        }
    }

    private TreeItem<String> createFilteredTree(TreeItem<String> original, String query) {
        if (original == null) return null;

        TreeItem<String> filteredRoot = new TreeItem<>(original.getValue());
        filteredRoot.setExpanded(true);

        boolean foundInChild = false;
        for (TreeItem<String> child : original.getChildren()) {
            TreeItem<String> filteredChild = createFilteredTree(child, query);
            if (filteredChild != null) {
                foundInChild = true;
                filteredRoot.getChildren().add(filteredChild);
            }
        }

        String value = original.getValue();

        if (value == null) {
            value = "";
        }

        if (foundInChild || value.toLowerCase().contains(query.toLowerCase())) {
            return filteredRoot;
        } else {
            return null;
        }
    }

    private TreeItem<String> findOrCreateChild(TreeItem<String> parent, String childName) {
        for (TreeItem<String> child : parent.getChildren()) {
            if (child.getValue().equals(childName)) {
                return child;
            }
        }

        TreeItem<String> newChild = new TreeItem<>(childName);
        parent.getChildren().add(newChild);
        return newChild;
    }

    private void sortTreeItemAlphabetically(TreeItem<String> parent) {
        parent.getChildren().sort(new IndexTree.NaturalOrderComparator());

        for (TreeItem<String> child : parent.getChildren()) {
            sortTreeItemAlphabetically(child);
        }
    }

    private static class NaturalOrderComparator implements Comparator<TreeItem<String>> {
        private final Pattern pattern = Pattern.compile("(\\D*)(\\d*)");

        @Override
        public int compare(TreeItem<String> o1, TreeItem<String> o2) {
            Matcher m1 = pattern.matcher(o1.getValue());
            Matcher m2 = pattern.matcher(o2.getValue());

            while (m1.find() && m2.find()) {
                // Compare non-digit part
                int nonDigitCompare = m1.group(1).compareTo(m2.group(1));
                if (nonDigitCompare != 0) {
                    return nonDigitCompare;
                }

                // Compare digit part
                if (!m1.group(2).isEmpty() && !m2.group(2).isEmpty()) {
                    long num1 = Long.parseLong(m1.group(2));
                    long num2 = Long.parseLong(m2.group(2));
                    int numCompare = Long.compare(num1, num2);
                    if (numCompare != 0) {
                        return numCompare;
                    }
                }
            }

            // Handle cases where one string is a prefix of the other
            return Integer.compare(o1.getValue().length(), o2.getValue().length());
        }
    }

    private void setupNodeDoubleClickHandler() {
        treeView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() != 2) return;

            TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
            if (selectedItem == null) return;

            String value = selectedItem.getValue();
            if (value == null || value.isEmpty()) return;

            String valueUpper = value.toUpperCase(java.util.Locale.ROOT);
            if (!Utilities.startsWithNumeric(valueUpper)) {
                for (Constants.DOUBLE_CLICK_IGNORE e : Constants.DOUBLE_CLICK_IGNORE.values()) {
                    if (valueUpper.contains(e.getName().toUpperCase(java.util.Locale.ROOT))) {
                        return;
                    }
                }
            }

            List<String> parts = new ArrayList<>();
            TreeItem<String> node = selectedItem;
            String keyChild = null;
            String fileName = null;

            while (node != null && node.getParent() != null) {
                TreeItem<String> p = node.getParent();
                fileName = node.getValue();
                if (p.getValue() == null) {
                    break;
                }
                keyChild = node.getValue();
                parts.add(keyChild);
                node = p;
            }

            Collections.reverse(parts);
            String rebuiltHierarchy = String.join("/", parts);

            HashMap<String, Integer> hierarchy = masterHierarchy.get(fileName);

            Integer idx = hierarchy.get(rebuiltHierarchy);
            if (idx == null) return;

            String parentValue = (node == null ? "" : node.getValue());
            String keyChildSafe = (keyChild == null ? "" : keyChild);

            System.out.println("Double-clicked on: " + parentValue + "/" + keyChildSafe + "/" + value
                    + "\tLine number: " + idx);

            indexDoubleClick(idx, parentValue);
        });
    }


    public Node getSearchableTree() {
        VBox vbox = new VBox();
        VBox.setVgrow(treeView, Priority.ALWAYS);
        vbox.getChildren().addAll(searchField, treeView);
        return vbox;
    }

    public void indexDoubleClick(int lineNumber, String masterFile) {
        TabPane logTabPane = logParserController.logTabPane;
        List<Tab> tabs = logTabPane.getTabs();

        int tabIndex = -1;
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).getText().equalsIgnoreCase(masterFile)) {
                tabIndex = i;
                break;
            }
        }
        if (tabIndex < 0) return; // or handle "tab not found"

        Tab tab = tabs.get(tabIndex);
        logTabPane.getSelectionModel().select(tabIndex);

        // If you truly need a separate stable ID, store it separately.
        // For now, use tabIndex consistently as the tab identifier.
        tabMetadata.setCurrentPageContainingLine(tabIndex, lineNumber);

        logParserController.refreshLogPane();

        // Re-fetch content from the tab you selected (refresh may have changed nodes)
        VBox v = (VBox) tab.getContent();
        CodeArea c = (CodeArea) ((VirtualizedScrollPane<?>) v.getChildren().get(0)).getContent();

        // If possible, avoid IO and pull from CodeArea directly instead of rereading the file.
        String fullLine = logFileController
                .getLogFileByID(tabIndex)
                .readLinesToString(
                        BigInteger.valueOf(lineNumber),
                        BigInteger.valueOf(lineNumber + 1),
                        false
                );

        if (fullLine.endsWith("\n")) {
            fullLine = fullLine.substring(0,fullLine.length()-2);
        }

        logParserController.performSearch(
                tabIndex, c, fullLine, null, null,
                new Text(Constants.ACTION_TYPES.DBKCLK.name())
        );
    }

    private long getLineNumberUsingSearch(String searchStr, String parent, int activeTabIdx) {
        searchStr = "\"" + parent + "\" AND \"" + searchStr + "\"";
        LogFile logFile = logFileController.getLogFileByID(activeTabIdx);
        int maxLine = logFile.getMaxLines();
        logFile.setMaxLines(1000);
        ArrayList<Long> arr = logFile.getHitLines("", searchStr, true);
        arr.sort(null);
        logFile.setMaxLines(maxLine);

        return arr.isEmpty() ? 1 : arr.get(0);
    }

    private String getNumericLead(String input) {
        // This regex looks for the first sequence of digits in the string
        Pattern pattern = Pattern.compile("(^\\d+)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }


    /*END INDEX TREE CODE*/


}
