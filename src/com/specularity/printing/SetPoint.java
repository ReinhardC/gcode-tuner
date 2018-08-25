package com.specularity.printing;

public class SetPoint {
    private Double offset;
    private Double extrusionPct;
    private Double angle;

    public Double getAngle() {
        return angle;
    }

    public void setAngle(Double angle) {
        this.angle = angle;
    }

    public Double getOffset() {
        return offset;
    }

    public void setOffset(Double offset) {
        this.offset = offset;
    }

    public Double getExtrusionPct() {
        return extrusionPct;
    }

    public void setExtrusionPct(Double extrusionPct) {
        this.extrusionPct = extrusionPct;
    }

    SetPoint(Double offset, Double extrusionPct, double angle) {
        this.offset = offset;
        this.extrusionPct = extrusionPct;
        this.angle = angle;
    }

    @Override
    public String toString() {
        return "SetPoint{" +
                "offset=" + offset +
                ", extrusionPct=" + extrusionPct +
                ", angle=" + angle +
                '}';
    }
}