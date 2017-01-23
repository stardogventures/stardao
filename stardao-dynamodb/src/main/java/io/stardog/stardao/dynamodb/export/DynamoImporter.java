package io.stardog.stardao.dynamodb.export;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamoImporter {
    public void importFromFile(Table table, File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        for (String line; (line = reader.readLine()) != null; ) {
            Item item = Item.fromJSON(line);
            table.putItem(item);
        }
    }

    public void importFromFileWithRelativeDates(Table table, File file, ZoneId timezone) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        for (String line; (line = reader.readLine()) != null; ) {
            String replaced = replaceLine(line, Instant.now(), timezone);
            Item item = Item.fromJSON(replaced);
            table.putItem(item);
        }
    }

    public String replaceLine(String line, Instant now, ZoneId timezone) {
        line = replaceLineDate(line, now, timezone);
        line = replaceLineTimestamp(line, now);
        return line;
    }

    public String replaceLineDate(String line, Instant now, ZoneId timezone) {
        Pattern pattern = Pattern.compile("\\{\\{\\$DATE:([^\\}]+)\\}\\}");
        Matcher matcher = pattern.matcher(line);
        StringBuffer sb = new StringBuffer(line.length());
        while (matcher.find()) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(matcher.group(1)).withZone(timezone);
            matcher.appendReplacement(sb, dtf.format(now));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public String replaceLineTimestamp(String line, Instant now) {
        Pattern pattern = Pattern.compile("\\{\\{\\$TIMESTAMP\\}\\}");
        Matcher matcher = pattern.matcher(line);
        StringBuffer sb = new StringBuffer(line.length());
        while (matcher.find()) {
            matcher.appendReplacement(sb, Long.toString(now.toEpochMilli()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
