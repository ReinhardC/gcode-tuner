package com.specularity.printing.GCodes;

import javax.vecmath.Vector2d;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GCodeCommand extends GCode {

    private static DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
    private static DecimalFormat d0 = new DecimalFormat("#0", symbols );
    private static DecimalFormat d3 = new DecimalFormat("#0.000", symbols );
    private static DecimalFormat d5 = new DecimalFormat("#0.0000", symbols );

    private Map<Character, Double> params = new HashMap<>();
    public String command;

    public GCodeCommand(String command, String comment) {
        this.command = command;
        this.comment = comment;
    }

    public double get(char c) {
        return params.get(c);
    }

    public void put(char c, double v) {
        params.put(c, v);
    }

    public boolean has(char c) {
        return params.containsKey(c);
    }

    public void putVector2d(Vector2d v) {
        params.put('X', v.x);
        params.put('Y', v.y);
    }

    @Override
    public String toString() {
        String toString = command + " " +
                (params.containsKey('X') ? "X" + d3.format(params.get('X')) + " " : "") +
                (params.containsKey('Y') ? "Y" + d3.format(params.get('Y')) + " " : "") +
                (params.containsKey('Z') ? "Z" + d3.format(params.get('Z')) + " " : "") +
                (params.containsKey('E') ? "E" + d5.format(params.get('E')) + " " : "") +
                (params.containsKey('F') ? "F" + d0.format(params.get('F')) + " " : "") +
                (params.containsKey('P') ? "P" + d0.format(params.get('P')) + " " : "") +
                (params.containsKey('S') ? "S" + d0.format(params.get('S')) + " " : "") +
                (params.containsKey('T') ? "T" + d0.format(params.get('T')) + " " : "");

        while(toString.charAt(toString.length()-1) == ' ')
            toString = toString.substring(0, toString.length()-1);

        if(comment != null && !comment.equals(""))
            toString += " " + comment;

        if(toString.equals("G92 E0.0000"))
            toString = "G92 E0";

        return toString;
    }

    @Override
    public void serialize(PrintWriter file) { file.write(toString() + "\r\n"); }

    public boolean isPosition() {
        return command.equals("G1") && has('X') && has('Y');
    }
}

