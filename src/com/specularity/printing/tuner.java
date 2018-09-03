package com.specularity.printing;

import com.google.gson.internal.LinkedTreeMap;
import com.specularity.printing.GCodes.GCode;
import com.specularity.printing.GCodes.GCodeCommand;
import com.specularity.printing.GCodes.GCodePerimeter;
import com.specularity.printing.GCodes.GCodePerimeterGroup;
import com.specularity.printing.ui.EditTab;
import com.specularity.printing.ui.SettingsTab;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.prefs.Preferences;

import static com.specularity.printing.GCodeToolkit.*;
import static com.specularity.printing.VectorTools.getTriAngle;
import static com.specularity.printing.ui.EditTab.emptyTableJson;
import static com.specularity.printing.ui.EditTab.gson;

public class tuner extends Application {
    private static String applicationTitle = "GCode Tuner V1.1";

    public static TextArea logArea;

    public static Preferences preferences = Preferences.userNodeForPackage(tuner.class);
    public static Font labelFont;

    private GCodeFile gCodeFile = null;

    /**
     * todo: find better solution for keeping these!
     */
    private final ObservableList<SetPoint> setPointsStartOuter = FXCollections.observableArrayList();
    private final ObservableList<SetPoint> setPointsEndOuter = FXCollections.observableArrayList();

    private final ObservableList<SetPoint> setPointsStart2ndOuter = FXCollections.observableArrayList();
    private final ObservableList<SetPoint> setPointsEnd2ndOuter = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage)
    {
        logArea = new TextArea();
        labelFont = Font.font("Regular", FontWeight.BOLD, 11.);

        restoreStateFromPreferences();

        Button btnProcessAgain= new Button("Reprocess");
        
        Button btnBrowseGCode = new Button("Process GCode");
        btnBrowseGCode.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();

            String initialDirectory = preferences.get("lastDirectoryBrowsed", null);
            if(initialDirectory == null || !new File(initialDirectory).exists())
                fileChooser.setInitialDirectory(null);
            else
                fileChooser.setInitialDirectory(new File(initialDirectory));

            fileChooser.setTitle("Open GCode File");
            File file = fileChooser.showOpenDialog(primaryStage);
            if(file != null) {
                preferences.put("lastDirectoryBrowsed", file.getParent());
                gCodeFile = new GCodeFile(file.getAbsolutePath());
                gCodeFile.load();
                primaryStage.setTitle(applicationTitle + " - " + gCodeFile.file.getName());
                btnProcessAgain.setDisable(false);
                log(file.getAbsolutePath() + " stats:");
                log(gCodeFile.gCodes.size() + " lines read");
                log(gCodeFile.gCodes.size() + " after grouping");
                log(gCodeFile.getPerimeters().count() + " perimeters (inner and outer)");
                log(gCodeFile.gCodes.stream().filter(gCode -> !(gCode instanceof GCodePerimeter)).count() + " other");
                log(gCodeFile.getPerimeters().mapToLong(gCode -> ((GCodePerimeter) gCode).gCodesLoop.size()).sum() + " inside groups");
                tune(gCodeFile);
                log("tuning complete. writing now.");
                String fileName = gCodeFile.writeCopy();
                log("tuned file written to " + fileName +".");
            }
        });
        
        btnProcessAgain.setDisable(true);
        btnProcessAgain.setOnAction(event -> {
            if(gCodeFile != null) {
                gCodeFile.load();
                log(gCodeFile.file.getAbsolutePath() + " stats:");
                log(gCodeFile.gCodes.size() + " lines read");
                log(gCodeFile.gCodes.size() + " after grouping");
                log(gCodeFile.getPerimeters().count() + " perimeters (inner and outer)");
                log(gCodeFile.gCodes.stream().filter(gCode -> !(gCode instanceof GCodePerimeter)).count() + " other");
                log(gCodeFile.getPerimeters().mapToLong(gCode -> ((GCodePerimeter) gCode).gCodesLoop.size()).sum() + " inside groups");
                tune(gCodeFile);
                log("tuning complete. writing now.");
                String fileName = gCodeFile.writeCopy();
                log("tuned file written to " + fileName +".");
            }
        });


        Label presetLabel = new Label("Presets:");

        Button savePresetButton = new Button("Save");
        savePresetButton.setOnAction(event -> {
            TextInputDialog dia = new TextInputDialog("");
            dia.setHeaderText("Enter a name for the preset.");
            dia.setTitle("GCode Tuner");
            dia.setGraphic(null);
            dia.showAndWait().ifPresent(presetName -> {
                try(PrintWriter out = new PrintWriter("./presets/"+presetName+".gtpreset")){
                    out.write(gson.toJson(setPointsStartOuter) + "\n");
                    out.write(gson.toJson(setPointsEndOuter) + "\n");
                    out.write(gson.toJson(setPointsStart2ndOuter) + "\n");
                    out.write(gson.toJson(setPointsEnd2ndOuter) + "\n");
                    log("Preset "+presetName+ ".gtpreset saved in presets folder.");
                } catch (FileNotFoundException ignored) {}
            });
        });

        MenuButton presetButton = new MenuButton("Restore");
        presetButton.setOnMouseEntered(event -> {
            presetButton.getItems().clear();
            ArrayList<File> files = new ArrayList<File>(Arrays.asList(Objects.requireNonNull(new File("./presets").listFiles(pathname -> FilenameUtils.getExtension(pathname.toString()).equals("gtpreset")))));
            files.forEach(file -> {
                MenuItem mi = new MenuItem(FilenameUtils.removeExtension(file.getName()));
                mi.setOnAction(event2 -> {
                    try {
                        String presetStr = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())), Charset.defaultCharset());
                        String[] line = presetStr.split("\n");
                        if(line.length != 4) {
                            log("wrong preset format. should be 4 lines long.");
                            return;
                        }
                        restoreFromString(setPointsStartOuter, line[0]);
                        preferences.put("setPointsStartOuter", line[0]);
                        restoreFromString(setPointsEndOuter, line[1]);
                        preferences.put("setPointsEndOuter", line[1]);
                        restoreFromString(setPointsStart2ndOuter, line[2]);
                        preferences.put("setPointsStart2ndOuter", line[2]);
                        restoreFromString(setPointsEnd2ndOuter, line[3]);
                        preferences.put("setPointsEnd2ndOuter", line[3]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                presetButton.getItems().add(mi);
            });
        });

        MenuButton removePresetButton = new MenuButton("Remove");
        removePresetButton.setOnMouseEntered(event -> {
            removePresetButton.getItems().clear();
            ArrayList<File> files = new ArrayList<File>(Arrays.asList(Objects.requireNonNull(new File("./presets").listFiles(pathname -> FilenameUtils.getExtension(pathname.toString()).equals("gtpreset")))));
            files.forEach(file -> {
                MenuItem mi = new MenuItem(FilenameUtils.removeExtension(file.getName()));
                mi.setOnAction(event2 -> {
                    file.delete();
                });
                removePresetButton.getItems().add(mi);
            });
        });

        Tab tabOuterPerimeter = new EditTab("Outer Perimeter", "Outer", setPointsStartOuter, setPointsEndOuter);
        Tab tab2ndPerimeter = new EditTab("2nd Outer Perimeter", "2ndOuter", setPointsStart2ndOuter, setPointsEnd2ndOuter);
        Tab tabOther = new SettingsTab();

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(tabOuterPerimeter, tab2ndPerimeter, tabOther);

        final BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setTop(new HBox(btnBrowseGCode, btnProcessAgain, presetLabel, savePresetButton, presetButton, removePresetButton));

        HBox.setMargin(btnBrowseGCode, new Insets(12,6,12,12));
        HBox.setMargin(btnProcessAgain, new Insets(12,18,12,0));
        HBox.setMargin(presetLabel, new Insets(16,8,12,0));
        HBox.setMargin(presetButton, new Insets(12,6,12,0));
        HBox.setMargin(savePresetButton, new Insets(12,6,12,0));
        HBox.setMargin(removePresetButton, new Insets(12,6,12,0));

        BorderPane logPane = new BorderPane();
        logPane.setCenter(logArea);
        logArea.setEditable(false);
        logArea.setFocusTraversable(true);
        logArea.setFont(new Font("Courier New", 11));
        logPane.setMaxHeight(120.0);

        Label logPaneLabel = new Label("Log output");
        logPaneLabel.setPadding(new Insets(6,4,4, 4));
        logPaneLabel.setFont(labelFont);
        logPane.setTop(logPaneLabel);

        root.setBottom(logPane);

        log("GCode Tuner started.\n");

        root.setOnDragOver(event -> {
            if (event.getGestureSource() != root && event.getDragboard().hasFiles())
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            event.consume();
        });

        root.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && db.getFiles().size() == 1) {
                gCodeFile = new GCodeFile(db.getFiles().get(0).getAbsolutePath());
                gCodeFile.load();
                primaryStage.setTitle(applicationTitle + " - " + gCodeFile.file.getName());
                btnProcessAgain.setDisable(false);
                log(db.getFiles().get(0).getAbsolutePath() + " stats:");
                log(gCodeFile.gCodes.size() + " lines read");
                log(gCodeFile.gCodes.size() + " after grouping");
                log(gCodeFile.getPerimeters().count() + " perimeters (inner and outer)");
                log(gCodeFile.gCodes.stream().filter(gCode -> !(gCode instanceof GCodePerimeter)).count() + " other");
                log(gCodeFile.getPerimeters().mapToLong(gCode -> ((GCodePerimeter) gCode).gCodesLoop.size()).sum() + " inside groups");
                tune(gCodeFile);
                log("tuning complete. writing now.");
                String fileName = gCodeFile.writeCopy();
                log("tuned file written to " + fileName +".");
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Top level container for all view content
        Scene scene = new Scene(root, 600, 800);

        // primaryStage is the main top level window created by platform
        primaryStage.setTitle(applicationTitle);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    static public void log(String text)
    {
       logArea.appendText(text+"\n");
       logArea.setWrapText(true);
       logArea.setScrollTop(Double.MAX_VALUE);
    }
    
    void tune(GCodeFile file) {
        for (GCode gCode : file.gCodes) {

            boolean bTune = true;
            boolean bFirstShellShifted = false;
            
            if (gCode instanceof GCodePerimeterGroup) {
                GCodePerimeterGroup group = (GCodePerimeterGroup) gCode;
                GCodePerimeter previousPerimeter = null;

                boolean bInner = group.isInnerPerimeter();

                for(int shellIx=1; shellIx <= group.perimeters.size(); shellIx++) {

                    GCodePerimeter perimeter = (GCodePerimeter) group.perimeters.get(bInner ? group.perimeters.size()-shellIx : shellIx-1 );

                    double angle = Math.abs(getTriAngle(perimeter.gCodesLoop.get(perimeter.gCodesLoop.size() - 1).getState().getXY(), perimeter.gCodesLoop.get(perimeter.gCodesLoop.size() - 2).getState().getXY(), perimeter.gCodesLoop.get(0).getState().getXY()) - 180.);
                    if (angle > preferences.getDouble("maxAngleBetweenSegments", 25.0))
                        bTune = false;

                    GCodeCommand xyTravelMove = GCodeToolkit.getLastXYTravelMove(perimeter.gCodesTravel);
                    GCodeCommand zTravelMove = GCodeToolkit.getOnlyZTravelMove(perimeter.gCodesTravel);

                    if (shellIx == 1) { // outer shell
                        if(preferences.get("hideOuterPerimeterStartPointInCavity", "on").equals("on"))
                            bFirstShellShifted = shiftPerimeter(perimeter, findIndexOfMostConcaveAngle(perimeter));

                        if(preferences.get("preventLayerChangeOnOuterPerimeter", "on").equals("on") && (zTravelMove != null) && (zTravelMove.getState().getOriginalLineNumber() > xyTravelMove.getState().getOriginalLineNumber())) {
                            GCodeCommand tmp = new GCodeCommand(xyTravelMove);
                            xyTravelMove.set(zTravelMove);
                            zTravelMove.set(tmp);
                        }

                        if(bFirstShellShifted) {
                            angle = Math.abs(getTriAngle(perimeter.gCodesLoop.get(perimeter.gCodesLoop.size() - 1).getState().getXY(), perimeter.gCodesLoop.get(perimeter.gCodesLoop.size() - 2).getState().getXY(), perimeter.gCodesLoop.get(0).getState().getXY()) - 180.);
                            if (angle > preferences.getDouble("maxAngleBetweenSegments", 25.0))
                                bTune = false;
                        }
                    }
                    else {
                        if(previousPerimeter != null && preferences.get("alignInnerPerimeterStartPointsToOuterPerimeterStartPoint", "on").equals("on")) {
                            GCodeCommand lastMove = getLastXYTravelMove(previousPerimeter.gCodesTravel);
                            if (lastMove != null)
                                shiftPerimeter(perimeter, findIndexOfClosestPointTo(perimeter, lastMove.getState().getXY()));
                        } 
                    }
                    
                    //                                    
                    //          actual tuning             
                    //                                    
                    
                    if(bTune) {
                        if (shellIx == 1) {
                            List<GCode> newGCodes = tunePerimeter(perimeter, setPointsStartOuter, setPointsEndOuter);

                            xyTravelMove.putVector2d(newGCodes.get(0).getState().getXY());

                            newGCodes.remove(0);

                            if (!((GCodeCommand) newGCodes.get(0)).has('F') && ((GCodeCommand) perimeter.gCodesLoop.get(0)).has('F'))
                                ((GCodeCommand) newGCodes.get(0)).put('F', ((GCodeCommand) perimeter.gCodesLoop.get(0)).get('F'));

                            perimeter.gCodesLoop = newGCodes;
                        } else if (shellIx == 2) {
                            List<GCode> newGCodes = tunePerimeter(perimeter, setPointsStart2ndOuter, setPointsEnd2ndOuter);

                            xyTravelMove.putVector2d(newGCodes.get(0).getState().getXY());

                            newGCodes.remove(0);

                            if (!((GCodeCommand) newGCodes.get(0)).has('F') && ((GCodeCommand) perimeter.gCodesLoop.get(0)).has('F'))
                                ((GCodeCommand) newGCodes.get(0)).put('F', ((GCodeCommand) perimeter.gCodesLoop.get(0)).get('F'));

                            perimeter.gCodesLoop = newGCodes;
                        }
                    }
                    
                    previousPerimeter = perimeter;
                }
            }
            else if (gCode instanceof GCodePerimeter) {
                GCodePerimeter perimeter = (GCodePerimeter) gCode;

                double angle = Math.abs(getTriAngle(perimeter.gCodesLoop.get(perimeter.gCodesLoop.size() - 1).getState().getXY(), perimeter.gCodesLoop.get(perimeter.gCodesLoop.size() - 2).getState().getXY(), perimeter.gCodesLoop.get(0).getState().getXY()) - 180.);
                if (angle > preferences.getDouble("maxAngleBetweenSegments", 25.0))
                    bTune = false;

                GCodeCommand xyTravelMove = GCodeToolkit.getLastXYTravelMove(perimeter.gCodesTravel);
                GCodeCommand zTravelMove = GCodeToolkit.getOnlyZTravelMove(perimeter.gCodesTravel);

                if(preferences.get("hideOuterPerimeterStartPointInCavity", "on").equals("on"))
                    bFirstShellShifted = shiftPerimeter(perimeter, findIndexOfMostConcaveAngle(perimeter));

                if(preferences.get("preventLayerChangeOnOuterPerimeter", "on").equals("on") && (zTravelMove != null) && (zTravelMove.getState().getOriginalLineNumber() > xyTravelMove.getState().getOriginalLineNumber())) {
                    GCodeCommand tmp = new GCodeCommand(xyTravelMove);
                    xyTravelMove.set(zTravelMove);
                    zTravelMove.set(tmp);
                }

                if(bFirstShellShifted) {
                    angle = Math.abs(getTriAngle(perimeter.gCodesLoop.get(perimeter.gCodesLoop.size() - 1).getState().getXY(), perimeter.gCodesLoop.get(perimeter.gCodesLoop.size() - 2).getState().getXY(), perimeter.gCodesLoop.get(0).getState().getXY()) - 180.);
                    if (angle > preferences.getDouble("maxAngleBetweenSegments", 25.0))
                        bTune = false;
                }

              if(bTune) {
                    List<GCode> newGCodes = tunePerimeter(perimeter, setPointsStartOuter, setPointsEndOuter);

                    xyTravelMove.putVector2d(newGCodes.get(0).getState().getXY());

                    newGCodes.remove(0);

                    if (!((GCodeCommand) newGCodes.get(0)).has('F') && ((GCodeCommand) perimeter.gCodesLoop.get(0)).has('F'))
                        ((GCodeCommand) newGCodes.get(0)).put('F', ((GCodeCommand) perimeter.gCodesLoop.get(0)).get('F'));

                    perimeter.gCodesLoop = newGCodes;
              }
            }
        }
    }

    private void restoreFromString(ObservableList<SetPoint> setPoints, String string) {
        setPoints.clear();
        for(Object o: gson.fromJson(string, ArrayList.class)) {
            LinkedTreeMap ltm = (LinkedTreeMap) o;
            setPoints.add(new SetPoint((Double)ltm.get("offset"), (Double)ltm.get("angle"), (Double)ltm.get("extrusionPct"), (Double)ltm.get("feedratePct"), (Double)ltm.get("zoffset")));
        }
    }

    private void restoreStateFromPreferences() {
        restoreFromString(setPointsStartOuter, preferences.get("setPointsStartOuter", emptyTableJson));
        restoreFromString(setPointsEndOuter, preferences.get("setPointsEndOuter", emptyTableJson));
        restoreFromString(setPointsStart2ndOuter, preferences.get("setPointsStart2ndOuter", emptyTableJson));
        restoreFromString(setPointsEnd2ndOuter, preferences.get("setPointsEnd2ndOuter", emptyTableJson));
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}

