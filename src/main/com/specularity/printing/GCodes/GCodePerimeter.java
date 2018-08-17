package main.com.specularity.printing.GCodes;

import javax.vecmath.Vector2d;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class GCodePerimeter extends GCode {
    public List<GCode> gCodes = new ArrayList<>();
    private double bbxMinX, bbxMaxX, bbxMinY, bbxMaxY;

    @Override
    public String toString() {
        return "nb gCodes=" + gCodes.size() + (comment != null ? comment : "");
    }

    @Override
    public void serialize(PrintWriter file) throws IOException {
        for (GCode gCode : gCodes) gCode.serialize(file);
    }

    public void updateBbx() {
        bbxMinX = 10000;
        bbxMaxX = -10000;
        bbxMinY = 10000;
        bbxMaxY = -10000;

        for (GCode gCode : gCodes){
            if(gCode instanceof GCodeCommand) {
                GCodeCommand cmd = (GCodeCommand) gCode;

                if(!(cmd.has('X') && cmd.has('Y')))
                    continue;

                double x = cmd.get('X');
                double y = cmd.get('Y');
                if(x < bbxMinX) bbxMinX = x;
                if(x > bbxMaxX) bbxMaxX = x;
                if(y < bbxMinY) bbxMinY = y;
                if(y > bbxMaxY) bbxMaxY = y;
            }
        }
    }

    public double getBbxArea() {
        return Math.sqrt((bbxMaxX-bbxMinX)*(bbxMaxX-bbxMinX) + (bbxMaxY-bbxMinY)*(bbxMaxY-bbxMinY));
    }

    /**
     * getExtrusionRate
     * @return extrusion rate in mm/traveled mm
     */
    public double getExtrusionRate() {
        double rate = 0.0;
        for (int i=1; i<gCodes.size(); i++) {
            GCode code_last = gCodes.get(i-1);
            GCode code = gCodes.get(i);

            if(!(code instanceof GCodeCommand && code_last instanceof GCodeCommand))
                continue;

            GCodeCommand cmd = (GCodeCommand) code;
            GCodeCommand cmd_last = (GCodeCommand) code_last;

            if(cmd.isPosition() && cmd_last.isPosition() && cmd.comment == null && cmd.has('E') && cmd_last.has('E')) {
                Vector2d v = new  Vector2d(cmd.get('X') - cmd_last.get('X'), cmd.get('Y') - cmd_last.get('Y'));
                return (cmd.get('E')*10 - cmd_last.get('E')*10) / v.length(); // extrusion to mm
            }
        }
        return 0.0;
    }

}
