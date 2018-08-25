package com.specularity.printing;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.specularity.printing.GCodes.GCodePerimeter;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;


public class tuner extends Application {

    private TextArea logArea;
    private Button btnTune = new Button("Tune GCode");
    private Button btnBrowseGCode = new Button();

    private GCodeFile gCodeFile = null;

    private static Preferences preferences = Preferences.userNodeForPackage(tuner.class);

    public static final ObservableList<SetPoint> setPointsStart = FXCollections.observableArrayList();
    public static final ObservableList<SetPoint> setPointsEnd = FXCollections.observableArrayList();

    private static Gson gson = new Gson();
    private static final String emptyTableJson = "[{\"offset\":0.0,\"extrusionPct\":100.0,\"angle\":0.0}]";

    @Override
    public void start(Stage primaryStage)
    {
        restoreStateFromPreferences();

        Tab tabOuterPerimeter = createTab("Outer Perimeter");
        Tab tab2ndPerimeter = createTab("2nd Perimeter");
        Tab tab3rdPerimeter = createTab("3rd Perimeter");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(tabOuterPerimeter, tab2ndPerimeter, tab3rdPerimeter);

        btnBrowseGCode.setText("Open GCode");
        btnBrowseGCode.setOnAction(event -> {
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
                gCodeFile.modifyPerimeters();
                // logArea.appendText("tuning complete, perimeter starting points extended by 3mm. writing now.\n");
                String fileName = gCodeFile.writeCopy();
                // logArea.appendText("tuned file written to " + fileName +".\n");
            } else logArea.appendText("no file\n");

        });

        // A layout container for UI controls
        final BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setTop(new HBox(btnBrowseGCode, btnTune));

        HBox.setMargin(btnBrowseGCode, new Insets(12,12,12,12));
        HBox.setMargin(btnTune, new Insets(12,18,12,0));

        // Top level container for all view content
        Scene scene = new Scene(root, 564, 550);

        // primaryStage is the main top level window created by platform
        primaryStage.setTitle("GCode Tuner");
        primaryStage.setScene(scene);
        primaryStage.show();

        gCodeFile = new GCodeFile("D:\\Desktop\\dbg.gcode");
        gCodeFile.load();
        gCodeFile.groupPerimeters();
    }

    private Tab createTab(String label) {
        TableView<SetPoint> tvStart = makeTable(setPointsStart, "setPointsStart");
        tvStart.setPadding(new Insets(4));
        BorderPane leftPane = new BorderPane();
        Label leftPaneLabel = new Label("Path Start Tweaks");
        leftPaneLabel.setFont(new Font("Helvetica", 11));
        leftPaneLabel.setPadding(new Insets(12,4,4, 4));
        leftPane.setTop(leftPaneLabel);
        leftPane.setCenter(tvStart);

        TableView<SetPoint> tvEnd = makeTable(setPointsEnd, "setPointsEnd");
        tvEnd.setPadding(new Insets(4));
        BorderPane rightPane = new BorderPane();
        Label rightPaneLabel = new Label("Path End Tweaks");
        rightPaneLabel.setPadding(new Insets(12,4,4, 4));
        rightPaneLabel.setFont(new Font("Helvetica", 11));
        rightPane.setTop(rightPaneLabel);
        rightPane.setCenter(tvEnd);

        HBox tabBox = new HBox();
        tabBox.getChildren().addAll(leftPane, rightPane);
        tabBox.setSpacing(5);
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        Tab tab = new Tab(label);
        tab.setContent(tabBox);

        return tab;
    }

    private void restoreStateFromPreferences() {
        String strSetPointsStart = preferences.get("setPointsStart", emptyTableJson);
        for(Object o: gson.fromJson(strSetPointsStart, ArrayList.class)) {
            LinkedTreeMap ltm = (LinkedTreeMap) o;
            setPointsStart.add(new SetPoint((Double)ltm.get("offset"), (Double)ltm.get("extrusionPct"), (Double)ltm.get("angle")));
        }

        String strSetPointsEnd = preferences.get("setPointsEnd", emptyTableJson);
        for(Object o: gson.fromJson(strSetPointsEnd, ArrayList.class)) {
            LinkedTreeMap ltm = (LinkedTreeMap) o;
            setPointsEnd.add(new SetPoint((Double)ltm.get("offset"), (Double)ltm.get("extrusionPct"), (Double)ltm.get("angle")));
        }
    }

    private TableView<SetPoint> makeTable(ObservableList<SetPoint> setPoints, String preferenceName) {
        TableView<SetPoint> tv = new TableView<>();

        tv.setId(preferenceName);

        TableColumn numberCol = new TableColumn("#");
        numberCol.setCellValueFactory((Callback<TableColumn.CellDataFeatures<SetPoint, String>, ObservableValue<String>>) p -> new ReadOnlyObjectWrapper<>((tv.getItems().indexOf(p.getValue()) + 1) + ""));
        numberCol.setSortable(false);

        TableColumn<SetPoint, Double> tc1 = new TableColumn<>("Offset (mm)");
        tc1.setId("offset");
        tc1.setMinWidth(80.0);
        tc1.setCellValueFactory(new PropertyValueFactory<>("offset"));
        tc1.setCellFactory(tableColumn -> new EditCell<>(new DoubleStringConverter()));
        tc1.setOnEditCommit( (TableColumn.CellEditEvent<SetPoint, Double> t) -> {
            t.getTableView().getItems().get(
                    t.getTablePosition().getRow()).setOffset(t.getNewValue());
            t.getTableView().refresh();
            preferences.put(preferenceName, gson.toJson(setPoints));
        });
        tc1.setEditable(true);
        tc1.setSortable(false);

        TableColumn<SetPoint, Double> tc2 = new TableColumn<>("Extrusion (%)");
        tc2.setId("extrusion");
        tc2.setMinWidth(80.0);
        tc2.setCellValueFactory(new PropertyValueFactory<>("extrusionPct"));
        tc2.setCellFactory(tableColumn -> new EditCell<>(new DoubleStringConverter()));
        tc2.setOnEditCommit( (TableColumn.CellEditEvent<SetPoint, Double> t) -> {
            t.getTableView().getItems().get(
                    t.getTablePosition().getRow()).setExtrusionPct(t.getNewValue());
            t.getTableView().refresh();
            preferences.put(preferenceName, gson.toJson(setPoints));
        });
        tc2.setEditable(true);
        tc2.setSortable(false);

        TableColumn<SetPoint, Double> tc3 = new TableColumn<>("Angle (°)");
        tc3.setId("angle");
        tc3.setMinWidth(60.0);
        tc3.setCellValueFactory(new PropertyValueFactory<>("angle"));
        tc3.setCellFactory(tableColumn -> new EditCell<>(new DoubleStringConverter()));
        tc3.setOnEditCommit( (TableColumn.CellEditEvent<SetPoint, Double> t) -> {
            t.getTableView().getItems().get(
                    t.getTablePosition().getRow()).setAngle(t.getNewValue());
            t.getTableView().refresh();
            preferences.put(preferenceName, gson.toJson(setPoints));
        });
        tc3.setEditable(true);
        tc3.setSortable(false);

        tv.setEditable(true);
        tv.getColumns().addAll(numberCol, tc1, tc2, tc3);
        tv.setItems(setPoints);

        tv.setRowFactory(tableView -> {
            final TableRow<SetPoint> row = new TableRow<>();
            final ContextMenu rowMenuNull = new ContextMenu();
            final ContextMenu rowMenuNotNull = new ContextMenu();

            MenuItem deleteEntry = new MenuItem("Delete Entry");
            deleteEntry.setOnAction(event -> {
                if(tv.getItems().size() > 1) {
                    tv.getItems().remove(row.getItem());
                    tableView.refresh();
                    preferences.put(preferenceName, gson.toJson(setPoints));
                }
            });

            MenuItem newEntry = new MenuItem("New Entry");
            newEntry.setOnAction(event -> {
                SetPoint item = row.getItem();
                tv.getItems().add(row.getIndex()+1, new SetPoint(item.getOffset(), item.getExtrusionPct(), item.getAngle()));
                tableView.refresh();
                preferences.put(preferenceName, gson.toJson(setPoints));
            });

            MenuItem resetTable = new MenuItem("Reset to default");
            resetTable.setOnAction(event -> {
                tv.getItems().clear();
                String strSetPointsStart = emptyTableJson;
                for(Object o: gson.fromJson(strSetPointsStart, ArrayList.class)) {
                    LinkedTreeMap ltm = (LinkedTreeMap) o;
                    tv.getItems().add(new SetPoint((Double) ltm.get("offset"), (Double) ltm.get("extrusionPct"), (Double) ltm.get("angle")));
                }
                tableView.refresh();
                preferences.put(preferenceName, gson.toJson(setPoints));
            });

            rowMenuNotNull.getItems().addAll(deleteEntry, newEntry, resetTable);

            MenuItem newEntry2 = new MenuItem("New Entry");
            newEntry2.setOnAction(event -> {
                SetPoint item = tv.getItems().get(tv.getItems().size() - 1);
                tv.getItems().add(new SetPoint(item.getOffset(), item.getExtrusionPct(), item.getAngle()));
                tableView.refresh();
                preferences.put(preferenceName, gson.toJson(setPoints));
            });

            MenuItem resetTable2 = new MenuItem("Reset to default");
            resetTable2.setOnAction(event -> {
                tv.getItems().clear();
                String strSetPointsStart = emptyTableJson;
                for(Object o: gson.fromJson(strSetPointsStart, ArrayList.class)) {
                    LinkedTreeMap ltm = (LinkedTreeMap) o;
                    tv.getItems().add(new SetPoint((Double) ltm.get("offset"), (Double) ltm.get("extrusionPct"), (Double) ltm.get("angle")));
                }
                tableView.refresh();
                preferences.put(preferenceName, gson.toJson(setPoints));
            });

            rowMenuNull.getItems().addAll(newEntry2, resetTable2);

//            row.setOnKeyPressed(event -> {
//                if(event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
//                    if(tv.getItems().size() > 1) {
//                        tv.getItems().remove(row.getItem());
//                        tableView.refresh();
//                        preferences.put(preferenceName, gson.toJson(setPoints));
//                    }
//                }
//            });

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && row.isEmpty() ) {
                    SetPoint item = tv.getItems().get(tv.getItems().size() - 1);
                    tv.getItems().add(new SetPoint(item.getOffset(), item.getExtrusionPct(), item.getAngle()));
                    tableView.refresh();
                    preferences.put(preferenceName, gson.toJson(setPoints));
                }
            });

            row.contextMenuProperty().bind(
                    Bindings.when(Bindings.isNotNull(row.itemProperty()))
                            .then(rowMenuNotNull)
                            .otherwise(rowMenuNull));

            return row;
        });
        tv.setPlaceholder(null);

        return tv;
    }
}
