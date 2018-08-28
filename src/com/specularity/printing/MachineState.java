package com.specularity.printing;

import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

public class MachineState {
    private boolean isValidXY, isValidZ;
    private Vector3d toolheadPosition;
    private int originalLineNumber;

    public MachineState(MachineState state) {
        isValidXY = state.isValidXY;
        isValidZ = state.isValidZ;
        toolheadPosition = new Vector3d(state.toolheadPosition);
        originalLineNumber = state.originalLineNumber;
    }

    public MachineState() {
        isValidXY = false;
        isValidZ = false;
        toolheadPosition = new Vector3d();
        originalLineNumber = -1;
    }

    public Vector2d getXY() {
        return new Vector2d(toolheadPosition.x, toolheadPosition.y);
    }

    public void updateX(double x) {
        isValidXY = true;
        toolheadPosition.x = x;
    }

    public void updateY(double y) {
        isValidXY = true;
        toolheadPosition.y = y;
    }

    public void updateZ(double z) {
        isValidZ = true;
        toolheadPosition.z = z;
    }

    public boolean isValidXY() {
        return isValidXY;
    }

    public boolean isValidZ() {
        return isValidZ;
    }

    public Vector3d getToolheadPosition() {
        return toolheadPosition;
    }

    public int getOriginalLineNumber() {
        return originalLineNumber;
    }

    public void setOriginalLineNumber(int originalLineNumber) {
        this.originalLineNumber = originalLineNumber;
    }
}
