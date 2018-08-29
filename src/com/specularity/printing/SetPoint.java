package com.specularity.printing;

public class SetPoint {

    private Double offset;
    private Double zoffset;
    private Double extrusionPct;
    private Double feedratePct;
    private Double angle;

    public SetPoint(Double offset, Double angle, Double extrusionPct, Double feedratePct, Double zoffset) {
        this.offset = offset;
        this.angle = angle;
        this.extrusionPct = extrusionPct;
        this.feedratePct = feedratePct;
        this.zoffset = zoffset;
    }

    public Double getOffset() {
        return offset;
    }

    public void setOffset(Double offset) {
        this.offset = offset;
    }

    public void setZoffset(Double zoffset) {
        this.zoffset = zoffset;
    }

    public Double getZoffset() {
        return zoffset;
    }

    public Double getExtrusionPct() {
        return extrusionPct;
    }

    public void setExtrusionPct(Double extrusionPct) {
        this.extrusionPct = extrusionPct;
    }

    public Double getAngle() {
        return angle;
    }

    public void setAngle(Double angle) {
        this.angle = angle;
    }

    public void setFeedratePct(Double feedratePct) {
        this.feedratePct = feedratePct;
    }

    public Double getFeedratePct() {
        return feedratePct;
    }

    @Override
    public String toString() {
        return "SetPoint{" +
                "offset=" + offset +
                ", zoffset=" + zoffset +
                ", extrusionPct=" + extrusionPct +
                ", feedratePct=" + feedratePct +
                ", angle=" + angle +
                '}';
    }
}
