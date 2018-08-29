package com.specularity.printing;

import com.google.gson.internal.LinkedTreeMap;
import com.specularity.printing.GCodes.GCode;
import com.specularity.printing.GCodes.GCodeCommand;
import com.specularity.printing.GCodes.GCodePerimeter;
import com.specularity.printing.ui.EditTab;
import com.specularity.printing.ui.SettingsTab;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static com.specularity.printing.VectorTools.getTriAngle;
import static com.specularity.printing.VectorTools.tunePerimeter;
import static com.specularity.printing.ui.EditTab.emptyTableJson;
import static com.specularity.printing.ui.EditTab.gson;


public class tuner extends Application {
    private static String applicationTitle = "GCode Tuner V1.0a";

    private TextArea logArea = new TextArea();
    private Button btnTune = new Button("Tune GCode");
    private Button btnBrowseGCode = new Button();

    private GCodeFile gCodeFile = null;

    public static Preferences preferences = Preferences.userNodeForPackage(tuner.class);
    public static Font labelFont = Font.font("Regular", FontWeight.BOLD, 11.);

    /**
     * todo: find better solution!
     */
    public final ObservableList<SetPoint> setPointsStartOuter = FXCollections.observableArrayList();
    public final ObservableList<SetPoint> setPointsEndOuter = FXCollections.observableArrayList();

    public final ObservableList<SetPoint> setPointsStart2ndOuter = FXCollections.observableArrayList();
    public final ObservableList<SetPoint> setPointsEnd2ndOuter = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage)
    {
        restoreStateFromPreferences();

        Tab tabOuterPerimeter = new EditTab("Outer Perimeter", "Outer", setPointsStartOuter, setPointsEndOuter);
        Tab tab2ndPerimeter = new EditTab("2nd Outer Perimeter", "2ndOuter", setPointsStart2ndOuter, setPointsEnd2ndOuter);
        Tab tabOther = new SettingsTab();

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabPane.getTabs().addAll(tabOuterPerimeter, tab2ndPerimeter, tabOther);

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
                logArea.appendText(gCodeFile.gCodes.size() + " after grouping\n");
                logArea.appendText(gCodeFile.getPerimeters().count() + " perimeters (inner and outer)\n");
                logArea.appendText(gCodeFile.gCodes.stream().filter(gCode -> !(gCode instanceof GCodePerimeter)).count() + " other\n");
                logArea.appendText(gCodeFile.getPerimeters().mapToLong(gCode -> ((GCodePerimeter) gCode).gCodesLoop.size()).sum() + " inside groups\n");
            }
        });

        btnTune.setOnAction(event -> {
            // GCodeFile gCodeFile2 = new GCodeFile("D:\\Desktop\\g\\dbg.gcode");
            gCodeFile.load();
            tune(gCodeFile);
            logArea.appendText("tuning complete. writing now.\n");
            String fileName = gCodeFile.writeCopy();
            logArea.appendText("tuned file written to " + fileName +".\n");
        });

        // A layout container for UI controls
        final BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setTop(new HBox(btnBrowseGCode, btnTune));

        BorderPane logPane = new BorderPane();
        logPane.setCenter(logArea);
        logArea.setEditable(false);
        logArea.setFont(new Font("Courier New", 11));
        logPane.setMaxHeight(120.0);

        Label logPaneLabel = new Label("Log output");
        logPaneLabel.setPadding(new Insets(6,4,4, 4));
        logPaneLabel.setFont(labelFont);
        logPane.setTop(logPaneLabel);

        root.setBottom(logPane);

        logArea.appendText("GCode Tuner started.\n");

        HBox.setMargin(btnBrowseGCode, new Insets(12,12,12,12));
        HBox.setMargin(btnTune, new Insets(12,18,12,0));

        // Top level container for all view content
        Scene scene = new Scene(root, 600, 800);

        // primaryStage is the main top level window created by platform
        primaryStage.setTitle(applicationTitle);
        primaryStage.setScene(scene);
        primaryStage.show();

        int ok = 01;
    }

    private void restoreStateFromPreferences() {
        String strSetPointsStart1 = preferences.get("setPointsStartOuter", emptyTableJson);
        for(Object o: gson.fromJson(strSetPointsStart1, ArrayList.class)) {
            LinkedTreeMap ltm = (LinkedTreeMap) o;
            setPointsStartOuter.add(new SetPoint((Double)ltm.get("offset"), (Double)ltm.get("angle"), (Double)ltm.get("extrusionPct"), (Double)ltm.get("feedratePct"), (Double)ltm.get("zoffset")));
        }

        String strSetPointsStart2 = preferences.get("setPointsStart2ndOuter", emptyTableJson);
        for(Object o: gson.fromJson(strSetPointsStart2, ArrayList.class)) {
            LinkedTreeMap ltm = (LinkedTreeMap) o;
            setPointsStart2ndOuter.add(new SetPoint((Double)ltm.get("offset"), (Double)ltm.get("angle"), (Double)ltm.get("extrusionPct"), (Double)ltm.get("feedratePct"), (Double)ltm.get("zoffset")));
        }

        String strSetPointsEnd1 = preferences.get("setPointsEndOuter", emptyTableJson);
        for(Object o: gson.fromJson(strSetPointsEnd1, ArrayList.class)) {
            LinkedTreeMap ltm = (LinkedTreeMap) o;
            setPointsEndOuter.add(new SetPoint((Double)ltm.get("offset"), (Double)ltm.get("angle"), (Double)ltm.get("extrusionPct"), (Double)ltm.get("feedratePct"), (Double)ltm.get("zoffset")));
        }

        String strSetPointsEnd2 = preferences.get("setPointsEnd2ndOuter", emptyTableJson);
        for(Object o: gson.fromJson(strSetPointsEnd2, ArrayList.class)) {
            LinkedTreeMap ltm = (LinkedTreeMap) o;
            setPointsEnd2ndOuter.add(new SetPoint((Double)ltm.get("offset"), (Double)ltm.get("angle"), (Double)ltm.get("extrusionPct"), (Double)ltm.get("feedratePct"), (Double)ltm.get("zoffset")));
        }
    }

    void tune(GCodeFile file) {
        for (GCode gCode : file.gCodes) {
            if (gCode instanceof GCodePerimeter) {
                GCodePerimeter perimeter = (GCodePerimeter) gCode;

                if(perimeter.shellIx == 1) {
                    double angle = Math.abs(getTriAngle(perimeter.gCodesLoop.get(perimeter.gCodesLoop.size() - 1).getState().getXY(), perimeter.gCodesLoop.get(perimeter.gCodesLoop.size() - 2).getState().getXY(), perimeter.gCodesLoop.get(0).getState().getXY()) - 180.);
                    if (angle > preferences.getDouble("maxAngleBetweenSegments", 25.0))
                        continue;

                    List<GCode> newGCodes = tunePerimeter(perimeter.gCodesLoop, setPointsStartOuter, setPointsEndOuter);

                    GCodeCommand xyTravelMove = Heuristics.getXYTravelMove(perimeter.gCodesTravel);
                    GCodeCommand zTravelMove = Heuristics.getZTravelMove(perimeter.gCodesTravel);

                    xyTravelMove.putVector2d(newGCodes.get(0).getState().getXY());

                    if (preferences.get("preventLayerChangeOnOuterPerimeter", "on").equals("on") && (zTravelMove != null) && (zTravelMove.getState().getOriginalLineNumber() > xyTravelMove.getState().getOriginalLineNumber())) {
                        GCodeCommand tmp = new GCodeCommand(xyTravelMove);
                        xyTravelMove.set(zTravelMove);
                        zTravelMove.set(tmp);
                    }

                    newGCodes.remove(0);

                    if (!((GCodeCommand) newGCodes.get(0)).has('F'))
                        ((GCodeCommand) newGCodes.get(0)).put('F', ((GCodeCommand) perimeter.gCodesLoop.get(0)).get('F'));

                    perimeter.gCodesLoop = newGCodes;

                    //               for (GCode newGCode : newGCodes)
                    //                   System.out.println(newGCode);
                    //               break;
                }
                else if(perimeter.shellIx == 2)
                {
                    double angle = Math.abs(getTriAngle(perimeter.gCodesLoop.get(perimeter.gCodesLoop.size() - 1).getState().getXY(), perimeter.gCodesLoop.get(perimeter.gCodesLoop.size() - 2).getState().getXY(), perimeter.gCodesLoop.get(0).getState().getXY()) - 180.);
                    if (angle > preferences.getDouble("maxAngleBetweenSegments", 25.0))
                        continue;

                    List<GCode> newGCodes = tunePerimeter(perimeter.gCodesLoop, setPointsStart2ndOuter, setPointsEnd2ndOuter);

                    GCodeCommand xyTravelMove = Heuristics.getXYTravelMove(perimeter.gCodesTravel);
                    xyTravelMove.putVector2d(newGCodes.get(0).getState().getXY());

                    newGCodes.remove(0);

                    if (!((GCodeCommand) newGCodes.get(0)).has('F'))
                        ((GCodeCommand) newGCodes.get(0)).put('F', ((GCodeCommand) perimeter.gCodesLoop.get(0)).get('F'));

                    perimeter.gCodesLoop = newGCodes;
                }
            }
        }
    }
}
