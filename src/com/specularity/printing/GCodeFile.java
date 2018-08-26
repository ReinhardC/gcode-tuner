package com.specularity.printing;

import com.specularity.printing.GCodes.*;

import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static com.specularity.printing.VectorTools.*;
import static com.specularity.printing.tuner.setPointsEnd;
import static com.specularity.printing.tuner.setPointsStart;

class GCodeFile {
    private final File file;

    List<GCode> gCodes = new ArrayList<>();

    GCodeFile(String filepath) {
        file = new File(filepath);
    }

    /**
     * TODO generalize Perimeters to Extrusions (Perimeter, 1st inner, 2nd inner, last inner, Infill etc.), travel moves and other moves
     */
    void modifyPerimeters() {
        for(GCode gCode: gCodes) {
            if(gCode instanceof GCodePerimeter) {
                GCodePerimeter perimeter = (GCodePerimeter) gCode;

                //if(perimeter.comment == null || !perimeter.comment.equals("; outer perimeter"))
                //    continue;

                perimeter.gCodesMoves.add(0, new GCodeComment("; begin gcode tuner modified perimeter"));

                double perimeterExtrudeFactor = Heuristics.getExtrudeFactor(perimeter.gCodesMoves);

                List<Vector2d> originalMoves = perimeter.gCodesMoves.stream()
                        .filter(gCode1 -> gCode1 instanceof GCodeCommand && ((GCodeCommand) gCode1).isPosition())
                        .map(gCode1 -> new Vector2d(((GCodeCommand) gCode1).get('X'), ((GCodeCommand) gCode1).get('Y'))).collect(Collectors.toList());

                originalMoves.remove(originalMoves.size()-1); // remove last point which is same as first point

                Double totalPathAngle = 0.0;
                Vector2d pathOffset = new Vector2d(0.0, 0.0); // used for rotation

                Vector2d previousMove = null;
                double nextExtrusionPoint = 0.0;

                ArrayList<GCode> newMoves = new ArrayList<>();

                //
                // replace start of extruded path
                //
                // reversed so the rotation can be applied to the points
                //

                List<List<Vector2d>> startPaths = new ArrayList<>();

                for (int i = setPointsStart.size() - 2; i >= 0; i--)
                {
                    double from = setPointsStart.get(i).getOffset();
                    double to = setPointsStart.get(i+1).getOffset();

                    List<Vector2d> startPath = resamplePath(originalMoves, from, to);
                    for (Vector2d v: startPath)
                        v.add(pathOffset);

                    totalPathAngle += setPointsStart.get(i+1).getAngle();

                    Vector2d newPathOffset = new Vector2d(startPath.get(0));
                    rotatePathAround(startPath, startPath.get(startPath.size() - 1), totalPathAngle);
                    newPathOffset.sub(startPath.get(0));
                    pathOffset.sub(newPathOffset);

                    startPath.forEach(VectorTools::round3Vector2d);
                    removeDoublePoints(startPath);
                    if(i != 0)
                        startPath.remove(0);

                    startPaths.add(0, startPath);
                }


                for (int i = 0; i < setPointsStart.size()-1; i++) // add startPaths in forward order
                {
                    System.out.println(startPaths.get(i) + " (startPath with " + setPointsStart.get(i+1).getExtrusionPct() + ")");
                    for (Vector2d v : startPaths.get(i)) {
                        GCodeCommand newMove = new GCodeCommand("G1", "; GCodeTuner - replaced start points");
                        newMove.putVector2d(v);
                        if(previousMove != null) {
                            previousMove.sub(v);
                            nextExtrusionPoint += previousMove.length() * perimeterExtrudeFactor * (setPointsStart.get(i+1).getExtrusionPct() / 100);
                        }
                        newMove.put('E', nextExtrusionPoint);
                        newMoves.add(newMove);
                        previousMove = v;
                    }
                }

                //
                //                       REPLACE MAIN EXTRUSION
                //

                List<Vector2d> mainExtrudedPath = resamplePath(originalMoves, setPointsStart.get(setPointsStart.size() - 1).getOffset(), getCycleLength(originalMoves) + setPointsEnd.get(0).getOffset());
                mainExtrudedPath.remove(0);
                System.out.println(mainExtrudedPath + " (mainExtrudedPath with 100)");
                for (Vector2d v : mainExtrudedPath) {
                    GCodeCommand newMove = new GCodeCommand("G1", null);
                    newMove.putVector2d(v);
                    if(previousMove != null) {
                        previousMove.sub(v);
                        nextExtrusionPoint += previousMove.length() * perimeterExtrudeFactor;
                    }
                    newMove.put('E', nextExtrusionPoint);
                    newMoves.add(newMove);
                    previousMove = v;
                }

                //
                // replace end of extruded path
                //

                pathOffset.set(0., 0.);
                totalPathAngle = 0.;

                for (int i = 0; i < setPointsEnd.size()-1; i++)
                {
                    double from = getCycleLength(originalMoves) + setPointsEnd.get(i).getOffset();
                    double to =  getCycleLength(originalMoves) + setPointsEnd.get(i+1).getOffset();

                    List<Vector2d> endPath = resamplePath(originalMoves, from, to);
                    for (Vector2d v: endPath)
                        v.add(pathOffset);

                    totalPathAngle += -setPointsEnd.get(i).getAngle(); // negate so positive angle is always inward into model

                    Vector2d newPathOffset = new Vector2d(endPath.get(endPath.size()-1));
                    rotatePathAround(endPath, endPath.get(0), totalPathAngle);
                    newPathOffset.sub(endPath.get(endPath.size()-1));
                    pathOffset.sub(newPathOffset);

                    endPath.forEach(VectorTools::round3Vector2d);
                    removeDoublePoints(endPath);
                    endPath.remove(0);

                    System.out.println(endPath + " (endPath with " + setPointsEnd.get(i+1).getExtrusionPct() + ")");
                    for (Vector2d v : endPath) {
                        GCodeCommand newMove = new GCodeCommand("G1", "; GCodeTuner - replaced end points");
                        newMove.putVector2d(v);
                        if(previousMove != null) {
                            previousMove.sub(v);
                            nextExtrusionPoint += previousMove.length() * perimeterExtrudeFactor * (setPointsEnd.get(i+1).getExtrusionPct() / 100);
                        }
                        newMove.put('E', nextExtrusionPoint);
                        newMoves.add(newMove);
                        previousMove = v;
                    }
                }

                System.out.println("");
                for (GCode newMove : newMoves)
                    System.out.println(newMove);

                break;

                 /* -0.1mm 10%
                -0.5mm

                GCodeCommand[] moves = perimeter.gCodesMoves.stream().filter(gCode1 -> gCode1 instanceof GCodeCommand && ((GCodeCommand) gCode1).isPosition()).toArray(GCodeCommand[]::new);

                int LAST = moves.length-1;

                double extrusionRate = 0.0;
                if(moves[1].has('E')) {
                    extrusionRate = 100 * moves[1].get('E') / v1.length();
                }

                Vector2d v2 = new  Vector2d(moves[LAST].get('X') - moves[LAST-1].get('X'), moves[LAST].get('Y') - moves[LAST-1].get('Y'));
                v2.normalize();
                v2.scale(extension);

                GCodeCommand cmd = new GCodeCommand("G1", "; inserted by tuner");
                cmd.put('X', moves[0].get('X') - v2.x);
                cmd.put('Y', moves[0].get('Y') - v2.y);
                perimeter.gCodesMoves.add(1, cmd);

                Vector2d v1 = new  Vector2d(moves[1].get('X') - moves[0].get('X'), moves[1].get('Y') - moves[0].get('Y'));
                v1.normalize();
                v1.scale(extension * 2);

                GCodeCommand cmd2 = new GCodeCommand("G1", "; inserted by tuner");
                cmd2.put('X', moves[LAST].get('X') + v1.x);
                cmd2.put('Y', moves[LAST].get('Y') + v1.y);
                cmd2.put('E', moves[LAST].get('E') + (v1.length() * extrusionRate) / 100.0);
                perimeter.gCodesMoves.add(cmd2);

                perimeter.gCodesMoves.add(new GCodeComment("; end gcode tuner modified perimeter"));

                //moves[0].put('X', moves[0].get('X') - v1.x);
                //moves[0].put('Y', moves[0].get('Y') - v1.y);
                */
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
        // wtf
        try (BufferedReader fileStream = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(file))))) {
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

    /**
     * group continuous extrusions
     * (an "extrusion" is a perimeter, infill, support) along with its travel moves
      */
    void groupExtrusions() {

    }

    void groupPerimeters() {
        List<GCode> oriCodes = gCodes;
        gCodes = new ArrayList<>();

        boolean bXYPositioned = false;

        Map<Point3d, Integer> perimeterPoints = new HashMap<>();

        double last_z = 0;

        Point3d p = new Point3d(0, 0, 0);
        Point3d lastPointPut = new Point3d(p);

        List<GCode> gCodesTmp = new ArrayList<>();

        for (int i = 0; i < oriCodes.size(); i++) {
            GCode gcode = oriCodes.get(i);
            if (gcode instanceof GCodeCommand) {
                GCodeCommand cmd = (GCodeCommand) gcode;
                if(cmd.command.equals("G1")) {
                    if (cmd.has('X')) {
                        bXYPositioned = true;
                        p.x = cmd.get('X');
                    }
                    if (cmd.has('Y')) {
                        bXYPositioned = true;
                        p.y = cmd.get('Y');
                    }
                    if (cmd.has('Z')) {
                        p.z = cmd.get('Z');
                    }
                }
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
                if(bXYPositioned) {
                    perimeterPoints.put(new Point3d(p), 0);
                    lastPointPut.set(p.x, p.y, p.z);
                }
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
                        perimeterGroup.gCodesMoves.addAll(gCodesTmp.subList(firstPerimeterPtIx, curIx + 1));
                        perimeterGroup.originalLineNumber = perimeterGroup.gCodesMoves.get(0).originalLineNumber;

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
                } else if(bXYPositioned){
                    perimeterPoints.put(new Point3d(p), curIx);
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
