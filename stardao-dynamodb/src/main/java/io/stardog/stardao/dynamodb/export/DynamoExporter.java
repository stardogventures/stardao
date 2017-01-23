package io.stardog.stardao.dynamodb.export;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class DynamoExporter {
    public void export(Table table, File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        export(table, writer);
        writer.flush();
        writer.close();
    }

    public void export(Table table, Writer writer) throws IOException {
        for (Item item : table.scan()) {
            writer.write(item.toJSON());
            writer.write('\n');
        }
    }
}
