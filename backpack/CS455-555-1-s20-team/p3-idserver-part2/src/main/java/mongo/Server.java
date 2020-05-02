package mongo;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.io.Serializable;

public class Server implements Serializable {

    private String serverIP;
    private Boolean primary;
    @BsonId
    private ObjectId id;

    public Server() {
         super();
    }

    public Server(String serverIP, boolean primary){
        this.serverIP = serverIP;
        this.primary = primary;
    }

    public String getServerIP(){
        return this.serverIP;
    }

    public void setServerIP(String serverIP){
        this.serverIP = serverIP;
    }

    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    public ObjectId getId() { return this.id; }

    public void setId(ObjectId uuid) { this.id = uuid; }

    @Override
    public String toString() {
        return String.join(" | ", serverIP, primary.toString(), id.toString());
    }
}
