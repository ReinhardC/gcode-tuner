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
import java.util.HashMap;
import java.util.Map;

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
                gCodeFile.gCodes.add(gcode);
            }

            Map<Point3D, Integer> loop = new HashMap<>();

            double last_z = 0;
            int nbLoops = 0;
            Point3D p = new Point3D(0,0,0);

            for (int i = 0; i < gCodeFile.gCodes.size(); i++) {
                int line = i + 1;
                GCode gcode = gCodeFile.gCodes.get(i);

                if(gcode instanceof GCodeCommand) {
                    GCodeCommand cmd = (GCodeCommand)gcode;
                    if(cmd.params.containsKey('X'))
                        p.x = cmd.params.get('X');
                    if(cmd.params.containsKey('Y'))
                        p.y = cmd.params.get('Y');
                    if(cmd.params.containsKey('Z'))
                        p.z = cmd.params.get('Z');
                }

                if(last_z != p.z)
                    loop.clear();
                else {
                    if (loop.containsKey(p)) {
                        if(loop.size()>=3) {
                            nbLoops++;
                            area.appendText("loop found lines " + loop.get(p) + "-" + line + ".\n");
                            loop.clear();
                        }
                    } else
                        loop.put(p, line);
                }

                last_z = p.z;
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