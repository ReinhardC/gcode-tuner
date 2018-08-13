package main.com.specularity.printing;

import main.com.specularity.printing.GCodes.*;

import java.io.*;
import java.util.*;

public class GCodeFile {
    private final File file;
    private GCodeGroup fileGroup = new GCodeGroup(GCodeGroup.Type.FILE);

    public GCodeFile(String filepath) {
        file = new File(filepath);
    }

    public void process() {
        load();
        groupLoops();
        processLoops();
        writeCopy();
    }

    private void processLoops() {
        fileGroup.gCodes.forEach(gCode -> {
            if(gCode instanceof GCodeGroup) {
                GCodeGroup loop = (GCodeGroup) gCode;

                Optional<GCode> test = loop.gCodes.stream().filter(gCode1 -> gCode1 instanceof GCodeCommand && ((GCodeCommand) gCode1).command.equals("G1")).findFirst();

                for (GCode code : loop.gCodes) {
                    if(code instanceof GCodeCommand) {
                        GCodeCommand cmd = (GCodeCommand)code;
                        if(cmd.command.equals("G1")) {

                        }
                    }
                }
            }
        });
    }

    public void writeCopy() {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file.getAbsolutePath().replace(".gcode", "_2.gcode"), "UTF-8");
            fileGroup.serialize(writer);
        } catch (IOException ex) {
            // notify user
        } finally {
            writer.close();
        }
    }

    public void load() {
        try (FileInputStream fstream = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fstream);
             BufferedReader fileStream = new BufferedReader(new InputStreamReader(dis))
        ) {
            String strLine;
            int nbLines = 0;
            while ((strLine = fileStream.readLine()) != null) {
                nbLines++;
                GCode gcode = GCodeFactory.produceFromString(strLine);
                gcode.originalLineNumber = nbLines;
                fileGroup.gCodes.add(gcode);
            }

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void groupLoops() {
        List<GCode> oriCodes = fileGroup.gCodes;
        fileGroup.gCodes = new ArrayList<>();

        Map<Point3D, Integer> loopPoints = new HashMap<>();
        GCodeGroup tmpGroup = new GCodeGroup(GCodeGroup.Type.TMP);

        double last_z = 0;
        Point3D p = new Point3D(0, 0, 0);
        Point3D lastPointPut = new Point3D(p);

        for (int i = 0; i < oriCodes.size(); i++) {
            GCode gcode = oriCodes.get(i);

            tmpGroup.gCodes.add(gcode);
            int curIx = tmpGroup.gCodes.size() - 1;

            if (gcode instanceof GCodeCommand) {
                GCodeCommand cmd = (GCodeCommand) gcode;
                if (cmd.params.containsKey('X'))
                    p.x = cmd.params.get('X');
                if (cmd.params.containsKey('Y'))
                    p.y = cmd.params.get('Y');
                if (cmd.params.containsKey('Z'))
                    p.z = cmd.params.get('Z');
            }

            if (last_z != p.z || i == oriCodes.size() - 1) {
                // no loop found, z was changed, or last of gcodes
                fileGroup.gCodes.addAll(tmpGroup.gCodes);
                loopPoints.clear();
                tmpGroup = new GCodeGroup(GCodeGroup.Type.TMP);
            } else {
                if (loopPoints.containsKey(p)) {
                    if (!lastPointPut.equals(p)) {
                        int firstLoopIx = loopPoints.get(p);

                        fileGroup.gCodes.addAll(tmpGroup.gCodes.subList(0, firstLoopIx));

                        GCodeGroup loopGroup = new GCodeGroup(GCodeGroup.Type.LOOP);
                        loopGroup.gCodes.addAll(tmpGroup.gCodes.subList(firstLoopIx, curIx + 1));
                        loopGroup.originalLineNumber = loopGroup.gCodes.get(0).originalLineNumber;
                        fileGroup.gCodes.add(loopGroup);

                        loopPoints.clear();
                        tmpGroup = new GCodeGroup(GCodeGroup.Type.TMP);
                    }
                } else {
                    loopPoints.put(p, curIx);
                    lastPointPut.copyFrom(p);
                }
            }

            last_z = p.z;
        }
    }
}
