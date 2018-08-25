package com.specularity.printing.GCodes;

import java.io.PrintWriter;

public class GCodeComment extends GCode {
    public GCodeComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return comment;
    }

    @Override
    public void serialize(PrintWriter file) {
        file.write(toString() + "\r\n");
    }
}
