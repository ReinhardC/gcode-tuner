package main.com.specularity.printing;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.com.specularity.printing.GCodes.*;

import java.io.*;
import java.util.*;

public class tuner extends Application {

    private TextArea area;

    @Override
    public void start(Stage primaryStage) {
        Button btnBrowseGCode = new Button();
        btnBrowseGCode.setText("Open a GCode File");
        btnBrowseGCode.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open GCode File");
                File file = fileChooser.showOpenDialog(primaryStage);
                GCodeFile gCodeFile = new GCodeFile(file.getAbsolutePath());
                gCodeFile.process();
            });


        area = new TextArea();

        // A layout container for UI controls
        final BorderPane root = new BorderPane();
        root.setCenter(area);
        root.setBottom(btnBrowseGCode);
        BorderPane.setMargin(area, new Insets(12,12,0,12));
        BorderPane.setMargin(btnBrowseGCode, new Insets(12,12,12,12));

        // Top level container for all view content
        Scene scene = new Scene(root, 800, 550);

        // primaryStage is the main top level window created by platform
        primaryStage.setTitle("GCode Tuner");
        primaryStage.setScene(scene);
        primaryStage.show();

        GCodeFile gCodeFile = new GCodeFile("D:\\Desktop\\dbg.gcode");
        gCodeFile.process();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
