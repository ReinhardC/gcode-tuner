package com.specularity.printing;

import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

public class MachineState {
    @Override
    public String toString() {
        return "MachineState{" +
                "#" + originalLineNumber +
                ", p=" + toolheadPosition +
                ", fr=" + feedrate +
                '}';
    }

    private boolean isValidXY;
    private boolean isValidZ;
    private boolean isValidFeedrate;
    private Vector3d toolheadPosition;
    private double feedrate;
    private int originalLineNumber;

    public MachineState(MachineState state) {
        isValidXY = state.isValidXY;
        isValidZ = state.isValidZ;
        isValidFeedrate = state.isValidFeedrate;
        toolheadPosition = new Vector3d(state.toolheadPosition);
        feedrate = state.feedrate;
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

    public double getX() {
        return toolheadPosition.x;
    }

    public double getY() {
        return toolheadPosition.y;
    }

    public double getZ() {
        return toolheadPosition.z;
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

    public void updateFeedrate(double f) {
        isValidFeedrate = true;
        feedrate = f;
    }

    public boolean isValidXY() {
        return isValidXY;
    }

    public boolean isValidZ() {
        return isValidZ;
    }

    public boolean isValidFeedrate() {
        return isValidFeedrate;
    }

    public void setValidFeedrate(boolean validFeedrate) {
        isValidFeedrate = validFeedrate;
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

    public double getFeedrate() {
        return feedrate;
    }

    public void setFeedrate(double feedrate) {
        this.feedrate = feedrate;
    }
}
