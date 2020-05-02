package mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import java.util.ArrayList;
import java.util.List;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;


public class ServerDao extends AbstractIdentityDao{
    public ServerDao(MongoClient mongoClient, String databaseName) {
        super(mongoClient, databaseName);
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        serversCollection = db.getCollection("servers", Server.class).withCodecRegistry(pojoCodecRegistry);
    }

    private final MongoCollection<Server> serversCollection;


    /**
     * add a server to the collection
     * @param server the new server to create
     * @return true if success or throws mongowriteexception
     */
    public Boolean addServer(Server server){
        InsertOneResult insertResult = serversCollection.insertOne(server);
        return insertResult.wasAcknowledged();
    }

    /**
     * get a list of all servers in the database
     * @return an ArrayList populated with Server objects for all servers
     */
    public ArrayList<Server> getAllServers(){
        ArrayList<Server> servers = new ArrayList<>();
        List<Bson> pipeline = new ArrayList<>();
        Bson match = Aggregates.match(Filters.exists("serverIP"));
        pipeline.add(match);
        this.serversCollection.aggregate(pipeline).into(servers);
        return servers;
    }

    /**
     * get a server from the collection
     * @param serverIP the IP for the server
     * @return the server in the database with that IP, or null
     */
    public Server getServer(String serverIP) {
        List<Bson> pipeline = new ArrayList<>();
        Bson match = Aggregates.match(Filters.eq("serverIP", serverIP));
        pipeline.add(match);
        return this.serversCollection.aggregate(pipeline).first();
    }

    /**
     * delete a server from the collection
     * @param serverIP the serverIP to remove
     * @return true if the delete was successful, false otherwise
     */
    public boolean deleteServer(String serverIP) {
        Bson match = Filters.eq("serverIP", serverIP);
        DeleteResult dResult = this.serversCollection.deleteOne(match);
        return dResult.getDeletedCount() == 1;
    }

    /**
     * get the primary server from the database
     * @return Server which is primary
     */
    public Server getPrimary(){
        Bson match = Filters.eq("primary", true);
        return this.serversCollection.find(match).first();
    }

    /**
     * promote a server to primary and demote all other servers
     * @param serverIP the IP address of the server to make primary
     * @return true if updated the server to primary, false otherwise
     */
    public boolean promoteServer(String serverIP) {
        List<Bson> pipeline = new ArrayList<>();
        Bson match = Aggregates.match(Filters.eq("serverIP", serverIP));
        Bson nomatch = Aggregates.match(Filters.ne("serverIP", serverIP));
        pipeline.add(nomatch);
        this.serversCollection.updateMany(Filters.ne("serverIP", serverIP), Updates.set("primary", false));
        UpdateResult updateResult = this.serversCollection.updateOne(Filters.eq("serverIP", serverIP), Updates.set("primary", true));
        return updateResult.getMatchedCount() == 1;
    }

}
