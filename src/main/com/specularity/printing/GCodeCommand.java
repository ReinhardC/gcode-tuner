package main.com.specularity.printing;

import javafx.util.Pair;

import java.util.HashMap;
import java.util.Map;

class GCodeCommand extends GCode {
    Map<Character, Double> params = new HashMap<>();
    private Character code;
    private Integer num;

    GCodeCommand(String line, Character code, Integer num) {
        super(line);
        this.code = code;
        this.num = num;
    }
}
