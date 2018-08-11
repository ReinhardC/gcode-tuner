package main.com.specularity.printing.GCodes;

import java.util.HashMap;
import java.util.Map;

class GCodeCommand extends GCode {
    Map<Character, Double> params = new HashMap<>();
    private Character code;
    private Integer num;

    GCodeCommand(Character code, Integer num) {
        this.code = code;
        this.num = num;
    }
}
