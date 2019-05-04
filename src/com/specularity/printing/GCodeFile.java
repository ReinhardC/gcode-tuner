package com.specularity.printing;

import com.specularity.printing.GCodes.*;

import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.specularity.printing.tuner.log;

class GCodeFile {
    public final File file;

    List<GCode> gCodes = new ArrayList<>();

    GCodeFile(String filepath) {
        file = new File(filepath);
    }

    private int findPositionInGCodes(List<GCode> gCodes, Vector3d pt, double tolerance) {
        for (int i = 0; i < gCodes.size(); i++) {
            GCode gCode = gCodes.get(i);
            Vector3d v = new Vector3d(gCode.getState().getXYZ());
            v.sub(pt);
            if(pt.z != v.z && v.length()<tolerance)
                return i;
        }
        return -1;
    }

    void load() {
        MachineState currentState = new MachineState();
        String lastMoveCommand = "G1";
        gCodes.clear();
        // wtf
        try (BufferedReader fileStream = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(file))))) {
            String strLine;
            int nbLines = 0;
            while ((strLine = fileStream.readLine()) != null) {
                nbLines++;
                List<GCode> gcodes = GCodeFactory.gCodesFromLine(strLine, lastMoveCommand);
                for (GCode gcode : gcodes) {
                    if (gcode != null) {
                        if (gcode instanceof GCodeCommand) {
                            GCodeCommand cmd = (GCodeCommand) gcode;
                            if (cmd.command.equals("G0") || cmd.command.equals("G1") || cmd.command.equals("G2") || cmd.command.equals("G3")) {
                                lastMoveCommand = cmd.command;
                                if (cmd.has('X'))
                                    currentState.updateX(cmd.get('X'));
                                if (cmd.has('Y'))
                                    currentState.updateY(cmd.get('Y'));
                                if (cmd.has('Z'))
                                    currentState.updateZ(cmd.get('Z'));
                                if (cmd.has('F'))
                                    currentState.updateFeedrate(cmd.get('F'));
                            }
                        }
                        currentState.setOriginalLineNumber(nbLines);
                        gcode.setState(new MachineState(currentState));
                        gCodes.add(gcode);
                    }
                    else log("unrecognized GCode: " + strLine);
                }
            }
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }

        //
        //              group commands into perimeters, and perimeters into perimeter groups
        //

        List<GCode> oriFileCodes = gCodes;
        gCodes = new ArrayList<>();
        List<GCode> gCodesTmp = new ArrayList<>();

        Vector3d lastToolheadPosition = new Vector3d(0, 0, -1);
        GCodePerimeter previousPerimeter = null;
        GCodePerimeterGroup currentPerimeterGroup = null;

        for (int i = 0; i < oriFileCodes.size(); i++) {
            GCode gcode = oriFileCodes.get(i);

            Vector3d currentToolheadPosition = gcode.getState().getXYZ();

            if (i == oriFileCodes.size() - 1) {
                if(currentPerimeterGroup != null)
                    gCodes.add(currentPerimeterGroup);
                gCodesTmp.add(gcode);
                gCodes.addAll(gCodesTmp);
                gCodesTmp.clear();
                gCodesTmp.add(gcode);
            }
            else {
                int firstPerimeterPtIx = findPositionInGCodes(gCodesTmp, currentToolheadPosition, 0.003);
                
                gCodesTmp.add(gcode);

                if ((firstPerimeterPtIx != -1) && 
                        (!lastToolheadPosition.equals(currentToolheadPosition)) && 
                        (gCodesTmp.size() - firstPerimeterPtIx > 4)) {
                    
                    GCode firstLoopPoint = gCodesTmp.get(firstPerimeterPtIx);

                    // backtrack to find travel moves
                    int beginTravelMovesIx = firstPerimeterPtIx;

                    // scans very greedily, everything not extruding is a "travel move"
                    while (!(gCodesTmp.get(beginTravelMovesIx) instanceof GCodeCommand &&
                            ((GCodeCommand) gCodesTmp.get(beginTravelMovesIx)).has('E') &&
                            ((GCodeCommand) gCodesTmp.get(beginTravelMovesIx)).get('E') > 0.0) &&
                            beginTravelMovesIx > 0)
                        beginTravelMovesIx--;

                    if (gCodesTmp.get(beginTravelMovesIx) instanceof GCodeCommand &&
                            ((GCodeCommand) gCodesTmp.get(beginTravelMovesIx)).has('E') &&
                            ((GCodeCommand) gCodesTmp.get(beginTravelMovesIx)).get('E') > 0.0)
                        beginTravelMovesIx++;

                    int endTravelMovesIx = firstPerimeterPtIx;
                    while (endTravelMovesIx < gCodesTmp.size() &&
                            gCodesTmp.get(endTravelMovesIx).getState().isValidXY() &&
                            gCodesTmp.get(endTravelMovesIx).getState().getXY().equals(firstLoopPoint.getState().getXY()))
                        endTravelMovesIx++;

                    GCodePerimeter perimeter = new GCodePerimeter();

                    if (currentPerimeterGroup == null)
                        currentPerimeterGroup = new GCodePerimeterGroup(perimeter);

                    // flush group in case extra moves were found
                    if(beginTravelMovesIx > 0 && currentPerimeterGroup.perimeters.size() > 0) {
                        if (currentPerimeterGroup.perimeters.size() != 1) {
                            currentPerimeterGroup.setState(currentPerimeterGroup.perimeters.get(0).getState());
                            currentPerimeterGroup.comment = currentPerimeterGroup.perimeters.get(0).comment;
                            currentPerimeterGroup.updateBbx();
                            gCodes.add(currentPerimeterGroup);
                        } else
                            gCodes.add(currentPerimeterGroup.perimeters.get(0));

                        currentPerimeterGroup = null;
                        previousPerimeter = null;
                    }

                    // unknown trailing commands (infill, support...) that are not travel moves
                    gCodes.addAll(gCodesTmp.subList(0, beginTravelMovesIx));
                    
                    
                    //travel moves
                    perimeter.gCodesTravel.addAll(gCodesTmp.subList(beginTravelMovesIx, endTravelMovesIx));

                    //loop points
                    perimeter.gCodesLoop.addAll(gCodesTmp.subList(endTravelMovesIx, gCodesTmp.size()));
                    if (perimeter.gCodesLoop.size() > 0)
                        perimeter.setState(perimeter.gCodesLoop.get(0).getState());

                    // possible perimeter comment from file
                    int j = 0;
                    while (j++ < 5)
                        if (perimeter.getState().getOriginalLineNumber() - j >= 0 && oriFileCodes.get(perimeter.getState().getOriginalLineNumber() - j).comment != null) {
                            perimeter.comment = oriFileCodes.get(perimeter.getState().getOriginalLineNumber() - j).comment;
                            break;
                        }

                    perimeter.updateBbx();
                        
                    boolean bKeepCollecting = false;

                    if (previousPerimeter == null)
                        bKeepCollecting = true;
                    else if (beginTravelMovesIx == 0 && // no extra commands allowed between grouped perimeters */ 
                            previousPerimeter.getState().getZ() == perimeter.getState().getZ()) {
                        GCodeCommand lastTravelMoveThis = GCodeToolkit.getLastXYTravelMove(perimeter.gCodesTravel);
                        GCodeCommand lastTravelMovePrevious = GCodeToolkit.getLastXYTravelMove(previousPerimeter.gCodesTravel);
                        if (lastTravelMoveThis != null && lastTravelMovePrevious != null) {
                            Vector2d v = lastTravelMoveThis.getVector2d();
                            v.sub(lastTravelMovePrevious.getVector2d());
                            if (v.length() < 0.7) {
                                currentPerimeterGroup.perimeters.add(perimeter);
                                bKeepCollecting = true;
                            }
                        }
                    }

                    // was not added?
                    if (!bKeepCollecting) {

                        if (currentPerimeterGroup.perimeters.size() != 1) {
                            currentPerimeterGroup.setState(currentPerimeterGroup.perimeters.get(0).getState());
                            currentPerimeterGroup.comment = currentPerimeterGroup.perimeters.get(0).comment;
                            currentPerimeterGroup.updateBbx();
                            gCodes.add(currentPerimeterGroup);
                        } else
                            gCodes.add(currentPerimeterGroup.perimeters.get(0));

                        currentPerimeterGroup = new GCodePerimeterGroup(perimeter);
                    }
;
                    gCodesTmp.clear();

                    previousPerimeter = perimeter;
                }
            }

            lastToolheadPosition.set(currentToolheadPosition);
        }
        
        return;
    }

    Stream<GCode> getPerimeters() {
        return gCodes.stream().filter(gCode -> gCode instanceof GCodePerimeter);
    }

    private void serialize(PrintWriter file) throws IOException {

        for (GCode gCode : gCodes) gCode.serialize(file);
    }

    String writeCopy() {

        String newFileName = file.getAbsolutePath().replace(".gcode", "_tuned.gcode");
        try (PrintWriter writer = new PrintWriter(newFileName, "UTF-8")) {
            serialize(writer);
        } catch (IOException ignored) {
        }
        return newFileName;
    }
}
