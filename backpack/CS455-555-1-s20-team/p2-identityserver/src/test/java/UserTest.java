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
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class UserTest {
    private static Properties properties;
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
        return MongoClients.create(connString);
    }

    @Before
    public void setup() throws IOException {
        String mongoUri = getProperty("mongodb.uri");
        databaseName = getProperty("mongodb.database");
        this.mongoClient=mongoClient(mongoUri);
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

        assertNotNull(
                "Should have correctly created the user - check add user method",
                dao.addUser(testUser)); // add string explanation

        User user = dao.getUser(testUser.getUserName());
        Assert.assertEquals(testUser.getUserName(), user.getUserName());
        Assert.assertEquals(testUser.getRealName(), user.getRealName());
        Assert.assertNull("You should not retrieve the user's password", user.getHashwd());
    }

    @Test
    public void testDeleteUser(){
        //Deleting a user with a password set
        dao.addUser(testUser);
        assertFalse("You should not be able to delete a user with a password with a wrong password",
                dao.deleteUser(testUser.getUserName(),"somewrongpassword"));
        assertFalse( "You should not be able to delete a user with a password without a null password",
                dao.deleteUser(testUser.getUserName(), null));
        assertTrue(
                "You should be able to delete correctly the testDb user.",
                dao.deleteUser(testUser.getUserName(),testUser.getHashwd()));
        assertNull(
                "User data should not be found after user been deleted.",
                dao.getUser(testUser.getUserName()));

        //Deleting user with no password set
        dao.addUser(testUser);
        dao.updateUserProperty(testUser.getUserName(), testUser.getHashwd(),"hashwd",null);
        assertFalse("You should not be able to delete a user with a null password with a wrong password",
                dao.deleteUser(testUser.getUserName(),"somewrongpassword"));
        assertTrue(
                "You should be able to delete correctly the testDb user.",
                dao.deleteUser(testUser.getUserName(),null));
        assertNull(
                "User data should not be found after user been deleted.",
                dao.getUser(testUser.getUserName()));
    }


    @Test
    public void testModifyUser(){
        dao.addUser(testUser);
        assertTrue("You should be able to update the testDb user realname.",
                dao.updateUserProperty(userName, testUser.getHashwd(),
                        "realName","She who shall not be named")
                );
        assertTrue("You should be able update the testDb user hashwd.",
                dao.updateUserProperty(userName, testUser.getHashwd(),"hashwd","newhashedpwd"));
        User user = dao.getUser(testUser.getUserName());
        Assert.assertEquals(testUser.getUserName(), user.getUserName());
        Assert.assertEquals("She who shall not be named", user.getRealName());
        Assert.assertNull("You should not see a user's password", user.getHashwd());

        assertFalse("You should not be able to use your old password, once changed.",
                dao.updateUserProperty(userName,testUser.getHashwd(),"realName","cant update"));
        assertFalse("You should not be able to modify a user with a set password without any password.",
                dao.updateUserProperty(userName,null,"realName","cannot update"));

    }

    @Test
    public void testGetAllUsers(){
        dao.addUser(testUser);
        List<User> users = dao.getAllUsers();
        assertEquals(1, users.size());
        assertNull("You should not retrieve the passwords of users", users.get(0).getHashwd());

    }

    @Test
    public void testGetUserByUserName(){
        dao.addUser(testUser);
        User user = dao.getUser(userName);
        assertEquals("You should be able to lookup a user by username",testUser.getId(), user.getId());
        assertNull("Looking for a username which doesn't exist should return null",
                dao.getUser("somenoneexistingusername"));
    }

    @Test
    public void testGetUserByUUID(){
        String uid = dao.addUser(testUser);
        User user = dao.getUserByUUID(uid);
        assertEquals("You should be able to lookup a user by UUID", testUser.getId(), user.getId());
        assertNull("Looking for a userId which doesn't exist should return null",
                dao.getUserByUUID(dao.generateObjectID().toString()));
        assertNull("Looking for a bad UUID should return null, not throw an exception.",
                dao.getUserByUUID("somebiglongandalsoinvalidUID"));
    }
}
