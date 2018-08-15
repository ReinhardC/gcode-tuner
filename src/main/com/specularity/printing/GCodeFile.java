package main.com.specularity.printing;

import main.com.specularity.printing.GCodes.*;

import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import java.io.*;
import java.util.*;
import java.util.stream.Stream;

class GCodeFile {
    private final File file;

    List<GCode> gCodes = new ArrayList<>();

    GCodeFile(String filepath) {
        file = new File(filepath);
    }

    void processPerimeters(double extension) {
        for(GCode gCode: gCodes) {
            if(gCode instanceof GCodePerimeter) {
                GCodePerimeter perimeter = (GCodePerimeter) gCode;

                if(perimeter.comment == null || !perimeter.comment.equals("; outer perimeter"))
                    continue;

                perimeter.gCodes.add(0, new GCodeComment("; begin gcode tuner modified perimeter"));
                perimeter.gCodes.add(new GCodeComment("; end gcode tuner modified perimeter"));

                GCodeCommand[] moves = perimeter.gCodes.stream().filter(gCode1 -> gCode1 instanceof GCodeCommand && ((GCodeCommand) gCode1).command.equals("G1") && ((GCodeCommand) gCode1).has('X')).toArray(GCodeCommand[]::new);

                int LAST = moves.length-1;
                Vector2d v1 = new  Vector2d(moves[1].get('X') - moves[0].get('X'), moves[1].get('Y') - moves[0].get('Y'));
                Vector2d v2 = new  Vector2d(moves[LAST].get('X') - moves[LAST-1].get('X'), moves[LAST].get('Y') - moves[LAST-1].get('Y'));

                double extrusionRate = 0.0;
                if(moves[1].has('E'))
                    extrusionRate = 100 * moves[1].get('E') / v1.length();

                v2.normalize();
                v2.scale(extension);

                GCodeCommand cmd = new GCodeCommand("G1", "; inserted by tuner");
                cmd.put('X', moves[0].get('X') - v2.x);
                cmd.put('Y', moves[0].get('Y') - v2.y);
                perimeter.gCodes.add(1, cmd);

                //moves[0].put('X', moves[0].get('X') - v1.x);
                //moves[0].put('Y', moves[0].get('Y') - v1.y);
            }
        }
    }

    String writeCopy() {
        String newFileName = file.getAbsolutePath().replace(".gcode", "_tuned.gcode");
        try (PrintWriter writer = new PrintWriter(newFileName, "UTF-8")) {
            serialize(writer);
        } catch (IOException ignored) {}
        return newFileName;
    }

    /**
     * load unprocessed gcodes
     */
    void load() {
        try (FileInputStream fstream = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fstream);
             BufferedReader fileStream = new BufferedReader(new InputStreamReader(dis))
        ) {
            String strLine;
            int nbLines = 0;
            while ((strLine = fileStream.readLine()) != null) {
                nbLines++;
                GCode gcode = GCodeFactory.produceFromString(strLine);
                if (gcode != null) {
                    gcode.originalLineNumber = nbLines;
                    gCodes.add(gcode);
                }
            }

        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void groupPerimeters() {
        List<GCode> oriCodes = gCodes;
        gCodes = new ArrayList<>();

        Map<Point3d, Integer> perimeterPoints = new HashMap<>();

        double last_z = 0;
        Point3d p = new Point3d(0, 0, 0);
        Point3d lastPointPut = new Point3d(p);

        List<GCode> gCodesTmp = new ArrayList<>();

        for (int i = 0; i < oriCodes.size(); i++) {
            GCode gcode = oriCodes.get(i);
            if (gcode instanceof GCodeCommand) {
                GCodeCommand cmd = (GCodeCommand) gcode;
                if (cmd.has('X'))
                    p.x = cmd.get('X');
                if (cmd.has('Y'))
                    p.y = cmd.get('Y');
                if (cmd.has('Z'))
                    p.z = cmd.get('Z');
            }

            // no loop found, either z was changed or eof
            if (last_z != p.z || i == oriCodes.size() - 1) {
                gCodes.addAll(gCodesTmp);

                // add X and Y explicitly to beginning of perimeter so they can be changed
                if (gcode instanceof GCodeCommand) {
                    GCodeCommand cmd = (GCodeCommand) gcode;
                    cmd.put('X', p.x);
                    cmd.put('Y', p.y);
                }

                gCodesTmp.clear();
                gCodesTmp.add(gcode);

                perimeterPoints.clear();
                perimeterPoints.put(p, 0);

                lastPointPut.set(p.x, p.y, p.z);
            } else {
                gCodesTmp.add(gcode);
                int curIx = gCodesTmp.size() - 1;

                if (perimeterPoints.containsKey(p)) {
                    if (!lastPointPut.equals(p)) {
                        int firstPerimeterPtIx = perimeterPoints.get(p);

                        // bottom part
                        gCodes.addAll(gCodesTmp.subList(0, firstPerimeterPtIx));

                        // top part
                        GCodePerimeter perimeterGroup = new GCodePerimeter();
                        perimeterGroup.gCodes.addAll(gCodesTmp.subList(firstPerimeterPtIx, curIx + 1));
                        perimeterGroup.originalLineNumber = perimeterGroup.gCodes.get(0).originalLineNumber;

                        // possible perimeter comment from file
                        int j = 0;
                        while(j++ < 5)
                            if(perimeterGroup.originalLineNumber - j >= 0 && oriCodes.get(perimeterGroup.originalLineNumber - j).comment != null) {
                                perimeterGroup.comment = oriCodes.get(perimeterGroup.originalLineNumber - j).comment;
                                break;
                            }

                        perimeterGroup.updateBbx();

                        gCodes.add(perimeterGroup);

                        perimeterPoints.clear();
                        gCodesTmp.clear();
                    }
                } else {
                    perimeterPoints.put(p, curIx);
                    lastPointPut.set(p.x, p.y, p.z);
                }
            }

            last_z = p.z;
        }
    }

    Stream<GCode> getPerimeters() {
        return gCodes.stream().filter(gCode -> gCode instanceof GCodePerimeter);
    }

    private void serialize(PrintWriter file) throws IOException {
        for (GCode gCode : gCodes) gCode.serialize(file);
    }
}
