package com.specularity.printing;

import com.specularity.printing.GCodes.*;

import javax.vecmath.Vector3d;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private void submitAndClearTmp(List<GCode> gCodesTmp, GCode group) {
        
        group.setState(gCodesTmp.get(gCodesTmp.size()-1).getState());
        // gCodesTmp.forEach(gCode -> gCode.setState(null));
        
        if(group instanceof GCodeTravel)
            ((GCodeTravel)group).gCodesTravel.addAll(gCodesTmp);
        else if(group instanceof GCodeTravelRetracted)
            ((GCodeTravelRetracted)group).gCodes.addAll(gCodesTmp);
        else if(group instanceof GCodeExtrusion)
            ((GCodeExtrusion)group).gCodesMoves.addAll(gCodesTmp);
        gCodesTmp.clear();
    }

    void load() {
        MachineState currentState = new MachineState();
        String lastMoveCommand = "G1";

         boolean isRetracted = false;
        
        gCodes.clear();

        GCode collectingGroup = null;

        try (BufferedReader fileStream = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(file))))) 
        {
            List<GCode> gCodesTmp = new ArrayList<>();
            GCode GCodePrev = null;

            int nbLines = 0;
            String strLine;
            while ((strLine = fileStream.readLine()) != null) {
                nbLines++;
                List<GCode> gCodesLine = GCodeFactory.gCodesFromLine(strLine, lastMoveCommand); // in rare cases, several per line
                for (GCode gCode : gCodesLine) 
                {
                    if (gCode instanceof GCodeCommand) {
                        GCodeCommand cmd = (GCodeCommand) gCode;
                        
                        if (cmd.isMoveCommand()) {
                            lastMoveCommand = cmd.command; // this is used for the next command-less lines like "X12 Y2 Z19"  
                            if (cmd.has('X')) currentState.updateX(cmd.get('X'));
                            if (cmd.has('Y')) currentState.updateY(cmd.get('Y'));
                            if (cmd.has('Z')) currentState.updateZ(cmd.get('Z'));
                            if (cmd.has('F')) currentState.updateFeedrate(cmd.get('F'));
                            if (cmd.has('E')) currentState.updateE(cmd.get('E'));
                        } 
                        else if(cmd.command.equals("M83")) {
                            currentState.setIsAbsoluteExtrusion(false);
                            gCode = new GCodeComment(";M83 removed by GCodeTuner");
                        }
                        else if(cmd.command.equals("M82")) {
                            currentState.setIsAbsoluteExtrusion(true);
                        }
                        else if(cmd.command.equals("G92")) {
                            if (cmd.has('E') && currentState.isAbsoluteExtrusion()) {
                                currentState.setZeroE(currentState.getE() - cmd.get('E'));
                                currentState.updateE(cmd.get('E'));
                            }
                            if (cmd.has('X')) {
                                currentState.setZeroX(currentState.getX() - cmd.get('X'));
                                currentState.updateX(cmd.get('X'));
                            }
                            if (cmd.has('Y')) {
                                currentState.setZeroY(currentState.getY() - cmd.get('Y'));
                                currentState.updateY(cmd.get('Y'));
                            }
                            if (cmd.has('Z')) {
                                currentState.setZeroZ(currentState.getZ() - cmd.get('Z'));
                                currentState.updateZ(cmd.get('Z'));
                            }
                            gCode = new GCodeComment(";G92 removed by GCodeTuner");
                        }
                    }

                    currentState.setOriginalLineNumber(nbLines);

                    gCode.setState(new MachineState(currentState));

                    boolean isTraveling = false;
                    boolean isExtruding = false;
                    boolean isUnretracting = false;
                    // isRetracted remains old state until changed
                    
                    if(gCode instanceof GCodeCommand) {
                        GCodeCommand cmd = ((GCodeCommand) gCode);
                        isTraveling = cmd.isMoveCommand() && GCodePrev != null && cmd.hasXY() && gCode.getState().getE() - GCodePrev.getState().getE() < 0.000005; // moved but didn't extrude
                        isExtruding = cmd.isMoveCommand() && GCodePrev != null && !GCodePrev.getState().checkRetracted() && gCode.getState().getE() - GCodePrev.getState().getE() >= 0.000005; // TODO: test coasting
                        isRetracted = gCode.getState().checkRetracted();
                        isUnretracting = GCodePrev != null && GCodePrev.getState().checkRetracted() && !gCode.getState().checkRetracted();
                    }

                    if (isTraveling && !(isRetracted|| isUnretracting) ) {
                        if(!(collectingGroup instanceof GCodeTravel)) {
                            if(collectingGroup != null) {
                                submitAndClearTmp(gCodesTmp, collectingGroup);
                                gCodes.add(collectingGroup);
                            }
                            collectingGroup = new GCodeTravel();
                        }
                        gCodesTmp.add(gCode);
                    } else if (isRetracted|| isUnretracting) {
                        if(!(collectingGroup instanceof GCodeTravelRetracted)) {
                            if(collectingGroup != null) {
                                submitAndClearTmp(gCodesTmp, collectingGroup);
                                gCodes.add(collectingGroup);
                            }
                            collectingGroup = new GCodeTravelRetracted();
                        }
                        gCodesTmp.add(gCode);
                    } else if(isExtruding) {
                        if(!(collectingGroup instanceof GCodeExtrusion)) {
                            if(collectingGroup != null) {
                                submitAndClearTmp(gCodesTmp, collectingGroup);
                                gCodes.add(collectingGroup);
                            }
                            collectingGroup = new GCodeExtrusion();
                        }
                        gCodesTmp.add(gCode);
                    } else {
                        if(collectingGroup != null) {
                            submitAndClearTmp(gCodesTmp, collectingGroup);
                            gCodes.add(collectingGroup);
                        }
                        gCodes.add(gCode);
                        collectingGroup = null;
                    }
                    GCodePrev = gCode;
                } 
            } 

            if(collectingGroup != null) {
                submitAndClearTmp(gCodesTmp, collectingGroup);
                gCodes.add(collectingGroup);
            }

            System.out.print("\033[H\033[2J");
            
            // resample retraction test
            
            double initialRetraction = 1;
            double retractionPerMmTraveled = 0.5;
            
            List<GCodeTravelRetracted> retractions = gCodes.stream().filter(gCode -> gCode instanceof GCodeTravelRetracted).map(gCode -> (GCodeTravelRetracted) gCode).collect(Collectors.toList());
            for (GCodeTravelRetracted retraction: retractions) 
            {
                List<GCodeCommand> moves = retraction.gCodes.stream().filter(gCodeCmd -> {
                    return gCodeCmd instanceof GCodeCommand 
                            && (((GCodeCommand) gCodeCmd).isXYPositioningCommand() 
                            || ((GCodeCommand) gCodeCmd).has('E'));
                }).map(gCodeCmd->(GCodeCommand)gCodeCmd).collect(Collectors.toList());

                MachineState state = moves.get(0).getState();
                double d = state.getMaxE() - state.getE(); 
                 moves.get(0).put('E', 123.0);//state.getMaxE() - initialRetraction);
                 moves.get(moves.size()-1).put('E', state.getMaxE());
                
                for (GCodeCommand move : moves) {
                    System.out.println(retraction.getState().getOriginalLineNumber() + ": " + (GCodeCommand)move);
                }
                System.out.println("------------------------");
            }
            
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }

        // group commands into ExtrudedPaths
        
        //
        //              group commands into perimeters, and perimeters into perimeter groups
        //

     /*   List<GCode> oriFileCodes = gCodes;
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
                            gCodesTmp.get(endTravelMovesIx).getState().hasXY() &&
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
                    else if (beginTravelMovesIx == 0 && // no extra commands allowed between grouped perimeters 
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
                            gCodesgCodes.add(currentPerimeterGroup.perimeters.get(0));

                        currentPerimeterGroup = new GCodePerimeterGroup(perimeter);
                    }
;
                    gCodesTmp.clear();

                    previousPerimeter = perimeter;
                }
            }

            lastToolheadPosition.set(currentToolheadPosition);
        }
        */
    }

    Stream<GCode> getPerimeters() {
        return gCodes.stream().filter(gCode -> gCode instanceof GCodePerimeter);
    }

    private void serialize(PrintWriter file) throws IOException {
        int i=0;
        for (GCode gCode : gCodes) {
            i++;
            if(gCode!=null)
                gCode.serialize(file);
            else
                gCode.serialize(file);
        }
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
