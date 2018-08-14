package main.com.specularity.printing.GCodes;

import java.io.IOException;
import java.io.PrintWriter;

public abstract class GCode {
    public String comment;
    public int originalLineNumber;

    public abstract void serialize(PrintWriter file) throws IOException;
}
