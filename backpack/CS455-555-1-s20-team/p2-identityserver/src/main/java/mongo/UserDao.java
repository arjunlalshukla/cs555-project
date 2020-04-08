package mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Data access object for the User class and MongoDB
 */
public class UserDao extends AbstractIdentityDao {
    public UserDao(MongoClient mongoClient, String databaseName) {
        super(mongoClient, databaseName);
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
    }

    private final MongoCollection<User> usersCollection;

    /**
     * get a count of users in the database
     * @return number of users in the collection
     */
    public long getUsersCount(){
        return this.usersCollection.countDocuments();
    }

    /**
     * add a user to the collection
     * @param user the new user to create
     * @return the UUID of the new user or throws mongowriteexception
     */
    public String addUser(User user){
        usersCollection.insertOne(user);
        return user.getId().toString();
    }

    /**
     * get a user from the collection
     * @param userName the username for the user
     * @return the user in the database with that username, or null
     */
    public User getUser(String userName) {
        List<Bson> pipeline = new ArrayList<>();
        Bson match = Aggregates.match(Filters.eq("userName", userName));
        Bson project = Aggregates.project(Projections.exclude("hashwd"));
        pipeline.add(match);
        pipeline.add(project);
        return this.usersCollection.aggregate(pipeline).first();
    }

    /**
     * delete a user from the collection
     * @param userName the username to remove
     * @param hashwd the password for the user
     * @return true if the delete was successful, false otherwise
     */
    public boolean deleteUser(String userName, String hashwd) {
        Bson match = Filters.and(Filters.eq("userName", userName), Filters.eq("hashwd",hashwd));
        DeleteResult dResult = this.usersCollection.deleteOne(match);
        return dResult.getDeletedCount() == 1;
    }

    /**
     * update the value for a property of a user
     * @param userName the user to update
     * @param hashwd the password for the user
     * @param field the field to update
     * @param value the new value for the field
     * @return true if successful, false otherwise, or throws MongoWriteException
     */
    public boolean updateUserProperty(String userName, String hashwd, String field, String value){
        if (field.equals("_id")) {return false;}
        Bson query = Filters.and(Filters.eq("userName", userName), Filters.eq("hashwd", hashwd));
        Bson update = new Document(field, value);
        UpdateResult updateResult = usersCollection.updateOne(query, new Document("$set", update));
        return updateResult.getMatchedCount() == 1;
    }

    /**
     * get a user by UUID
     * @param uuid the uuid of the user to retrieve
     * @return instance of User that represents the user in database, or null
     */
    public User getUserByUUID(String uuid) {
        if (! ObjectId.isValid(uuid)){return null;}
        List<Bson> pipeline = new ArrayList<>();
        Bson match = Aggregates.match(Filters.eq("_id", new ObjectId(uuid)));
        Bson project = Aggregates.project(Projections.exclude("hashwd"));
        pipeline.add(match);
        pipeline.add(project);
        return this.usersCollection.aggregate(pipeline).first();
    }

    /**
     * get a list of all users in the database
     * @return an ArrayList populated with User objects for all users
     */
    public ArrayList<User> getAllUsers(){
        ArrayList<User> users = new ArrayList<>();
        List<Bson> pipeline = new ArrayList<>();
        Bson match = Aggregates.match(Filters.exists("userName"));
        Bson project = Aggregates.project(Projections.exclude("hashwd"));
        pipeline.add(match);
        pipeline.add(project);
        this.usersCollection.aggregate(pipeline).into(users);
        return users;
    }
}

