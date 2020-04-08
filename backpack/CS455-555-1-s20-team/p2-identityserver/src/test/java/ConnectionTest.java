import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import mongo.UserDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

public class ConnectionTest {
    private static Properties properties;
    private String mongoUri;
    private UserDao dao;
    private MongoClient mongoClient;
    private String databaseName;

    public ConnectionTest() throws IOException{
        mongoUri = getProperty("mongodb.uri");
        databaseName = getProperty("mongodb.database");
        this.mongoClient=mongoClient(mongoUri);
    }

    public static void init() throws IOException {
        properties = new Properties();
        properties.load(ClassLoader.getSystemResourceAsStream("application.properties"));
    }

    public static String getProperty(String propertyKey) throws IOException{
        if (properties==null) {
            ConnectionTest.init();
        }
        return properties.getProperty(propertyKey);
    }

    public MongoClient mongoClient(String connectionString){
        ConnectionString connString = new ConnectionString(connectionString);

        MongoClient mongoClient = MongoClients.create(connString);
        return mongoClient;
    }

    @Before
    public void setup() throws IOException {
        this.dao = new UserDao(mongoClient, databaseName);
    }

    @Test
    public void testConnectionFindsDatabase() {

        MongoClient mc = MongoClients.create(mongoUri);
        boolean found = false;
        for (String dbname : mc.listDatabaseNames()) {
            if (databaseName.equals(dbname)) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(
                "We can connect to MongoDB, but couldn't find expected database.",
                found);
    }

    @Test
    public void testUsersCount(){
        long expected = 1;
        Assert.assertTrue("Check the connection string", expected <= dao.getUsersCount());
    }
}
