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
            String comment = null;
            if(commentPos >= 0)
                comment = line.substring(commentPos);
            else
                commentPos = line.length();

            String command = line.substring(0, commentPos);

            String[] tokens = lineSeparator.split(command);

            Pair<Character, Double> cmdPair = getParamValuePair(tokens[0]);
            if(cmdPair != null) {
                GCodeCommand command2;
                command2 = new GCodeCommand(cmdPair.getKey() + "" + cmdPair.getValue().intValue(), comment);
                for(int i=1; i<tokens.length; i++) {
                    String token = tokens[i];
                    Pair<Character, Double> parameter = getParamValuePair(token);
                    if(parameter == null)
                        break;
                    command2.put(parameter.getKey(), parameter.getValue());
                }
                return command2;
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
