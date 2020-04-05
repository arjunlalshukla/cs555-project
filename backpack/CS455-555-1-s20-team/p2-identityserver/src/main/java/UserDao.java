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

    public long getUsersCount(){
        return this.usersCollection.countDocuments();
    }

    public String addUser(User user){
        usersCollection.insertOne(user);
        return user.getId().toString();
    }

    public User getUser(String userName) {
        List<Bson> pipeline = new ArrayList<>();
        Bson match = Aggregates.match(Filters.eq("userName", userName));
        Bson project = Aggregates.project(Projections.exclude("hashwd"));
        pipeline.add(match);
        pipeline.add(project);
        return this.usersCollection.aggregate(pipeline).first();
    }

    public boolean deleteUser(String userName, String hashwd) {
        Bson match = Filters.and(Filters.eq("userName", userName), Filters.eq("hashwd",hashwd));
        DeleteResult dResult = this.usersCollection.deleteOne(match);
        return dResult.getDeletedCount() == 1;
    }

    public boolean updateUserProperty(String userName, String hashwd, String field, String value){
        if (field.equals("_id")) {return false;}
        Bson query = Filters.and(Filters.eq("userName", userName), Filters.eq("hashwd", hashwd));
        Bson update = new Document(field, value);
        UpdateResult updateResult = usersCollection.updateOne(query, new Document("$set", update));
        return updateResult.getMatchedCount() == 1;
    }

    public User getUserByUUID(String uuid) {
        if (! ObjectId.isValid(uuid)){return null;}
        List<Bson> pipeline = new ArrayList<>();
        Bson match = Aggregates.match(Filters.eq("_id", new ObjectId(uuid)));
        Bson project = Aggregates.project(Projections.exclude("hashwd"));
        pipeline.add(match);
        pipeline.add(project);
        return this.usersCollection.aggregate(pipeline).first();
    }

    public List<User> getAllUsers(){
        List<User> users = new ArrayList<>();
        List<Bson> pipeline = new ArrayList<>();
        Bson match = Aggregates.match(Filters.exists("userName"));
        Bson project = Aggregates.project(Projections.exclude("hashwd"));
        pipeline.add(match);
        pipeline.add(project);
        this.usersCollection.aggregate(pipeline).into(users);
        return users;
    }

}

