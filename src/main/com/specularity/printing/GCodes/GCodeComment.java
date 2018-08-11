package main.com.specularity.printing.GCodes;

class GCodeComment extends GCode {
    private String comment;
    GCodeComment(String comment) {
        this.comment = comment;
    }
}
