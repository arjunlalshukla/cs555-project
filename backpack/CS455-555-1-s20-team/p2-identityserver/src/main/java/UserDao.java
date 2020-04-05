import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

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

    public boolean addUser(User user){
        usersCollection.insertOne(user);
        return true;
    }

    public User getUser(String userName) {
        User user = null;
        List<Bson> pipeline = new ArrayList<>();
        Bson match = Aggregates.match(Filters.eq("userName", userName));
        pipeline.add(match);
        user = this.usersCollection.aggregate(pipeline).first();
        return user;
    }

    public boolean deleteUser(String userName) {
        Bson query = Filters.eq("userName", userName);
        DeleteResult dResult = this.usersCollection.deleteOne(query);
        return dResult.getDeletedCount() == 1;
    }
}

