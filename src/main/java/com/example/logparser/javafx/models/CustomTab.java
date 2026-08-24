package com.example.logparser.javafx.models;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import org.fxmisc.richtext.CodeArea;

public class CustomTab extends Tab {
    private final int metadataID;
    private CodeArea codeArea;

    public CustomTab(int metadataID, String text, Node content) {
        this.metadataID = metadataID;
        super.setText(text);
        super.setContent(content);
        super.setId(String.valueOf(metadataID));
    }

    public int getMetadataID() {
        return metadataID;
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public void setCodeArea(CodeArea codeArea) {
        this.codeArea = codeArea;
    }
}
