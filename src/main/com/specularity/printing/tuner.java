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

        File testfile = new File("D:\\Desktop\\dbg.gcode");
        gGodeParse(testfile);
    }

    private void gGodeParse(File file) {
        area.appendText("parsing gcode file " + file.getAbsolutePath() + ".\n");
        try( FileInputStream fstream = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fstream);
             BufferedReader fileStream = new BufferedReader(new InputStreamReader(dis))
        ) {
            String strLine;
            int nbLines = 0;
            while ((strLine = fileStream.readLine()) != null) {
                nbLines++;
                GCode gcode = GCodeFactory.produceFromString(strLine);
                gcode.originalLineNumber = nbLines;
                gCodeFile.gCodes.add(gcode);
            }

            // identify loops

            List<GCode> oriCodes = gCodeFile.gCodes;
            gCodeFile.gCodes = new ArrayList<>();

            Map<Point3D, Integer> loopPoints = new HashMap<>();
            GCodeGroup tmpGroup = new GCodeGroup(GCodeGroup.Type.TMP);

            double last_z = 0;
            int nbLoops = 0;
            Point3D p = new Point3D(0,0,0);
            Point3D lastPointPut = new Point3D(p);

            for (int i = 0; i < oriCodes.size(); i++) {
                GCode gcode = oriCodes.get(i);

                tmpGroup.gCodes.add(gcode);
                int curIx = tmpGroup.gCodes.size() - 1;

                if(gcode instanceof GCodeCommand) {
                    GCodeCommand cmd = (GCodeCommand)gcode;
                    if(cmd.params.containsKey('X'))
                        p.x = cmd.params.get('X');
                    if(cmd.params.containsKey('Y'))
                        p.y = cmd.params.get('Y');
                    if(cmd.params.containsKey('Z'))
                        p.z = cmd.params.get('Z');
                }

                if(last_z != p.z) {
                    // no loop found, z was changed
                    gCodeFile.gCodes.addAll(tmpGroup.gCodes);
                    loopPoints.clear();
                    tmpGroup = new GCodeGroup(GCodeGroup.Type.TMP);
                }
                else {
                    if (loopPoints.containsKey(p)) {
                        if(!lastPointPut.equals(p)) {
                            nbLoops++;

                            int firstLoopIx = loopPoints.get(p);

                            area.appendText("loop found lines " + ((i+1) - (curIx-firstLoopIx)) + "-" + (i+1) + ".\n");

                            gCodeFile.gCodes.addAll(tmpGroup.gCodes.subList(0, firstLoopIx ));

                            GCodeGroup loopGroup = new GCodeGroup(GCodeGroup.Type.LOOP);
                            loopGroup.gCodes.addAll(tmpGroup.gCodes.subList(firstLoopIx, curIx + 1));
                            loopGroup.originalLineNumber = loopGroup.gCodes.get(0).originalLineNumber;
                            gCodeFile.gCodes.add(loopGroup);

                            loopPoints.clear();
                            tmpGroup = new GCodeGroup(GCodeGroup.Type.TMP);
                        }
                    }
                    else {
                        loopPoints.put(p, curIx);
                        lastPointPut.copyFrom(p);
                    }
                }

                last_z = p.z;
            }

            PrintWriter writer = new PrintWriter(file.getAbsolutePath().replace(".gcode", "_2.gcode"), "UTF-8");
            try {
                gCodeFile.serialize(writer);
            } catch (FileNotFoundException ex) {
                // complain to user
            } catch (IOException ex) {
                // notify user
            } finally {
                writer.close();
            }

            area.appendText(gCodeFile.gCodes.stream().filter(gCode -> !(gCode instanceof GCodeComment)).count()+" commands in "+nbLines+" lines read.\n");
            area.appendText(nbLoops + " loops found.");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) {
        launch(args);
    }
}
