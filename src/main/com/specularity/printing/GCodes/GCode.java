package main.com.specularity.printing.GCodes;

import java.io.IOException;
import java.io.PrintWriter;

public abstract class GCode {
    public int originalLineNumber = 0;

    public abstract void serialize(PrintWriter file) throws IOException;
}
