import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UserTest {
    private static Properties properties;
    private String mongoUri;
    private UserDao dao;
    private MongoClient mongoClient;
    private String databaseName;
    private User testUser;
    private static String userName = "hgranger";

    public static void init() throws IOException {
        properties = new Properties();
        properties.load(ClassLoader.getSystemResourceAsStream("application.properties"));
    }

    public static String getProperty(String propertyKey) throws IOException{
        if (properties==null) {
            UserTest.init();
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
        mongoUri = getProperty("mongodb.uri");
        databaseName = getProperty("mongodb.database");
        this.mongoClient=mongoClient(mongoUri);
        this.dao = new UserDao(mongoClient, databaseName);
        this.dao = new UserDao(mongoClient, databaseName);
        this.testUser = new User(userName);
        this.testUser.setRealName("Hermione Granger");
        this.testUser.setHashwd("somehashedpwd");
    }

    @After
    public void tearDownClass() {
        MongoDatabase db = mongoClient.getDatabase(databaseName);
        db.getCollection("users").deleteMany(new Document("userName", userName));
    }

    @Test
    public void testRegisterUser() {

        assertTrue(
                "Should have correctly created the user - check your write user method",
                dao.addUser(testUser)); // add string explanation

        User user = dao.getUser(testUser.getUserName());
        Assert.assertEquals(testUser.getUserName(), user.getUserName());
        Assert.assertEquals(testUser.getRealName(), user.getRealName());
        Assert.assertEquals(testUser.getHashwd(), user.getHashwd());
    }

    @Test
    public void testDeleteUser(){
        dao.addUser(testUser);
        assertTrue(
                "You should be able to delete correctly the testDb user. Check your delete filter",
                dao.deleteUser(testUser.getUserName()));
        assertNull(
                "User data should not be found after user been deleted. Make sure you delete data from users collection",
                dao.getUser(testUser.getUserName()));
    }

}
