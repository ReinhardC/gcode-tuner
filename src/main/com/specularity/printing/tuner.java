package main.com.specularity.printing;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.FormatStringConverter;
import main.com.specularity.printing.GCodes.*;

import java.io.*;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.prefs.Preferences;

import static javafx.scene.control.cell.TextFieldTableCell.forTableColumn;

public class tuner extends Application {

    private TextArea logArea;
    private TextField extensionWidth = new TextField();
    private Button btnTune = new Button("Tune GCode");
    private Button btnBrowseGCode = new Button("Open a GCode File");

    private TableView<TestData> tv = new TableView<>();

    private GCodeFile gCodeFile = null;

    @Override
    public void start(Stage primaryStage)
    {
        ObservableList<TestData> data = FXCollections.observableArrayList(
                new TestData(1.,0.),
                new TestData(100.,0.));

        TableColumn<TestData, Double> tc1 = new TableColumn<>("From");
        tc1.setCellValueFactory(new PropertyValueFactory<>("a"));

        TableColumn<TestData, Double> tc2 = new TableColumn<>("To");
        tc2.setCellValueFactory(new PropertyValueFactory<>("b"));
        tc2.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        tc2.setOnEditCommit( (TableColumn.CellEditEvent<TestData, Double> event) -> {});
        tc2.setEditable(true);

        tv.setEditable(true);
        tv.getColumns().addAll(tc1, tc2);
        tv.setItems(data);

        btnBrowseGCode.setOnAction(event -> {
            Preferences preferences = Preferences.userNodeForPackage(tuner.class);
            String initDir = preferences.get("lastDirectoryBrowsed", null);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open GCode File");
            fileChooser.setInitialDirectory(initDir != null ? new File(initDir) : null);

            File file = fileChooser.showOpenDialog(primaryStage);
            if(file != null) {
                preferences.put("lastDirectoryBrowsed", file.getParent());
                gCodeFile = new GCodeFile(file.getAbsolutePath());
                gCodeFile.load();
                logArea.appendText(file.getAbsolutePath() + " stats:\n");
                logArea.appendText(gCodeFile.gCodes.size() + " lines read\n");
                gCodeFile.groupPerimeters();
                logArea.appendText(gCodeFile.gCodes.size() + " after grouping\n");
                logArea.appendText(gCodeFile.getPerimeters().count() + " perimeters (inner and outer)\n");
                logArea.appendText(gCodeFile.gCodes.stream().filter(gCode -> !(gCode instanceof GCodePerimeter)).count() + " other\n");
                logArea.appendText(gCodeFile.getPerimeters().mapToLong(gCode -> ((GCodePerimeter) gCode).gCodes.size()).sum() + " inside groups\n");
            }
        });

        btnTune.setOnAction(event -> {
            if(gCodeFile != null) {
                gCodeFile.processPerimeters(Double.parseDouble(extensionWidth.getText()));
                logArea.appendText("tuning complete, perimeter starting points extended by " + Double.parseDouble(extensionWidth.getText()) + "mm. writing now.\n");
                String fileName = gCodeFile.writeCopy();
                logArea.appendText("tuned file written to " + fileName +".\n");
            } else logArea.appendText("no file\n");

        });

        extensionWidth.setMaxWidth(50.);
        extensionWidth.setText("0.33");
        extensionWidth.setTooltip(new Tooltip("length of extension in mm"));

        logArea = new TextArea();

        // A layout container for UI controls
        final BorderPane root = new BorderPane();
        root.setCenter(tv);
        root.setTop(new HBox(btnBrowseGCode, extensionWidth, btnTune));
        BorderPane.setMargin(logArea, new Insets(0,12,12,12));

        HBox.setMargin(btnBrowseGCode, new Insets(12,18,12,12));
        HBox.setMargin(extensionWidth, new Insets(12,2,12,0));
        HBox.setMargin(btnTune, new Insets(12,18,12,0));

        // Top level container for all view content
        Scene scene = new Scene(root, 800, 550);

        // primaryStage is the main top level window created by platform
        primaryStage.setTitle("GCode Tuner");
        primaryStage.setScene(scene);
        primaryStage.show();

        //  GCodeFile gCodeFile = new GCodeFile("D:\\Desktop\\dbg.gcode");
        //  gCodeFile.process();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
