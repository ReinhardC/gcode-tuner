package com.specularity.printing.GCodes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class GCodePerimeter extends GCode {
    public List<GCode> gCodesTravel = new ArrayList<>();
    public List<GCode> gCodesLoop = new ArrayList<>();

    public double bbxMinX, bbxMaxX, bbxMinY, bbxMaxY;
    
    @Override
    public String toString() {
        return "nb gCodesLoop=" + gCodesLoop.size() + (comment != null ? comment : "");
    }

    @Override
    public void serialize(PrintWriter file) throws IOException {
        for (GCode gCode : gCodesTravel) gCode.serialize(file);
        for (GCode gCode : gCodesLoop) gCode.serialize(file);
    }

    public void updateBbx() {
        bbxMinX = 10000;
        bbxMaxX = -10000;
        bbxMinY = 10000;
        bbxMaxY = -10000;

        for (GCode gCode : gCodesLoop){
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
}
