public class User {

    private String userName;
    private String realName;
    private String hashwd;
    private String uuid;

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

    public String getUUID() { return this.uuid; }

    public void setUUID(String uuid) { this.uuid = uuid; }

}
