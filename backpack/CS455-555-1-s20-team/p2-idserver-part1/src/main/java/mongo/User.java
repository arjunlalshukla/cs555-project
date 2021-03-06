package mongo;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.io.Serializable;

/**
 * POJO (Plain Old Java Object) class to represent the User model
 */
public class User implements Serializable {

    private String userName;
    private String realName;
    private String hashwd;
    @BsonId
    private ObjectId id;

    public User() {
        super();
    }

    public User(String userName){
        this.userName = userName;
    }

    public User(String userName, String realName, String hashwd){
        this.userName = userName;
        this.realName = realName;
        this.hashwd = hashwd;
    }

    public void setUserName(String userName){
        this.userName = userName;
    }

    public void setRealName(String realName){
        this.realName = realName;
    }

    public String getRealName(){
        return this.realName;
    }

    public void setHashwd(String hashwd){
        this.hashwd = hashwd;
    }

    public String getHashwd(){
        return this.hashwd;
    }

    public String getUserName(){
        return this.userName;
    }

    public ObjectId getId() { return this.id; }

    public void setId(ObjectId uuid) { this.id = uuid; }

    @Override
    public String toString() {
        return String.join(" | ", userName, realName, id.toString());
    }
}
