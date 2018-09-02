package com.specularity.printing.GCodes;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.specularity.printing.tuner.log;
import static com.specularity.printing.tuner.logArea;

public class GCodeFactory {

    private static Pattern lineSeparator = Pattern.compile("(?=\\b[A-Z])"); // Pattern.compile("[ \\t]+");

    private static Pattern nextTokenPattern = Pattern.compile("[A-Z]-?[0-9.]+");

    public static List<GCode> gCodesFromLine(String line, String defaultMoveCmd) {
        List<GCode> newCommands = new ArrayList<>();

        int commentPos = line.indexOf(";");
        if(commentPos == -1)
            commentPos = line.indexOf("(");

        if(commentPos == 0)
            newCommands.add(new GCodeComment(line));
        else {
            String lineComment = null;
            if(commentPos != -1)
                lineComment = line.substring(commentPos);
            else
                commentPos = line.length();

            String command = line.substring(0, commentPos).replaceAll("[ \\t]+", "");

            Matcher matcher = nextTokenPattern.matcher(command);

            Pair<Character, Double> pair = getNextParamValuePair(matcher);
            if(pair == null) {
                newCommands.add(new GCodeComment(line));
                return newCommands;
            }

            if(pair.getKey().equals('N'))
                pair = getNextParamValuePair(matcher); // ignore line numbers

            GCodeCommand newCommand = null;

            while(pair != null) {
                Character k = pair.getKey();
                if("GTM".indexOf(k) != -1) {
                    newCommand = new GCodeCommand(k + "" + pair.getValue().intValue(), lineComment);
                    newCommands.add(newCommand);
                }
                else if(newCommand != null) {
                    newCommand.put(k, pair.getValue());
                }
                else if("XYZ".indexOf(k) != -1) { // this must stay behind "if(newCommand != null)"!
                    newCommand = new GCodeCommand(defaultMoveCmd, lineComment);
                    newCommands.add(newCommand);
                    newCommand.put(k, pair.getValue());
                }
                else {
                    log("Unrecognized command found, not collecting parameters: " + k + pair.getValue().intValue());
                    newCommands.add(new GCodeComment(k + "" + pair.getValue().intValue() + " ; unrecognized"));
                }

                pair = getNextParamValuePair(matcher);
            }
        }
        return newCommands;
    }

    private static Pair<Character, Double> getNextParamValuePair(Matcher m) {
        if(!m.find())
            return null;
        String nextToken = m.group();
        return getParamValuePair(nextToken);
    }

    private static Pair<Character, Double> getParamValuePair(String token) {
        if(!token.matches("[A-Z](-?)[0-9.]+"))
            return null;

        Character param = token.charAt(0);
        Double value = Double.valueOf(token.substring(1));

        return new Pair<>(param, value);
    }
}
