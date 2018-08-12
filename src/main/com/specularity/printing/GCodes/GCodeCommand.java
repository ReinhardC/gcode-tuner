    package main.com.specularity.printing.GCodes;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;

public class GCodeCommand extends GCode {
    public Map<Character, Double> params = new HashMap<>();
    public String comment = "";

    private Character code;
    private Integer num;

    GCodeCommand(Character code, Integer num) {
        this.code = code;
        this.num = num;
    }

    @Override
    public void serialize(PrintWriter file) throws IOException {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');

        DecimalFormat d0 = new DecimalFormat("#0", symbols );
        DecimalFormat d3 = new DecimalFormat("#0.000", symbols );
        DecimalFormat d5 = new DecimalFormat("#0.0000", symbols );

        String out =   code.toString() + num + " " +
                        (params.containsKey('X') ? "X" + d3.format(params.get('X')) + " " : "") +
                        (params.containsKey('Y') ? "Y" + d3.format(params.get('Y')) + " " : "") +
                        (params.containsKey('Z') ? "Z" + d3.format(params.get('Z')) + " " : "") +
                        (params.containsKey('E') ? "E" + d5.format(params.get('E')) + " " : "") +
                        (params.containsKey('F') ? "F" + d0.format(params.get('F')) + " " : "") +
                        (params.containsKey('P') ? "P" + d0.format(params.get('P')) + " " : "") +
                        (params.containsKey('S') ? "S" + d0.format(params.get('S')) + " " : "") +
                        (params.containsKey('T') ? "T" + d0.format(params.get('T')) + " " : "");

        while(out.charAt(out.length()-1) == ' ')
            out = out.substring(0, out.length()-1);

        if(out.equals("G92 E0.0000"))
            out = "G92 E0";

        if(comment.equals(""))
            file.write(out + "\n");
        else
            file.write(out + " " + comment + "\n");
    }
}
