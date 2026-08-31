module com.example.logparser {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires com.dlsc.formsfx;
//    requires org.kordamp.bootstrapfx.core;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
//    requires org.junit.jupiter.api;
    requires org.apache.lucene.core;
    requires org.apache.lucene.queryparser;
    requires org.apache.lucene.misc;
    requires org.json;

    opens com.example.logparser to javafx.fxml;

    exports com.example.logparser;
    exports com.example.logparser.models;
    exports com.example.logparser.controllers;
    exports com.example.logparser.modules;
    exports models;
    exports controllers;
    opens com.example.logparser.controllers to javafx.fxml;
    exports com.example.logparser.javafx.modules;
    exports com.example.logparser.javafx.models;
}