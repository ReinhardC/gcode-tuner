package com.specularity.printing.ui;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.specularity.printing.SetPoint;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;

import java.util.ArrayList;

import static com.specularity.printing.tuner.labelFont;
import static com.specularity.printing.tuner.preferences;

public class EditTab extends Tab {

    public static final String emptyTableJson = "[{\"offset\":0.0,\"angle\":0.0,\"extrusionPct\":100.0,\"feedratePct\":100.0,\"zoffset\":0.0}]";

    public static Gson gson = new Gson();

    public EditTab(String label, String nb, ObservableList<SetPoint> setPointsStart, ObservableList<SetPoint> setPointsEnd) {
        super(label);

        TableView<SetPoint> tvStart = makeTable(setPointsStart, "setPointsStart" + nb);
        BorderPane leftPane = new BorderPane();
        Label leftPaneLabel = new Label("Path Start Tweaks");
        leftPaneLabel.setFont(labelFont);
        leftPaneLabel.setPadding(new Insets(12,4,4, 4));
        leftPane.setTop(leftPaneLabel);
        leftPane.setCenter(tvStart);

        TableView<SetPoint> tvEnd = makeTable(setPointsEnd, "setPointsEnd" + nb);
        BorderPane rightPane = new BorderPane();
        Label rightPaneLabel = new Label("Path End Tweaks");
        rightPaneLabel.setPadding(new Insets(4,4,4, 4));
        rightPaneLabel.setFont(labelFont);
        rightPane.setTop(rightPaneLabel);
        rightPane.setCenter(tvEnd);

        VBox tabBox = new VBox();
        tabBox.getChildren().addAll(leftPane, rightPane);
        tabBox.setSpacing(5);
        VBox.setVgrow(leftPane, Priority.ALWAYS);
        VBox.setVgrow(rightPane, Priority.ALWAYS);

        setContent(tabBox);
    }

    private TableView<SetPoint> makeTable(ObservableList<SetPoint> setPoints, String preferenceName) {
        TableView<SetPoint> tv = new TableView<>();

        tv.setId(preferenceName);

        TableColumn colNumber = new TableColumn("#");
        colNumber.setCellValueFactory((Callback<TableColumn.CellDataFeatures<SetPoint, String>, ObservableValue<String>>) p -> new ReadOnlyObjectWrapper<>((tv.getItems().indexOf(p.getValue()) + 1) + ""));
        colNumber.setSortable(false);

        TableColumn<SetPoint, Double> colOffset = new TableColumn<>("Offset (mm)");
        colOffset.setId("offset");
        colOffset.setMinWidth(80.0);
        colOffset.setCellValueFactory(new PropertyValueFactory<>("offset"));
        colOffset.setCellFactory(tableColumn -> new EditCell<>(new DoubleStringConverter()));
        colOffset.setOnEditCommit( (TableColumn.CellEditEvent<SetPoint, Double> t) -> {
            t.getTableView().getItems().get(
                    t.getTablePosition().getRow()).setOffset(t.getNewValue());
            t.getTableView().refresh();
            preferences.put(preferenceName, gson.toJson(setPoints));
        });
        colOffset.setEditable(true);
        colOffset.setSortable(false);

        TableColumn<SetPoint, Double> colAngle = new TableColumn<>("Angle (°)");
        colAngle.setId("angle");
        colAngle.setMinWidth(60.0);
        colAngle.setCellValueFactory(new PropertyValueFactory<>("angle"));
        colAngle.setCellFactory(tableColumn -> new EditCell<>(new DoubleStringConverter()));
        colAngle.setOnEditCommit( (TableColumn.CellEditEvent<SetPoint, Double> t) -> {
            t.getTableView().getItems().get(
                    t.getTablePosition().getRow()).setAngle(t.getNewValue());
            t.getTableView().refresh();
            preferences.put(preferenceName, gson.toJson(setPoints));
        });
        colAngle.setEditable(true);
        colAngle.setSortable(false);

        TableColumn<SetPoint, Double> colExtrusion = new TableColumn<>("Extrusion (%)");
        colExtrusion.setId("extrusion");
        colExtrusion.setMinWidth(80.0);
        colExtrusion.setCellValueFactory(new PropertyValueFactory<>("extrusionPct"));
        colExtrusion.setCellFactory(tableColumn -> new EditCell<>(new DoubleStringConverter()));
        colExtrusion.setOnEditCommit( (TableColumn.CellEditEvent<SetPoint, Double> t) -> {
            t.getTableView().getItems().get(
                    t.getTablePosition().getRow()).setExtrusionPct(t.getNewValue());
            t.getTableView().refresh();
            preferences.put(preferenceName, gson.toJson(setPoints));
        });
        colExtrusion.setEditable(true);
        colExtrusion.setSortable(false);

        TableColumn<SetPoint, Double> colFeedrate = new TableColumn<>("Feedrate (%)");
        colFeedrate.setId("feedrate");
        colFeedrate.setMinWidth(80.0);
        colFeedrate.setCellValueFactory(new PropertyValueFactory<>("feedratePct"));
        colFeedrate.setCellFactory(tableColumn -> new EditCell<>(new DoubleStringConverter()));
        colFeedrate.setOnEditCommit( (TableColumn.CellEditEvent<SetPoint, Double> t) -> {
            t.getTableView().getItems().get(
                    t.getTablePosition().getRow()).setFeedratePct(t.getNewValue());
            t.getTableView().refresh();
            preferences.put(preferenceName, gson.toJson(setPoints));
        });
        colFeedrate.setEditable(true);
        colFeedrate.setSortable(false);

        TableColumn<SetPoint, Double> colZ = new TableColumn<>("Z-Offset (mm)");
        colZ.setId("zoffset");
        colZ.setMinWidth(80.0);
        colZ.setCellValueFactory(new PropertyValueFactory<>("zoffset"));
        colZ.setCellFactory(tableColumn -> new EditCell<>(new DoubleStringConverter()));
        colZ.setOnEditCommit( (TableColumn.CellEditEvent<SetPoint, Double> t) -> {
            t.getTableView().getItems().get(
                    t.getTablePosition().getRow()).setZoffset(t.getNewValue());
            t.getTableView().refresh();
            preferences.put(preferenceName, gson.toJson(setPoints));
        });
        colZ.setEditable(true);
        colZ.setSortable(false);

        tv.setEditable(true);
        tv.getColumns().addAll(colNumber, colOffset, colAngle, colExtrusion, colFeedrate, colZ);
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
                tv.getItems().add(row.getIndex()+1, new SetPoint(item.getOffset(), item.getAngle(), item.getExtrusionPct(), item.getFeedratePct(), item.getZoffset()));
                tableView.refresh();
                preferences.put(preferenceName, gson.toJson(setPoints));
            });

            MenuItem resetTable = new MenuItem("Reset to default");
            resetTable.setOnAction(event -> {
                tv.getItems().clear();
                String strSetPointsStart = emptyTableJson;
                for(Object o: gson.fromJson(strSetPointsStart, ArrayList.class)) {
                    LinkedTreeMap ltm = (LinkedTreeMap) o;
                    tv.getItems().add(new SetPoint((Double) ltm.get("offset"), (Double) ltm.get("angle"), (Double) ltm.get("extrusionPct"), (Double) ltm.get("feedratePct"), (Double) ltm.get("zoffset")));
                }
                tableView.refresh();
                preferences.put(preferenceName, gson.toJson(setPoints));
            });

            rowMenuNotNull.getItems().addAll(deleteEntry, newEntry, resetTable);

            MenuItem newEntry2 = new MenuItem("New Entry");
            newEntry2.setOnAction(event -> {
                SetPoint item = tv.getItems().get(tv.getItems().size() - 1);
                tv.getItems().add(new SetPoint(item.getOffset(), item.getAngle(), item.getExtrusionPct(), item.getFeedratePct(), item.getZoffset()));
                tableView.refresh();
                preferences.put(preferenceName, gson.toJson(setPoints));
            });

            MenuItem resetTable2 = new MenuItem("Reset to default");
            resetTable2.setOnAction(event -> {
                tv.getItems().clear();
                String strSetPointsStart = emptyTableJson;
                for(Object o: gson.fromJson(strSetPointsStart, ArrayList.class)) {
                    LinkedTreeMap ltm = (LinkedTreeMap) o;
                    tv.getItems().add(new SetPoint((Double) ltm.get("offset"), (Double) ltm.get("angle"), (Double) ltm.get("extrusionPct"), (Double) ltm.get("feedratePct"), (Double) ltm.get("zoffset")));
                }
                tableView.refresh();
                preferences.put(preferenceName, gson.toJson(setPoints));
            });

            rowMenuNull.getItems().addAll(newEntry2, resetTable2);

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && row.isEmpty() ) {
                    SetPoint item = tv.getItems().get(tv.getItems().size() - 1);
                    tv.getItems().add(new SetPoint(item.getOffset(), item.getAngle(), item.getExtrusionPct(), item.getFeedratePct(), item.getZoffset()));
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
