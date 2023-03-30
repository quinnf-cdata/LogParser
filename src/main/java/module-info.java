module com.example.logparser {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires org.fxmisc.richtext;
    requires protobuf.java;
    requires protobuf.java.util;
    requires org.fxmisc.flowless;
    requires org.junit.jupiter.api;

    opens com.example.logparser to javafx.fxml;
    exports com.example.logparser;
    exports models;
}