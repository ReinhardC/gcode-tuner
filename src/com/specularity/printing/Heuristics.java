package com.specularity.printing;

import com.specularity.printing.GCodes.GCode;
import com.specularity.printing.GCodes.GCodeCommand;

import javax.vecmath.Vector2d;
import java.util.List;

/**
 * helpers that "should" work....
 */
public class Heuristics {
    /* getExtrudeFactor
     * @return extrusion length in mm/traveled mm
     */
    public static double getExtrudeFactor(List<GCode> gCodes) {
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
        return gCodes.get(0).getState().getToolheadPosition().z;
    }

    public static GCodeCommand getLastXYTravelMove(List<GCode> gCodesTravel) {

        Object[] xyOnlyTravelMoves = gCodesTravel.stream().filter(gCode1 -> gCode1 instanceof GCodeCommand && ((GCodeCommand) gCode1).has('X') && !((GCodeCommand) gCode1).has('Z')).toArray();

        if(xyOnlyTravelMoves.length == 0)
            System.out.println("Heuristics assertion failed, no travel move found");

        return xyOnlyTravelMoves.length == 0 ? null : ((GCodeCommand)xyOnlyTravelMoves[xyOnlyTravelMoves.length-1]);
    }

    public static GCodeCommand getLastZTravelMove(List<GCode> gCodesTravel) {

        Object[] zOnlyTravelMoves = gCodesTravel.stream().filter(gCode1 -> gCode1 instanceof GCodeCommand && ((GCodeCommand) gCode1).has('Z') && !((GCodeCommand) gCode1).has('X')).toArray();

        if(zOnlyTravelMoves.length == 0)
            System.out.println("Heuristics assertion failed, no Z travel move found");

        return zOnlyTravelMoves.length == 0 ? null : ((GCodeCommand)zOnlyTravelMoves[zOnlyTravelMoves.length-1]);
    }

    public static double getLayerHeight(List<GCode> gCodesTravel) {
        Object[] zOnlyTravelMoves = gCodesTravel.stream().filter(gCode1 -> gCode1 instanceof GCodeCommand && ((GCodeCommand) gCode1).has('Z') && !((GCodeCommand) gCode1).has('X')).toArray();

        if(zOnlyTravelMoves.length == 0)
            System.out.println("Heuristics assertion failed, no Z travel move found");

        // use last Z
        return zOnlyTravelMoves.length == 0 ? 0.0 : ((GCodeCommand)zOnlyTravelMoves[zOnlyTravelMoves.length-1]).get('Z');
    }
}
