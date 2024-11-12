import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Function;

public class Prettifier {

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && args[0].equals("-h")) {
            displayUsage();
            return;
        }
        if (args.length != 3) {
            displayUsage();
            return;
        }

        String input = args[0]; // input.txt
        String output = args[1];    // output.txt
        String airportLookupFile = args[2]; // airport-lookup.csv

        // input and lookup file checking
        if (!Files.exists(Paths.get(input))) {
            System.out.println("Input not found");
            return;
        }
        if (!Files.exists(Paths.get(airportLookupFile))) {
            System.out.println("Airport lookup not found");
            return;
        }
        // airport codes and details into map of maps
        Map<String, Map<String, String>> airportLookup;
        try {
            airportLookup = loadAirportLookup(airportLookupFile);
        } catch (IllegalArgumentException e) {
            System.out.println("Airport lookup malformed");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(input));
             BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {

            StringBuilder contentBuilder = new StringBuilder();
            String line;
            // read all lines from the input file into a single string
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }

            String content = contentBuilder.toString();
            content = replaceSpecialCharacters(content);
            content = normalizeWhiteSpace(content);

            // process each line individually
            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String currentline = lines[i];
                String processedLine = processLine(currentline, airportLookup);
                writer.write(processedLine);
                writer.newLine();
            }
        }
    }
    // displays usage instructions
    private static void displayUsage() {
        System.out.println("Usage:");
        System.out.println("$ java Prettifier.java ./input.txt ./output.txt ./airport-lookup.csv");
    }

    // reads airport lookup data from a CSV file into a nested map
    private static Map<String, Map<String, String>> loadAirportLookup(String airportLookupFile) throws IOException {
        Map<String, Map<String, String>> lookup = new HashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(airportLookupFile));

        // checks if first line contains necessary data 
        if (lines.isEmpty() || !lines.get(0).contains("iata_code") || !lines.get(0).contains("icao_code")) {
            throw new IllegalArgumentException("Airport lookup malformed");
        }

        String[] headers = lines.get(0).split(",");
        int iataIndex = -1, icaoIndex = -1, nameIndex = -1, cityIndex = -1;

        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equals("iata_code")) iataIndex = i;
            if (headers[i].equals("icao_code")) icaoIndex = i;
            if (headers[i].equals("name")) nameIndex = i;
            if (headers[i].equals("municipality")) cityIndex = i;
        }

        if (iataIndex == -1 || icaoIndex == -1 || nameIndex == -1 || cityIndex == -1) {
            throw new IllegalArgumentException("Airport lookup malformed");
        }

        // read data rows
        for (int i = 1; i < lines.size(); i++) {
            String[] data = lines.get(i).split(",");
            if (data.length < headers.length || data[iataIndex].isBlank() || data[icaoIndex].isBlank()
                    || data[nameIndex].isBlank() || data[cityIndex].isBlank()) {
                throw new IllegalArgumentException("Airport lookup malformed");
            }

            String name = data[nameIndex].trim();
            String city = data[cityIndex].trim();
            String iataCode = data[iataIndex].trim();
            String icaoCode = data[icaoIndex].trim();

            lookup.put(iataCode, Map.of("name", name, "city", city));
            lookup.put(icaoCode, Map.of("name", name, "city", city));
        }

        return lookup;
    }

    // processes each line to replace codes and formats
    private static String processLine(String line, Map<String, Map<String, String>> airportLookup) {
        // processing 4 character *##ICAO codes first to avoid partial matches
        line = replaceCode(line, airportLookup, "\\*##(\\w{4})", "city");
        line = replaceCode(line, airportLookup, "\\*#(\\w{3})", "city");

        // processing 4 character ##ICAO codes first to avoid partial matches
        line = replaceCode(line, airportLookup, "##(\\w{4})", "name");
        line = replaceCode(line, airportLookup, "#(\\w{3})", "name");
       
        line = replaceDate(line, "D\\((.+?)\\)", "dd MMM yyyy");
        line = replaceTime(line, "T12\\((.+?)\\)", "hh:mma (XXX)", false);
        line = replaceTime(line, "T24\\((.+?)\\)", "HH:mm (XXX)", true);

        return line;
    }

    // replaces airport codes based on the pattern and key
    private static String replaceCode(String line, Map<String, Map<String, String>> lookup, String pattern, String key) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(line);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String code = m.group(1);
            String replacement = lookup.getOrDefault(code, Map.of()).getOrDefault(key, m.group(0));
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // replaces date patterns with formatted date 
    private static String replaceDate(String line, String pattern, String format) {
        return replaceWithFormattedDateTime(line, pattern, date -> formatTime(date, format, false));
    }

    // replaces time patterns with a formatted time string, depending on hour format
    private static String replaceTime(String line, String pattern, String format, boolean is24Hour) {
        return replaceWithFormattedDateTime(line, pattern, time -> formatTime(time, format, is24Hour));
    }

    // method for patterns replacing
    private static String replaceWithFormattedDateTime(String line, String pattern, Function<String, String> formatter) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(line);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String dateOrTime = m.group(1);
            String formatted = formatter.apply(dateOrTime);
            m.appendReplacement(sb, formatted);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // parses and formats time
    private static String formatTime(String time, String format, boolean is24Hour) {
        try {
            OffsetDateTime dateTime = OffsetDateTime.parse(time, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String formattedTime = dateTime.format(DateTimeFormatter.ofPattern(format));
            return formattedTime.replace("Z", "+00:00");
        } catch (Exception e) {
            return time;
        }
    }

    // handles line-break characters
    private static String replaceSpecialCharacters(String content) {
        return content.replace("\\v", "\n")
                      .replace("\\f", "\n")
                      .replace("\\r", "\n");
    }

    // handles blank lines
    private static String normalizeWhiteSpace(String content) {
        return content.replaceAll("(\n{2,})", "\n\n");
    }
}
