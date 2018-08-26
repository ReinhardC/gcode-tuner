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
    public static double getExtrudeFactor(List<GCode> gCodesMoves) {
        double rate = 0.0;
        for (int i = 1; i< gCodesMoves.size(); i++) {
            GCode code_last = gCodesMoves.get(i-1);
            GCode code = gCodesMoves.get(i);

            if(!(code instanceof GCodeCommand && code_last instanceof GCodeCommand))
                continue;

            GCodeCommand cmd = (GCodeCommand) code;
            GCodeCommand cmd_last = (GCodeCommand) code_last;

            if(cmd.isPosition() && cmd_last.isPosition() && cmd.comment == null && cmd.has('E') && cmd_last.has('E')) {
                Vector2d v = new  Vector2d(cmd.get('X') - cmd_last.get('X'), cmd.get('Y') - cmd_last.get('Y'));
                return (cmd.get('E') - cmd_last.get('E')) / v.length(); // extrusion to mm
            }
        }
        return 0.0;
    }
}
