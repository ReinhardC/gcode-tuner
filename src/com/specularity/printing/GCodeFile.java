package com.specularity.printing;

import com.specularity.printing.GCodes.*;

import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;
import java.io.*;
import java.util.*;
import java.util.stream.Stream;

import static com.specularity.printing.tuner.logArea;

class GCodeFile {
    private final File file;

    List<GCode> gCodes = new ArrayList<>();

    GCodeFile(String filepath) {
        file = new File(filepath);
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
                    else logArea.appendText("unrecognized GCode: " + strLine + "\n");
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

        Map<Vector3d, Integer> perimeterPointLookup = new HashMap<>();

        for (int i = 0; i < oriFileCodes.size(); i++) {
            GCode gcode = oriFileCodes.get(i);

            Vector3d currentToolheadPosition = gcode.getState().getToolheadPosition();

            if (i == oriFileCodes.size() - 1) {
                gCodes.addAll(gCodesTmp);
                gCodesTmp.clear();
                gCodesTmp.add(gcode);
            }
            else {
                gCodesTmp.add(gcode);
                int tmpLastIx = gCodesTmp.size() - 1;
                
               if (!perimeterPointLookup.containsKey(currentToolheadPosition)) {
                    if (gcode.getState().isValidXY())
                        perimeterPointLookup.put(new Vector3d(currentToolheadPosition), tmpLastIx);
               } else if (!lastToolheadPosition.equals(currentToolheadPosition)) {
                   int firstPerimeterPtIx = perimeterPointLookup.get(currentToolheadPosition);

                   GCode firstLoopPoint = gCodesTmp.get(firstPerimeterPtIx);

                   // backtrack to find travel moves
                   int beginTravelMovesIx = firstPerimeterPtIx;

//                        while (gCodesTmp.get(beginTravelMovesIx).getState().isValidXY() &&
//                     rr           gCodesTmp.get(beginTravelMovesIx).getState().getXY().equals(firstLoopPoint.getState().getXY()) &&
//                                beginTravelMovesIx > 0) {
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
                   
                   //travel moves
                   perimeter.gCodesTravel.addAll(gCodesTmp.subList(beginTravelMovesIx, endTravelMovesIx));

                   //loop points
                   perimeter.gCodesLoop.addAll(gCodesTmp.subList(endTravelMovesIx, tmpLastIx + 1));
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

                   boolean bGroupFinished = true;

                   // assign size ids to perimeters (works for outer perimeters only)
                   if (previousPerimeter != null && previousPerimeter.getState().getZ() == perimeter.getState().getZ()) // same layer?
                   {
                       GCodeCommand lastTravelMoveThis = GCodeToolkit.getLastXYTravelMove(perimeter.gCodesTravel);
                       GCodeCommand lastTravelMovePrevious = GCodeToolkit.getLastXYTravelMove(previousPerimeter.gCodesTravel);
                       if (lastTravelMoveThis != null && lastTravelMovePrevious != null) {
                           Vector2d v = lastTravelMoveThis.getVector2d();
                           v.sub(lastTravelMovePrevious.getVector2d());
                           if (v.length() < 0.7/*mm*/) {
                               currentPerimeterGroup.perimeters.add(perimeter);
                               bGroupFinished = false;
                           }
                       }
                   }

                   // was not added?
                   if (bGroupFinished) {
                       if (previousPerimeter != null) {
                           if (currentPerimeterGroup.perimeters.size() != 1) {
                               currentPerimeterGroup.setState(currentPerimeterGroup.perimeters.get(0).getState());
                               currentPerimeterGroup.updateBbx();
                               gCodes.add(currentPerimeterGroup);
                           } else
                               gCodes.add(currentPerimeterGroup.perimeters.get(0));
                       }
                       currentPerimeterGroup = new GCodePerimeterGroup(perimeter);
                   }

                   // unknown trailing commands (infill, support...) that are not travel moves
                   gCodes.addAll(gCodesTmp.subList(0, beginTravelMovesIx));

                   perimeterPointLookup.clear();
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
