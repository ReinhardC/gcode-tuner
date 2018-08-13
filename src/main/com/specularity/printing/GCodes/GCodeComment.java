package main.com.specularity.printing.GCodes;

import java.io.IOException;
import java.io.PrintWriter;

public class GCodeComment extends GCode {
    private String comment;
    GCodeComment(String comment) {
        this.comment = comment;
    }

    @Override
    public void serialize(PrintWriter file) throws IOException {
        file.write(comment + "\r\n");
    }
}
