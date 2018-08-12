package main.com.specularity.printing.GCodes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class GCodeGroup extends GCode {
    public enum Type { FILE, TMP, LOOP;}
    public final Type type;
    public List<GCode> gCodes = new ArrayList<>();
    @Override
    public String toString() {
        return "type=" + type + ", nb gCodes=" + gCodes.size();
    }

    public GCodeGroup(Type type) {
        this.type = type;
    }

    @Override
    public void serialize(PrintWriter file) throws IOException {
        for (int i = 0; i < gCodes.size(); i++)
            gCodes.get(i).serialize(file);
    }
}
