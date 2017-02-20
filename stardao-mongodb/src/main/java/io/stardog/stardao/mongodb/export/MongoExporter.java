package io.stardog.stardao.mongodb.export;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class MongoExporter {
    public void export(MongoCollection<Document> collection, File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        export(collection, writer);
        writer.flush();
        writer.close();
    }

    public void export(MongoCollection<Document> collection, Writer writer) throws IOException {
        for (Document doc : collection.find()) {
            writer.write(doc.toJson());
            writer.write('\n');
        }
    }
}