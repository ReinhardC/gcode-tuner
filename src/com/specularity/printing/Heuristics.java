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
}
