package com.specularity.printing.GCodes;

import com.specularity.printing.MachineState;

import java.io.IOException;
import java.io.PrintWriter;

public abstract class GCode {
    public String comment;

    MachineState state;

    public abstract void serialize(PrintWriter file) throws IOException;

    public MachineState getState() {
        return state;
    }

    public void setState(MachineState state) {
        this.state = state;
    }

    public void addCommentTag(String tag) {
        if (comment == null)
            comment = "; +" + tag;
        else
            comment += " +" + tag;
    }
}
