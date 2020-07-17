package me.travja.mongo;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import me.travja.utils.menu.Menu;
import me.travja.utils.menu.MenuOption;
import me.travja.utils.utils.IOUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;

public class MongoTester {

    private Block<Document> printBlock = document -> System.out.println(document.toJson());

    private MongoClient mongo;
    private MongoDatabase db;
    private MongoCollection<Document> collection;

    private Menu menu = new Menu("What would you like to do?",
            new MenuOption("Search", () -> {
                ArrayList<Bson> docs = new ArrayList<>();
                do {
                    String field = IOUtils.promptForString("Enter the field: ");
                    String value = IOUtils.promptForString("Enter the value to search for: ");
                    docs.add(regex(field, Pattern.compile(value, Pattern.CASE_INSENSITIVE)));
                } while (IOUtils.promptForBoolean("Use another field? (y/n)", "y", "n"));

                FindIterable<Document> col = collection.find(and(docs));
                Iterator iter = col.iterator();
                if (!iter.hasNext())
                    System.out.println("No data found");

                col.forEach(printBlock);
            }),
            new MenuOption("Delete", () -> {
                String field = IOUtils.promptForString("Enter the field to match: ");
                String value = IOUtils.promptForString("Enter the value used to delete (Must be exact match): ");

                DeleteResult result;
                if (IOUtils.promptForBoolean("Delete multiple? (y/n) ", "y", "n"))
                    result = collection.deleteMany(regex(field, Pattern.compile("^" + value + "$", Pattern.CASE_INSENSITIVE)));
                else
                    result = collection.deleteOne(regex(field, Pattern.compile("^" + value + "$", Pattern.CASE_INSENSITIVE)));
                System.out.println(result.getDeletedCount() + " record" + (result.getDeletedCount() == 1 ? "" : "s") + " deleted.");
            }),
            new MenuOption("Get document by id", () -> {
                String input = IOUtils.promptForString("Enter the id: ");
                ObjectId id;
                try {
                    id = new ObjectId(input);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid Object ID");
                    return;
                }

                FindIterable<Document> col = collection.find(Filters.eq("_id", id));
                Iterator iter = col.iterator();
                if (!iter.hasNext())
                    System.out.println("No data found");

                col.forEach(printBlock);
            }),
            new MenuOption("Count objects with a certain field", () -> {
                String fieldCheck = IOUtils.promptForString("What field should we check for? ");

                System.out.println(collection.count(exists(fieldCheck)) + " documents found with field '" + fieldCheck + "'");
            })
    );

    public void init() {
        System.out.println("Attempting to connect to Mongo server on 'localhost'");
        mongo = new MongoClient();

        try {
            mongo.getAddress();
            System.out.println("Connected to Mongo server.");
        } catch (Exception e) {
            System.out.println("Could not connect to Mongo server.");
            mongo.close();
            return;
        }

        db = mongo.getDatabase(IOUtils.promptForString("Enter the database name: "));

        try {
            db.getName();
        } catch (Exception e) {
            System.out.println("Could not connect to database.");
            mongo.close();
            return;
        }

        collection = db.getCollection(IOUtils.promptForString("Enter the collection name: "));

        try {
            System.out.println("Connected to collection. " + collection.count() + " records found.");
        } catch (Exception e) {
            System.out.println("Could not find collection");
            mongo.close();
            return;
        }

        menu.open(true);
    }

}
