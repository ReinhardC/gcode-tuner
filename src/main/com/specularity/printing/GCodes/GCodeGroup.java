package main.com.specularity.printing.GCodes;

import java.util.ArrayList;
import java.util.List;

public class GCodeGroup extends GCode {
    public enum Type { FILE, LOOP }
    public final Type type;
    public List<GCode> gCodes = new ArrayList<>();

    public GCodeGroup(Type type) {
        this.type = type;
    }
}
