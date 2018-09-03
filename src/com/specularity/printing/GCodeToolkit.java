package com.specularity.printing;

import com.specularity.printing.GCodes.GCode;
import com.specularity.printing.GCodes.GCodeCommand;
import com.specularity.printing.GCodes.GCodePerimeter;
import javafx.collections.ObservableList;

import javax.vecmath.Vector2d;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.specularity.printing.VectorTools.*;
import static com.specularity.printing.tuner.log;
import static com.specularity.printing.tuner.preferences;

public class GCodeToolkit {

    public boolean testPerimeterWillBeShifted(GCodePerimeter perimeter) {
        List<Vector2d> moves = perimeter.gCodesLoop.stream()
                .filter(gCode1 -> gCode1 instanceof GCodeCommand)
                .map(gCode1 -> gCode1.getState().getXY()).collect(Collectors.toList());

        int minIx = 0;
        double minAngle = 360.;
        for (int i = 0; i < moves.size(); i++) {
            int previ = i != 0 ? i - 1 : moves.size() - 1;
            int nexti = i != moves.size() - 1 ? i + 1 : 0;
            double a = getSignedTravelAngle(moves.get(previ), moves.get(i), moves.get(nexti));
            if (a < minAngle) {
                minAngle = a;
                minIx = i;
            }
        }

        return (-minAngle >= preferences.getDouble("maxAngleBetweenSegments", 25.0));
    }

    public static int findIndexOfClosestPointTo(GCodePerimeter perimeter, Vector2d p) {
        List<Vector2d> moves = perimeter.gCodesLoop.stream()
                .filter(gCode1 -> gCode1 instanceof GCodeCommand)
                .map(gCode1 -> gCode1.getState().getXY()).collect(Collectors.toList());
        
        int minIx = -1;
        double minDist = 100000.;
        for (int i = 0; i < moves.size(); i++) {
            Vector2d v = new Vector2d(moves.get(i));
            v.sub(p);
            double d = v.length();
            if (d < minDist) {
                minDist = d;
                minIx = i;
            }
        }

        return minIx;
    }
     
    public static int findIndexOfMostConcaveAngle(GCodePerimeter perimeter) {
        List<Vector2d> moves = perimeter.gCodesLoop.stream()
                .filter(gCode1 -> gCode1 instanceof GCodeCommand)
                .map(gCode1 -> gCode1.getState().getXY()).collect(Collectors.toList());

        int minIx = -1;
        double minAngle = 360.;
        for (int i = 0; i < moves.size(); i++) {
            int previ = i != 0 ? i - 1 : moves.size() - 1;
            int nexti = i != moves.size() - 1 ? i + 1 : 0;
            double a = getSignedTravelAngle(moves.get(previ), moves.get(i), moves.get(nexti));
            if (a < minAngle) {
                minAngle = a;
                minIx = i;
            }
        }

        if (-minAngle < preferences.getDouble("maxAngleBetweenSegments", 25.0)) { 
            return -1;
        }
        
        return minIx;
    }
    
    public static boolean shiftPerimeter(GCodePerimeter perimeter, int shiftIx) {

        if(shiftIx == -1)
            return false;
        
        List<Vector2d> moves2 = perimeter.gCodesLoop.stream()
                .filter(gCode1 -> gCode1 instanceof GCodeCommand)
                .map(gCode1 -> gCode1.getState().getXY()).collect(Collectors.toList());

        List<Vector2d> moves = new ArrayList<>();
        moves.addAll(moves2.subList(shiftIx+1, moves2.size()));
        moves.addAll(moves2.subList(0, shiftIx+1));

        if (perimeter.gCodesLoop.size() != moves.size()) {
            log("Error: shift could not be applied to a perimeter because there were non-move commands in the loop part.");
            return false;
        }

        GCodeCommand travelMove = GCodeToolkit.getLastXYTravelMove(perimeter.gCodesTravel);
        if(travelMove == null) {
            log("Error: shift could not be applied to a perimeter because there were no travel moves.");
            return false;
        }

        for (int i = 0; i < perimeter.gCodesLoop.size(); i++)
            ((GCodeCommand) perimeter.gCodesLoop.get(i)).putVector2d(moves.get(i));

        travelMove.putVector2d(moves.get(moves.size()-1));
        
        return true;
    }

    public static List<GCode> tunePerimeter(GCodePerimeter perimeter, ObservableList<SetPoint> setPointsStart, ObservableList<SetPoint> setPointsEnd)
    {
        List<GCode> gCodesInput = perimeter.gCodesLoop;
        ArrayList<GCode> gCodesOutput = new ArrayList<>();

        GCode firstGCode = gCodesInput.get(0);

        GCodeCommand lastXYTravelMove = GCodeToolkit.getLastXYTravelMove(perimeter.gCodesTravel);
        if(setPointsStart.get(0).getZoffset() != 0)
            lastXYTravelMove.put('Z', firstGCode.getState().getXYZ().getZ() + setPointsStart.get(0).getZoffset());

        double perimeterExtrudeFactor = GCodeToolkit.getExtrudeFactor(gCodesInput);

        List<Vector2d> originalMoves = gCodesInput.stream()
                .filter(gCode1 -> gCode1 instanceof GCodeCommand)
                .map(gCode1 -> gCode1.getState().getXY()).collect(Collectors.toList());

        // rotate last to first. last was original offset point
        originalMoves.add(0, originalMoves.get(originalMoves.size()-1));
        originalMoves.remove(originalMoves.size()-1);


        double minExtrusionD = preferences.getDouble("minExtrusionDistance", 0.0001);

        Double totalPathAngle = 0.0;
        Vector2d pathOffset = new Vector2d(0.0, 0.0); // used for rotation

        Vector2d previousMove = null;
        double nextExtrusionPoint = 0.0;

        //                                                             
        // replace start of extruded path                              
        //                                                             
        // reversed so the rotation can be applied to the points       
        //                                                             

        List<List<Vector2d>> startPaths = new ArrayList<>(); // used only for reversing

        for (int i = setPointsStart.size() - 2; i >= 0; i--)
        {
            double from = setPointsStart.get(i).getOffset();
            double to = setPointsStart.get(i+1).getOffset();

            List<Vector2d> startPath = resampleVectorPath(originalMoves, from, to);
            for (Vector2d v: startPath)
                v.add(pathOffset);

            totalPathAngle -= setPointsStart.get(i+1).getAngle(); // negate so positive angle is always inward into model

            Vector2d newPathOffset = new Vector2d(startPath.get(0));
            rotatePathAround(startPath, startPath.get(startPath.size() - 1), totalPathAngle);
            newPathOffset.sub(startPath.get(0));
            pathOffset.sub(newPathOffset);

            startPath.forEach(VectorTools::round3Vector2d);
            removeDoublePoints(startPath);
            if(i != 0) {
                startPath.remove(0);
            }

            startPaths.add(0, startPath);
        }

        boolean zModStart = setPointsStart.stream().anyMatch(setPoint -> setPoint.getZoffset() != 0);

        for (int i = 0; i < setPointsStart.size()-1; i++) // add startPaths in forward order
        {
            // System.out.println(startPaths.get(i) + " (startPath with " + setPointsStart.get(i+1).getExtrusionPct() + ")");
            for (Vector2d move : startPaths.get(i)) {
                GCodeCommand newMove = new GCodeCommand("G1", "; GCodeTuner - replaced start points");
                newMove.setState(new MachineState(firstGCode.getState()));
                newMove.putVector2d(move);
                newMove.put('F', newMove.getState().getFeedrate() * (setPointsStart.get(i+1).getFeedratePct() / 100.));
                if(zModStart)
                    newMove.put('Z', newMove.getState().getXYZ().getZ() + setPointsStart.get(i+1).getZoffset());

                if(previousMove != null) {
                    previousMove.sub(move);
                    double extrusionD = previousMove.length() * perimeterExtrudeFactor * (setPointsStart.get(i + 1).getExtrusionPct() / 100.);
                    if(extrusionD < minExtrusionD) {
                        previousMove = move;
                        continue;
                    }
                    nextExtrusionPoint += extrusionD;
                }
                newMove.put('E', nextExtrusionPoint);
                gCodesOutput.add(newMove);
                previousMove = move;
            }
        }

        //                                                                  
        //                       REPLACE MAIN EXTRUSION                     
        //                                                                  

        List<Vector2d> mainExtrudedPath = resampleVectorPath(originalMoves, setPointsStart.get(setPointsStart.size() - 1).getOffset(), getCycleLength(originalMoves) + setPointsEnd.get(0).getOffset());
        if(setPointsStart.size() != 1)
            mainExtrudedPath.remove(0);

        for (int i = 0; i < mainExtrudedPath.size(); i++) {
            Vector2d v = mainExtrudedPath.get(i);
            GCodeCommand newMove = new GCodeCommand("G1", null);
            newMove.setState(new MachineState(firstGCode.getState()));
            newMove.putVector2d(v);
            if(previousMove != null) {
                previousMove.sub(v);
                nextExtrusionPoint += previousMove.length() * perimeterExtrudeFactor;
            }
            newMove.put('E', nextExtrusionPoint);
            gCodesOutput.add(newMove);
            previousMove = v;
        }

        //                                         
        //     replace end of extruded path        
        //                                         

        pathOffset.set(0., 0.);
        totalPathAngle = 0.;

        boolean zModEnd = setPointsEnd.stream().anyMatch(setPoint -> setPoint.getZoffset() != 0);

        for (int i = 0; i < setPointsEnd.size()-1; i++)
        {
            double from = getCycleLength(originalMoves) + setPointsEnd.get(i).getOffset();
            double to =  getCycleLength(originalMoves) + setPointsEnd.get(i+1).getOffset();

            List<Vector2d> endPath = resampleVectorPath(originalMoves, from, to);
            for (Vector2d v: endPath)
                v.add(pathOffset);

            totalPathAngle += setPointsEnd.get(i).getAngle();

            Vector2d newPathOffset = new Vector2d(endPath.get(endPath.size()-1));
            rotatePathAround(endPath, endPath.get(0), totalPathAngle);
            newPathOffset.sub(endPath.get(endPath.size()-1));
            pathOffset.sub(newPathOffset);

            endPath.forEach(VectorTools::round3Vector2d);
            removeDoublePoints(endPath);
            endPath.remove(0);

            for (Vector2d move : endPath) {
                GCodeCommand newMove = new GCodeCommand("G1", "; GCodeTuner - replaced end points");
                newMove.setState(new MachineState(firstGCode.getState()));
                newMove.putVector2d(move);
                newMove.put('F', newMove.getState().getFeedrate() * (setPointsEnd.get(i+1).getFeedratePct() / 100.));
                if(zModEnd)
                    newMove.put('Z', newMove.getState().getXYZ().getZ() + setPointsEnd.get(i+1).getZoffset());

                if(previousMove != null) {
                    previousMove.sub(move);
                    nextExtrusionPoint += previousMove.length() * perimeterExtrudeFactor * (setPointsEnd.get(i+1).getExtrusionPct() / 100);
                }
                newMove.put('E', nextExtrusionPoint);

                gCodesOutput.add(newMove);
                previousMove = move;
            }
        }

        if(setPointsEnd.get(setPointsEnd.size()-1).getZoffset() != 0) {
            GCodeCommand cmd = new GCodeCommand("G1", "; back to old Z");
            cmd.put('Z', firstGCode.getState().getZ());
            gCodesOutput.add(cmd);
        }

        return gCodesOutput;
    }

    /* getExtrudeFactor
     * @return extrusion length in mm/traveled mm
     */
    private static double getExtrudeFactor(List<GCode> gCodes) {
        double rate = 0.0;
        for (int i = 1; i< gCodes.size(); i++) {
            GCode code_last = gCodes.get(i-1);
            GCode code = gCodes.get(i);

            if(!(code instanceof GCodeCommand && code_last instanceof GCodeCommand))
                continue;

            GCodeCommand cmd = (GCodeCommand) code;
            GCodeCommand cmd_last = (GCodeCommand) code_last;

            if(cmd.has('E') && cmd_last.has('E')) {
                Vector2d v = cmd.getState().getXY();
                v.sub(cmd_last.getState().getXY());
                return (cmd.get('E') - cmd_last.get('E')) / v.length(); // extrusion to mm
            }
        }
        return 0.0;
    }

    public static double getZHeight(List<GCode> gCodes) {
        return gCodes.get(0).getState().getXYZ().z;
    }

    static GCodeCommand getLastXYTravelMove(List<GCode> gCodesTravel) {

        Object[] xyTravelMoves = gCodesTravel.stream().filter(gCode1 -> gCode1 instanceof GCodeCommand && ((GCodeCommand) gCode1).has('X') && ((GCodeCommand) gCode1).has('Y') && !((GCodeCommand) gCode1).has('Z')).toArray();

        return xyTravelMoves.length == 0 ? null : ((GCodeCommand)xyTravelMoves[xyTravelMoves.length-1]);
    }

    static GCodeCommand getOnlyZTravelMove(List<GCode> gCodesTravel) {

        Object[] zTravelMoves = gCodesTravel.stream().filter(gCode1 -> gCode1 instanceof GCodeCommand && ((GCodeCommand) gCode1).has('Z') && !((GCodeCommand) gCode1).has('X') && !((GCodeCommand) gCode1).has('Y')).toArray();

        if( zTravelMoves.length != 1 )
            log("Error: Mutiple Z changes in travel moves found. Do expect unexpected behavior.");
        
        return zTravelMoves.length != 1 ? null : ((GCodeCommand)zTravelMoves[zTravelMoves.length-1]);
    }

    public static double getLayerHeight(List<GCode> gCodesTravel) {
        Object[] zOnlyTravelMoves = gCodesTravel.stream().filter(gCode1 -> gCode1 instanceof GCodeCommand && ((GCodeCommand) gCode1).has('Z') && !((GCodeCommand) gCode1).has('X') && !((GCodeCommand) gCode1).has('Y')).toArray();

        // use last Z
        return zOnlyTravelMoves.length == 0 ? 0.0 : ((GCodeCommand)zOnlyTravelMoves[zOnlyTravelMoves.length-1]).get('Z');
    }
}
