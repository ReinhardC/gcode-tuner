package com.specularity.printing;

import com.specularity.printing.GCodes.GCode;
import com.specularity.printing.GCodes.GCodeCommand;
import com.specularity.printing.GCodes.GCodePerimeter;
import javafx.collections.ObservableList;

import javax.vecmath.Vector2d;
import java.util.*;
import java.util.stream.Collectors;

import static com.specularity.printing.tuner.preferences;

public class VectorTools {

    public static List<GCode> tunePerimeter(GCodePerimeter perimeter, ObservableList<SetPoint> setPointsStart, ObservableList<SetPoint> setPointsEnd)
    {
        List<GCode> gCodesInput = perimeter.gCodesLoop;
        ArrayList<GCode> gCodesOutput = new ArrayList<>();

        GCode firstGCode = gCodesInput.get(0);

        GCodeCommand lastXYTravelMove = Heuristics.getLastXYTravelMove(perimeter.gCodesTravel);
        if(setPointsStart.get(0).getZoffset() != 0)
            lastXYTravelMove.put('Z', firstGCode.getState().getToolheadPosition().getZ() + setPointsStart.get(0).getZoffset());

        double perimeterExtrudeFactor = Heuristics.getExtrudeFactor(gCodesInput);

        List<Vector2d> originalMoves = gCodesInput.stream()
                .filter(gCode1 -> gCode1 instanceof GCodeCommand)
                .map(gCode1 -> gCode1.getState().getXY()).collect(Collectors.toList());

        // rotate last to first
        originalMoves.add(0, originalMoves.get(originalMoves.size()-1));
        originalMoves.remove(originalMoves.size()-1); // remove last point which is same as first point

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
            System.out.println(startPaths.get(i) + " (startPath with " + setPointsStart.get(i+1).getExtrusionPct() + ")");
            for (Vector2d move : startPaths.get(i)) {
                GCodeCommand newMove = new GCodeCommand("G1", "; GCodeTuner - replaced start points");
                newMove.setState(new MachineState(firstGCode.getState()));
                newMove.putVector2d(move);
                newMove.put('F', newMove.getState().getFeedrate() * (setPointsStart.get(i+1).getFeedratePct() / 100.));
                if(zModStart)
                    newMove.put('Z', newMove.getState().getToolheadPosition().getZ() + setPointsStart.get(i+1).getZoffset());

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

        System.out.println(mainExtrudedPath + " (mainExtrudedPath with 100)");
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
        // replace end of extruded path
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

            if(endPath.size() > 0)
                System.out.println(endPath + " (endPath with " + setPointsEnd.get(i+1).getExtrusionPct() + ")");

            for (Vector2d move : endPath) {
                GCodeCommand newMove = new GCodeCommand("G1", "; GCodeTuner - replaced end points");
                newMove.setState(new MachineState(firstGCode.getState()));
                newMove.putVector2d(move);
                newMove.put('F', newMove.getState().getFeedrate() * (setPointsEnd.get(i+1).getFeedratePct() / 100.));
                if(zModEnd)
                    newMove.put('Z', newMove.getState().getToolheadPosition().getZ() + setPointsEnd.get(i+1).getZoffset());

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

    public static List<Vector2d> resampleVectorPath(List<Vector2d> input, double from, double to) {

        List<Vector2d> resampledVectorPath = new ArrayList<>();

        // 1. go to "from"

        int currentIx = 0;
        int nextIx = tmod(currentIx+1, input.size());

        double remaining = from;
        if(remaining < 0)
            remaining += getCycleLength(input);
        else if(remaining >= getCycleLength(input))
            remaining -= getCycleLength(input);

        Vector2d v = new Vector2d(input.get(nextIx));
        Vector2d v2 = new Vector2d(input.get(currentIx));
        v.sub(v2);

        while(remaining - v.length() >= 0.0)
        {
            remaining -= v.length();

            currentIx = nextIx;
            nextIx = tmod(currentIx+1, input.size());

            v.set(input.get(nextIx));
            v.sub(input.get(currentIx));
        }

        v.set(input.get(nextIx));
        v.sub(input.get(currentIx));
        v.scale(remaining / v.length() );

        // 2. add "from"

        v2.set(input.get(currentIx));
        v2.add(v);

        resampledVectorPath.add(new Vector2d(v2));

        // 3. go to "to"

        remaining = to - from;

        v.set(input.get(nextIx));
        v.sub(v2);

        while(remaining - v.length() > 0.0) {
            remaining -= v.length();

            currentIx = nextIx;
            nextIx = tmod(currentIx + 1, input.size());

            v2.set(input.get(currentIx));

            // 4. add points on the path
            resampledVectorPath.add(new Vector2d(v2));

            v.set(input.get(nextIx));
            v.sub(input.get(currentIx));
        }

        v.set(input.get(nextIx));
        v.sub(v2);
        v.scale(remaining / v.length() );

        // 5. add "to" if not already included
        if(v.length()>0.00000001) {
            v2.add(v);
            resampledVectorPath.add(new Vector2d(v2));
        }

        return resampledVectorPath; // new ArrayList((Collection) output.stream().distinct());
    }

    public static void removeDoublePoints(List<Vector2d> path)
    {
        Iterator<Vector2d> iterator = path.iterator();
        Vector2d last = null;
        while(iterator.hasNext()) {
            Vector2d next = iterator.next();
            if(last != null && next.equals(last)) {
                iterator.remove();
            }
            last = next;
        }
    }

    public static double getCycleLength(List<Vector2d> input)
    {
        double totalLength = getPathLength(input);

        Vector2d tmp = new Vector2d(input.get(0));
        tmp.sub(input.get(input.size()-1));
        totalLength += tmp.length();

        return totalLength;

    }

    public static double getPathLength(List<Vector2d> input)
    {
        double totalLength = 0.0;

        for (int currentIx = 0; currentIx < input.size()-1; currentIx++)
        {
            Vector2d tmp = new Vector2d(input.get(currentIx+1));
            tmp.sub(input.get(currentIx));
            totalLength += tmp.length();
        }

        return totalLength;
    }

    public static void rotatePathAround(List<Vector2d> input, Vector2d around, double angle) {
        for (Vector2d v : input)
            rotatePointAround(v, around, angle);
    }

    public static void rotatePointAround(Vector2d point, Vector2d around, double angle2) {
        double angle = Math.toRadians(angle2);
        double x = point.x - around.x; // move to origin
        double y = point.y - around.y;
        double rotated_x = x * Math.cos(angle) - y * Math.sin(angle);
        double rotated_y = x * Math.sin(angle) + y * Math.cos(angle);
        point.x = rotated_x + around.x;
        point.y = rotated_y + around.y;
    }

    public static double getTriAngle(Vector2d origin, Vector2d p1, Vector2d p2) {
        Vector2d v1 = new Vector2d(p1);
        Vector2d v2 = new Vector2d(p2);
        v1.sub(origin);
        v2.sub(origin);
        return Math.toDegrees(v1.angle(v2));
    }

    public static void round3Vector2d(Vector2d v) {
        v.x = round3(v.x);
        v.y = round3(v.y);
    }

    public static double round3(double value) {
        return Math.round(1000. * value) / 1000.;
    }

    /**
     * "tiled" modulo - instead of -1%5=-1, -2%5=-2, ...
     * @return -1%5=4, -5$5=0
     */
    public static int tmod(int input, int size) { return ((input % size) + size) % size; }
}
