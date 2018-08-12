package main.com.specularity.printing.GCodes;

import javafx.util.Pair;

import java.util.regex.Pattern;

public class GCodeFactory {

    private static Pattern lineSeparator = Pattern.compile("[ \\t]+");

    public static GCode produceFromString(String line) {

        int commentPos = line.indexOf(';');

        if(commentPos == 0)
            return new GCodeComment(line);
        else {
            String comment = "";
            if(commentPos >= 0)
                comment = line.substring(commentPos);
            else
                commentPos = line.length();

            String command = line.substring(0, commentPos);

            String[] tokens = lineSeparator.split(command);

            Pair<Character, Double> cmdPair = getParamValuePair(tokens[0]);
            if(cmdPair != null) {
                GCodeCommand cmd = new GCodeCommand(cmdPair.getKey(), cmdPair.getValue().intValue());
                for(int i=1; i<tokens.length; i++) {
                    String token = tokens[i];
                    Pair<Character, Double> parameter = getParamValuePair(token);
                    if(parameter == null)
                        break;
                    cmd.params.put(parameter.getKey(), parameter.getValue());
                }

                cmd.comment = comment;
                return cmd;
            }
            else return null;
        }
    }

    private static Pair<Character, Double> getParamValuePair(String token) {
        if(!token.matches("[A-Z](-?)[0-9.]+"))
            return null;

        Character param = token.charAt(0);
        Double value = Double.valueOf(token.substring(1));

        return new Pair<>(param, value);
    }
}
