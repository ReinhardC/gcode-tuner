package com.specularity.printing;

import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import static com.specularity.printing.tuner.preferences;

public class MachineState {
    @Override
    public String toString() {
        return "MachineState{" +
                "#" + originalLineNumber +
                ", p=" + relToolheadPosition +
                ", fr=" + feedrate +
                ", E=" + E +
                "}";
    }


    private boolean hasXY;
    private boolean hasZ;
    private boolean isValidE;
    private boolean hasF;
    private boolean isAbsoluteExtrusion;
    private Vector3d relToolheadPosition;
    private Vector3d zeroPosition;
    private double E, zeroE, maxE;
    private double feedrate;
    private int originalLineNumber;

    public MachineState(MachineState state) {
        hasXY = state.hasXY;
        hasZ = state.hasZ;
        hasF = state.hasF;
        feedrate = state.feedrate;
        E = state.E;
        zeroE = state.zeroE;
        maxE = state.maxE;
        isAbsoluteExtrusion = state.isAbsoluteExtrusion;
        relToolheadPosition = new Vector3d(state.relToolheadPosition);
        zeroPosition = new Vector3d(state.zeroPosition);
        originalLineNumber = state.originalLineNumber;
    }

    public MachineState() {
        hasXY = false;
        hasZ = false;
        hasF = false;
        feedrate = 0;
        E = 0;
        zeroE = 0;
        maxE = -10000;
        isAbsoluteExtrusion = false;
        relToolheadPosition = new Vector3d();
        zeroPosition = new Vector3d();
        originalLineNumber = -1;
    }

    public Vector2d getXY() {
        return new Vector2d(relToolheadPosition.x, relToolheadPosition.y);
    }
    public double getX() {
        return this.zeroPosition.x + relToolheadPosition.x;
    }
    public double getY() {
        return this.zeroPosition.y + relToolheadPosition.y;
    }
    public double getZ() {
        return this.zeroPosition.z + relToolheadPosition.z;
    }
    public double getE() { return this.zeroE + this.E; }
    public double getMaxE() { return this.maxE; }
    
    public void updateX(double x) {
        hasXY = true;
        
        relToolheadPosition.x = x;
    }

    public void updateY(double y) {
        hasXY = true;
        
        relToolheadPosition.y = y;
    }

    public void updateZ(double z) {
        hasZ = true;
        
        relToolheadPosition.z = z;
    }
    
    public void updateE(double e) {
        isValidE = true;
        
        if(isAbsoluteExtrusion)
            this.E = e;
        else
            this.E += e;
        
        if(this.getE() > maxE)
            maxE = this.getE();
    }
    
    public void updateFeedrate(double f) {
        hasF = true;
        feedrate = f;
    }

    public boolean hasXY() {
        return hasXY;
    }

    public boolean hasZ() {
        return hasZ;
    }

    public boolean hasF() {
        return hasF;
    }

    public void setHasF(boolean validFeedrate) {
        hasF = validFeedrate;
    }

    public Vector3d getXYZ() {
        return relToolheadPosition;
    }

    public int getOriginalLineNumber() {
        return originalLineNumber;
    }

    public void setOriginalLineNumber(int originalLineNumber) {
        this.originalLineNumber = originalLineNumber;
    }

    public double getFeedrate() {
        return feedrate;
    }

    public void setFeedrate(double feedrate) {
        this.feedrate = feedrate;
    }

    public boolean isAbsoluteExtrusion() { return this.isAbsoluteExtrusion; }
    
    public void setIsAbsoluteExtrusion(boolean b) {
        this.isAbsoluteExtrusion = b;
    }

    public void setZeroE(double e) {
        this.zeroE = e;
    }
    
    public void setZeroX(double x) {
        this.zeroPosition.x = x;
    }
    
    public void setZeroY(double y) {
        this.zeroPosition.y = y;
    }
    
    public void setZeroZ(double z) {
        this.zeroPosition.z = z;
    }
    
    public boolean checkRetracted() { return this.maxE > this.getE() + preferences.getDouble("extrusionRestartDistance", 0.0); }
}

    
