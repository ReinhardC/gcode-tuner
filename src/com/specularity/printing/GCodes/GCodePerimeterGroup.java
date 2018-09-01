package com.specularity.printing.GCodes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.specularity.printing.VectorTools.getSignedTravelAngle;

public class GCodePerimeterGroup  extends GCode {
    public List<GCodePerimeter> perimeters = new ArrayList<>();

    public double bbxMinX, bbxMaxX, bbxMinY, bbxMaxY;

    public GCodePerimeterGroup(GCodePerimeter perimeter) {
        perimeters.add(perimeter);
    }

    public GCodePerimeterGroup() {
    }

    @Override
    public void serialize(PrintWriter file) throws IOException {
        for (GCode gCode : perimeters) gCode.serialize(file);
    }
    
    public void updateBbx() {
        bbxMinX = 10000;
        bbxMaxX = -10000;
        bbxMinY = 10000;
        bbxMaxY = -10000;

        for (GCodePerimeter perimeter : perimeters){
            perimeter.updateBbx();
            if(perimeter.bbxMinX < bbxMinX) bbxMinX = perimeter.bbxMinX;
            if(perimeter.bbxMaxX > bbxMaxX) bbxMaxX = perimeter.bbxMaxX;
            if(perimeter.bbxMinY < bbxMinY) bbxMinY = perimeter.bbxMinY;
            if(perimeter.bbxMaxY > bbxMaxY) bbxMaxY = perimeter.bbxMaxY;
        }
    }

    public double getBbxArea() {
        return Math.sqrt((bbxMaxX-bbxMinX)*(bbxMaxX-bbxMinX) + (bbxMaxY-bbxMinY)*(bbxMaxY-bbxMinY));
    }
    
    public boolean isInnerPerimeter() {
        List<GCode> peri0 = perimeters.get(0).gCodesLoop;
        List<GCode> peri1 = perimeters.get(1).gCodesLoop;

        return getSignedTravelAngle(peri0.get(0).getState().getXY(), peri1.get(0).getState().getXY(), peri1.get(1).getState().getXY()) > 0.0;
    }
}
