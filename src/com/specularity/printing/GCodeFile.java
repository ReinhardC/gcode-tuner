package com.specularity.printing;

import com.specularity.printing.GCodes.GCode;
import com.specularity.printing.GCodes.GCodeCommand;
import com.specularity.printing.GCodes.GCodeFactory;
import com.specularity.printing.GCodes.GCodePerimeter;
import sun.misc.GC;

import javax.vecmath.Vector3d;
import java.io.*;
import java.util.*;
import java.util.stream.Stream;

import static com.specularity.printing.VectorTools.calculateTunedPath;

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
        for (GCode gCode : gCodes) {
            if (gCode instanceof GCodePerimeter) {
                GCodePerimeter perimeter = (GCodePerimeter) gCode;

                //if(perimeter.comment == null || !perimeter.comment.equals("; outer perimeter"))
                //    continue;

                List<GCode> newGCodes = calculateTunedPath(perimeter.gCodesLoop);

                Object[] xyOnlyTravelMove = perimeter.gCodesTravel.stream().filter(gCode1 -> gCode1 instanceof GCodeCommand && ((GCodeCommand) gCode1).has('X') && !((GCodeCommand) gCode1).has('Z')).toArray();
                Object[] zOnlyTravelMove = perimeter.gCodesTravel.stream().filter(gCode1 -> gCode1 instanceof GCodeCommand && ((GCodeCommand) gCode1).has('Z') && !((GCodeCommand) gCode1).has('X')).toArray();

                if(xyOnlyTravelMove.length == 1)
                    ((GCodeCommand) xyOnlyTravelMove[0]).putVector2d(newGCodes.get(0).getState().getXY());

                if(xyOnlyTravelMove.length == 1 && zOnlyTravelMove.length == 1 && (((GCodeCommand) xyOnlyTravelMove[0]).getState().getOriginalLineNumber() < ((GCodeCommand) zOnlyTravelMove[0]).getState().getOriginalLineNumber())) {
                    GCodeCommand tmp = new GCodeCommand(((GCodeCommand) xyOnlyTravelMove[0]));
                    ((GCodeCommand) xyOnlyTravelMove[0]).set(((GCodeCommand) zOnlyTravelMove[0]));
                    ((GCodeCommand) zOnlyTravelMove[0]).set(tmp);
                }

                newGCodes.remove(0);
                ((GCodeCommand) newGCodes.get(0)).put('F', ((GCodeCommand) perimeter.gCodesLoop.get(0)).get('F'));

                perimeter.gCodesLoop = newGCodes;

                //for (GCode newGCode : newGCodes)
                //    System.out.println(newGCode);
            }
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

    /**
     * load unprocessed gcodes
     */
    void load() {
        // wtf
        MachineState currentState = new MachineState();

        try (BufferedReader fileStream = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(file))))) {
            String strLine;
            int nbLines = 0;
            while ((strLine = fileStream.readLine()) != null) {
                nbLines++;
                GCode gcode = GCodeFactory.produceFromString(strLine);
                if (gcode != null) {
                    if (gcode instanceof GCodeCommand) {
                        GCodeCommand cmd = (GCodeCommand) gcode;
                        if (cmd.command.equals("G1")) {
                            if (cmd.has('X'))
                                currentState.updateX(cmd.get('X'));

                            if (cmd.has('Y'))
                                currentState.updateY(cmd.get('Y'));

                            if (cmd.has('Z'))
                                currentState.updateZ(cmd.get('Z'));
                        }
                    }

                    currentState.setOriginalLineNumber(nbLines);
                    gcode.setState(new MachineState(currentState));
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
        List<GCode> oriFileCodes = gCodes;
        gCodes = new ArrayList<>();
        List<GCode> gCodesTmp = new ArrayList<>();

        Vector3d lastToolheadPosition = new Vector3d(0, 0, -1);

        Map<Vector3d, Integer> perimeterPointLookup = new HashMap<>();

        for (int i = 0; i < oriFileCodes.size(); i++) {
            GCode gcode = oriFileCodes.get(i);

            Vector3d currentToolheadPosition = gcode.getState().getToolheadPosition();

            if (i != oriFileCodes.size() - 1) {
                gCodesTmp.add(gcode);
                int curIx = gCodesTmp.size() - 1;

                if (perimeterPointLookup.containsKey(currentToolheadPosition)) {
                    if (!lastToolheadPosition.equals(currentToolheadPosition)) {
                        int firstPerimeterPtIx = perimeterPointLookup.get(currentToolheadPosition);

                        GCode firstLoopPoint = gCodesTmp.get(firstPerimeterPtIx);

                        // backtrack to find travel moves
                        int beginTravelMovesIx = firstPerimeterPtIx;
                        while (gCodesTmp.get(beginTravelMovesIx).getState().isValidXY() &&
                                gCodesTmp.get(beginTravelMovesIx).getState().getXY().equals(firstLoopPoint.getState().getXY()) &&
                                beginTravelMovesIx > 0)
                            beginTravelMovesIx--;

                        if(!(gCodesTmp.get(beginTravelMovesIx).getState().isValidXY() &&
                                gCodesTmp.get(beginTravelMovesIx).getState().getXY().equals(firstLoopPoint.getState().getXY())))
                            beginTravelMovesIx++;

                        int endTravelMovesIx = firstPerimeterPtIx;
                        while (gCodesTmp.get(endTravelMovesIx).getState().isValidXY() &&
                                gCodesTmp.get(endTravelMovesIx).getState().getXY().equals(firstLoopPoint.getState().getXY()))
                            endTravelMovesIx++;

                        // unknown trailing commands (infill, support...) that are not travel moves
                        gCodes.addAll(gCodesTmp.subList(0, beginTravelMovesIx));

                        GCodePerimeter perimeterGroup = new GCodePerimeter();

                        //travel moves
                        perimeterGroup.gCodesTravel.addAll(gCodesTmp.subList(beginTravelMovesIx, endTravelMovesIx));

                        //loop points
                        perimeterGroup.gCodesLoop.addAll(gCodesTmp.subList(endTravelMovesIx, curIx + 1));
                        perimeterGroup.setState(perimeterGroup.gCodesLoop.get(0).getState());
                        //perimeterGroup.getState().setOriginalLineNumber(perimeterGroup.gCodesLoop.get(0).getState().getOriginalLineNumber());

                        // possible perimeter comment from file
                        int j = 0;
                        while (j++ < 5)
                            if (perimeterGroup.getState().getOriginalLineNumber() - j >= 0 && oriFileCodes.get(perimeterGroup.getState().getOriginalLineNumber() - j).comment != null) {
                                perimeterGroup.comment = oriFileCodes.get(perimeterGroup.getState().getOriginalLineNumber() - j).comment;
                                break;
                            }

                        perimeterGroup.updateBbx();

                        gCodes.add(perimeterGroup);

                        perimeterPointLookup.clear();
                        gCodesTmp.clear();
                    }
                } else if (gcode.getState().isValidXY())
                    perimeterPointLookup.put(new Vector3d(currentToolheadPosition), curIx);
            } else {
                gCodes.addAll(gCodesTmp);

                // add X and Y explicitly to beginning of perimeter so they can be changed

                gCodesTmp.clear();
                gCodesTmp.add(gcode);

                perimeterPointLookup.clear();

                if (gcode.getState().isValidXY())
                    perimeterPointLookup.put(new Vector3d(gcode.getState().getToolheadPosition()), 0);

            }

            lastToolheadPosition.set(currentToolheadPosition);
        }
    }

    Stream<GCode> getPerimeters() {
        return gCodes.stream().filter(gCode -> gCode instanceof GCodePerimeter);
    }

    private void serialize(PrintWriter file) throws IOException {
        for (GCode gCode : gCodes) gCode.serialize(file);
    }
}
