package com.specularity.printing;

import com.specularity.printing.GCodes.GCode;
import com.specularity.printing.GCodes.GCodeCommand;
import com.specularity.printing.GCodes.GCodeFactory;
import com.specularity.printing.GCodes.GCodePerimeter;

import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;
import java.io.*;
import java.util.*;
import java.util.stream.Stream;

import static com.specularity.printing.VectorTools.getTriAngle;
import static com.specularity.printing.VectorTools.tunePerimeter;
import static com.specularity.printing.tuner.*;

class GCodeFile {
    private final File file;

    List<GCode> gCodes = new ArrayList<>();

    GCodeFile(String filepath) {
        file = new File(filepath);
    }

    void load() {
        MachineState currentState = new MachineState();
        gCodes.clear();
        // wtf
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
                            if (cmd.has('F'))
                                currentState.updateFeedrate(cmd.get('F'));
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

        //
        //              group perimeters
        //

        List<GCode> oriFileCodes = gCodes;
        gCodes = new ArrayList<>();
        List<GCode> gCodesTmp = new ArrayList<>();

        Vector3d lastToolheadPosition = new Vector3d(0, 0, -1);
        GCodePerimeter lastPerimeter = null;

        List<GCodePerimeter> currentPerimeterGroup = new ArrayList<>();

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

                        GCodePerimeter perimeter = new GCodePerimeter();

                        //travel moves
                        perimeter.gCodesTravel.addAll(gCodesTmp.subList(beginTravelMovesIx, endTravelMovesIx));

                        //loop points
                        perimeter.gCodesLoop.addAll(gCodesTmp.subList(endTravelMovesIx, curIx + 1));
                        perimeter.setState(perimeter.gCodesLoop.get(0).getState());

                        // possible perimeter comment from file
                        int j = 0;
                        while (j++ < 5)
                            if (perimeter.getState().getOriginalLineNumber() - j >= 0 && oriFileCodes.get(perimeter.getState().getOriginalLineNumber() - j).comment != null) {
                                perimeter.comment = oriFileCodes.get(perimeter.getState().getOriginalLineNumber() - j).comment;
                                break;
                            }

                        perimeter.updateBbx();

                        // assign size ids to perimeters (works for outer perimeters only)
                        if(lastPerimeter != null && lastPerimeter.getState().getZ() == perimeter.getState().getZ()) // same layer?
                        {
                            Vector2d v = Heuristics.getXYTravelMove(perimeter.gCodesTravel).getVector2d();
                            v.sub(Heuristics.getXYTravelMove(lastPerimeter.gCodesTravel).getVector2d());
                            double perimeterDistance = v.length();

                            if(currentPerimeterGroup.size() == 0 || perimeterDistance < 1.0/*mm*/) // very short distance? belonging to same perimeter group
                                currentPerimeterGroup.add(perimeter);
                            else {
                                if(currentPerimeterGroup.size() >= 2) {
                                    if (checkInverted(currentPerimeterGroup)) // currentPerimeterGroup.get(0).getBbxArea() < currentPerimeterGroup.get(currentPerimeterGroup.size()-1).getBbxArea())
                                        Collections.reverse(currentPerimeterGroup);

                                    for (int i1 = 0; i1 < currentPerimeterGroup.size(); i1++)
                                        currentPerimeterGroup.get(i1).shellIx = i1 + 1;
                                }

                                currentPerimeterGroup.clear();
                                currentPerimeterGroup.add(perimeter);
                            }
                        }
                        else {
                            if(currentPerimeterGroup.size() >= 2) {
                                if(checkInverted(currentPerimeterGroup)) // currentPerimeterGroup.get(0).getBbxArea() < currentPerimeterGroup.get(currentPerimeterGroup.size()-1).getBbxArea())
                                    Collections.reverse(currentPerimeterGroup);

                                for (int i1 = 0; i1 < currentPerimeterGroup.size(); i1++)
                                    currentPerimeterGroup.get(i1).shellIx = i1+1;
                            }

                            currentPerimeterGroup.clear();
                            currentPerimeterGroup.add(perimeter);
                        }

                        gCodes.add(perimeter);

                        perimeterPointLookup.clear();
                        gCodesTmp.clear();

                        lastPerimeter = perimeter;
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

    private boolean checkInverted(List<GCodePerimeter> perimeterGroup) {

        List<GCode> peri0 = perimeterGroup.get(0).gCodesLoop;
        List<GCode> peri1 = perimeterGroup.get(1).gCodesLoop;
        Vector3d v1 = new Vector3d( peri0.get(0).getState().getX(), peri0.get(0).getState().getY(), 0);
        Vector3d v2 = new Vector3d( peri0.get(0).getState().getX(), peri0.get(0).getState().getY(), 0);

        v1.sub(new Vector3d(peri1.get(0).getState().getX(), peri1.get(0).getState().getY(), 0));
        v2.sub(new Vector3d(peri0.get(1).getState().getX(), peri0.get(1).getState().getY(), 0));
        v1.cross(v1, v2);

        return v1.y > 0;
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
