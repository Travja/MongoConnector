package me.travja.mongo;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import static com.mongodb.client.model.Updates.*;

import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import me.travja.utils.menu.Menu;
import me.travja.utils.menu.MenuOption;
import me.travja.utils.utils.IOUtils;
import org.bson.*;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;

public class MongoTester {

    private Block<Document> printBlock = document -> System.out.println(document.toJson());

    private MongoClient mongo;
    private MongoDatabase db;
    private MongoCollection<Document> collection;

    private Menu menu = new Menu("What would you like to do?",
            new MenuOption("Create", () -> {
                String[] fields = {"person.first_name", "person.last_name", "person.gender", "person.nick_name","cell_phone_number", "pet", "address.street","address.city","address.state","address.zip_code","home.email","home.phone_number","work.email","work.phone_number","skill","car.year","car.make","car.model"};
                ArrayList<String> nonNulls = new ArrayList<>(Arrays.asList("person.first_name", "person.last_name", "person.gender", "address.street", "address.city", "address.state", "address.zip_code"));
                Document doc = new Document();
                doc.append("person", new Document().append("first_name", "").append("last_name","").append("nick_name", ""))
                        .append("cell_phone_number", "").append("pet", new String[0])
                        .append("address", new Document().append("street","").append("city","").append("state","").append("zip_code",""))
                        .append("home", new Document().append("email","").append("phone_number",""))
                        .append("work", new Document().append("email","").append("phone_number",""))
                        .append("skill", new String[0])
                        .append("car", new Document().append("year",0).append("make","").append("model",""));

                boolean carError;
                for(String s : fields) {
                    String value;
                    int it = 0;
                    do {
                        carError = false;
                        if(it > 0) {
                            System.out.println("This field may not be nullable or you may have entered incorrect information\nPlease try again");
                        }
                        value = IOUtils.promptForString("Enter a value for the field \"" + s + "\" (enter null to not fill):");
                        it++;
                        if(s.equals("car.year")) {
                            if(value.toLowerCase().equals("null")) {
                                ((Document)doc.get("car")).remove("year");
                            }
                            else {
                                try {
                                    Integer.parseInt(value);
                                    ((Document) doc.get("car")).put("year", Integer.parseInt(value));
                                } catch (NumberFormatException nfe) {
                                    carError = true;
                                    System.out.println("The car.year field must have a valid year entered");
                                }
                            }
                        }
                        else if(s.equals("person.gender")) {
                            value = value.toUpperCase();
                        }
                    } while (carError || (value.toLowerCase().equals("null") && nonNulls.contains(s)) || (s.equals("person.gender") && !(value.equals("M") || value.equals("F"))));
                    if(!value.equals("null") && (s.equals("pet") || s.equals("skill")) ) {
                        ArrayList<String> values = new ArrayList<>();
                        values.add(value);
                        while(IOUtils.promptForBoolean("Add another value for this field? (y/n)", "y", "n")) {
                            values.add(IOUtils.promptForString("Enter a value for the field \"" + s + "\":"));
                        }
                        String[] arr = values.toArray(new String[0]);
                        doc.replace(s, Arrays.asList(arr));
                    }
                    else if(!s.equals("car.year")) {
                        int endIndex = s.contains(".") ? s.indexOf(".") : s.length();
                        if(value.toLowerCase().equals("null")) {
                            if(s.contains(".")) {
                                ((Document) doc.get(s.substring(0, endIndex))).remove(s.substring(s.indexOf(".")+1));
                            }
                            else {
                                doc.remove(s);
                            }
                        }
                        else if(s.contains(".")) {
                            ((Document) doc.get(s.substring(0, endIndex))).replace(s.substring(s.indexOf(".")+1), value);
                        }
                        else {
                            doc.replace(s, value);
                        }
                    }
                }
                collection.insertOne(doc);
            }),
            new MenuOption("Search (Read)", () -> {
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
            new MenuOption("Update", () -> {
                ArrayList<Bson> searchVals = new ArrayList<>();
                ArrayList<Bson> updateVals = new ArrayList<>();

                do {
                    String field = IOUtils.promptForString("Enter the field to search for: ");
                    String value = IOUtils.promptForString("Enter the value to search for: ");
                    searchVals.add(eq(field, value));
                } while (IOUtils.promptForBoolean("Use another field? (y/n)", "y", "n"));

                do {
                    String field = IOUtils.promptForString("Enter the field to update or remove: ");
                    String value = IOUtils.promptForString("Enter the value to update to (type null to remove field): ");
                    if(value.equals("null")) {
                        updateVals.add(unset(field));
                    }
                    else {
                        updateVals.add(set(field, value));
                    }
                } while (IOUtils.promptForBoolean("Update another field? (y/n)", "y", "n"));

                UpdateResult result = collection.updateMany(and(searchVals), and(updateVals));
                System.out.println(result);
            }),
            new MenuOption("Delete", () -> {
                ArrayList<Bson> docs = new ArrayList<>();
                do {
                    String field = IOUtils.promptForString("Enter the field: ");
                    String value = IOUtils.promptForString("Enter the value to search for: ");
                    docs.add(regex(field, Pattern.compile("^" + value + "$", Pattern.CASE_INSENSITIVE)));
                } while (IOUtils.promptForBoolean("Use another field? (y/n)", "y", "n"));

                DeleteResult result;
                if (IOUtils.promptForBoolean("Delete multiple? (y/n) ", "y", "n"))
                    result = collection.deleteMany(and(docs));
                else
                    result = collection.deleteOne(and(docs));
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
