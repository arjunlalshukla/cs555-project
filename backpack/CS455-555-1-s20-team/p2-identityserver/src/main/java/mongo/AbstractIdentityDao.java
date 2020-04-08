package mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;


public abstract class AbstractIdentityDao {

        protected MongoClient mongoClient;
        protected final String IDENTITY_DATABASE;
        protected MongoDatabase db;

        protected AbstractIdentityDao(MongoClient mongoClient, String databaseName) {
            this.mongoClient = mongoClient;
            IDENTITY_DATABASE = databaseName;
            this.db = this.mongoClient.getDatabase(IDENTITY_DATABASE);
        }

        public ObjectId generateObjectID() {return new ObjectId();}

    }

