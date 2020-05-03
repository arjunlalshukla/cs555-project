import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import mongo.Server;
import mongo.ServerDao;
import org.bson.Document;
import org.junit.*;


import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class ServerTest {

    private static Properties properties;
    private ServerDao dao;
    private MongoClient mongoClient;
    private String databaseName;
    private Server testServerGood;
    private Server testServerBad;
    private static String serverIPexists;
    private static String serverIPnoExists;
    private Server primary;


    public ServerTest() throws IOException {
        String mongoUri = getProperty("mongodb.uri");
        databaseName = getProperty("mongodb.database");
        this.mongoClient=mongoClient(mongoUri);
        this.dao = new ServerDao(mongoClient, databaseName);
        serverIPexists = "0.0.0.1";
        serverIPnoExists = "255.10.10.1";
        testServerGood = new Server(serverIPexists, false);
        testServerBad = new Server(serverIPnoExists, false);
        this.primary = null;
    }

    public static MongoClient mongoClient(String connectionString){
        ConnectionString connString = new ConnectionString(connectionString);
        return MongoClients.create(connString);
    }

    public static void init() throws IOException {
        properties = new Properties();
        properties.load(ClassLoader.getSystemResourceAsStream("application.properties"));
    }

    public static String getProperty(String propertyKey) throws IOException{
        if (properties==null) {
            ServerTest.init();
        }
        return properties.getProperty(propertyKey);
    }

    @Before
    public void setupTestServers(){
        primary = dao.getPrimary();
        dao.addServer(this.testServerGood);
        dao.promoteServer(this.testServerGood.getServerIP());
    }

    @After
    public void restoreState(){
        if (primary != null) {
            dao.promoteServer(primary.getServerIP());
        }
        dao.deleteServer(this.testServerBad.getServerIP());
        dao.deleteServer(this.testServerGood.getServerIP());
        assertNull(dao.getServer(serverIPnoExists));
        assertNull(dao.getServer(serverIPexists));
    }

    @Test
    public void testServerSetup(){
        assertEquals("Servers in the database should be found",
                testServerGood.getServerIP(), dao.getServer(serverIPexists).getServerIP());
        assertNull("Servers not in the database should not be found", dao.getServer(serverIPnoExists));
        assertEquals("can locate primary server", serverIPexists, dao.getPrimary().getServerIP());
        dao.addServer(this.testServerBad);
        dao.promoteServer(this.testServerBad.getServerIP());
        assertEquals("updating primary is reflected in database", serverIPnoExists, dao.getPrimary().getServerIP());
        assertTrue("getting list of all servers works", dao.getAllServers().size() >= 2 );
    }

}
