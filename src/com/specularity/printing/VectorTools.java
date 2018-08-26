package com.specularity.printing;

import javax.vecmath.Vector2d;
import java.util.*;

public class VectorTools {

    public static List<Vector2d> resamplePath(List<Vector2d> input, double from, double to) {

        List<Vector2d> output = new ArrayList<>();

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

        output.add(new Vector2d(v2));

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
            output.add(new Vector2d(v2));

            v.set(input.get(nextIx));
            v.sub(input.get(currentIx));
        }

        v.set(input.get(nextIx));
        v.sub(v2);
        v.scale(remaining / v.length() );

        // 5. add "to"

        v2.add(v);
        output.add(new Vector2d(v2));

        return output; // new ArrayList((Collection) output.stream().distinct());
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

    public static void round3Vector2d(Vector2d v) {
        v.x = round3(v.x);
        v.y = round3(v.y);
    }

    public static double round3(double value) {
        return Math.round(10000. * value) / 10000.;
    }

    /**
     * "tiled" modulo - instead of -1%5=-1, -2%5=-2, ...
     * @return -1%5=4, -5$5=0
     */
    public static int tmod(int input, int size) { return ((input % size) + size) % size; }
}
