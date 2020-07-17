package me.travja.mongo;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.travja.utils.utils.IOUtils;
import org.bson.Document;

import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.regex;

public class Mongo {

    public static void main(String[] args) {
        new MongoTester().init();
    }

}
