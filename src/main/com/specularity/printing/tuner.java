package main.com.specularity.printing;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.com.specularity.printing.GCodes.GCode;
import main.com.specularity.printing.GCodes.GCodeFactory;
import main.com.specularity.printing.GCodes.GCodeGroup;

import java.io.*;

public class tuner extends Application {

    private TextArea area;
    private GCodeGroup gCodeFile = new GCodeGroup(GCodeGroup.Type.FILE);

    @Override
    public void start(Stage primaryStage) {
        Button btnBrowseGCode = new Button();
        btnBrowseGCode.setText("Open a GCode File");
        btnBrowseGCode.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open GCode File");
                File file = fileChooser.showOpenDialog(primaryStage);
                gGodeParse(file);
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
    }

    private void gGodeParse(File file) {
        StringBuilder sb = new StringBuilder();

        try( FileInputStream fstream = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fstream);
             BufferedReader fileStream = new BufferedReader(new InputStreamReader(dis))
        ) {
            String line;
            while ((line = fileStream.readLine()) != null) {
                GCode gcode = GCodeFactory.produceFromString(line);
                gCodeFile.gCodes.add(gcode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        area.setText(String.valueOf(sb));
    }

    public static void main(String[] args) {
        launch(args);
    }
}