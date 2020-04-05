import java.io.IOException
import java.util.Properties
import com.mongodb.ConnectionString
import com.mongodb.client.{MongoClient, MongoClients}


object IdentityServer {

  var properties:Properties = new Properties()
  properties.load(ClassLoader.getSystemResourceAsStream("application.properties"))
  val mongoUri: String = getProperty("mongodb.uri")
  val databaseName: String = getProperty("mongodb.database")
  val mongoClient: MongoClient = mongoClient(mongoUri)
  val dao: UserDao = new UserDao(mongoClient, databaseName)

  @throws[IOException]
  def getProperty(propertyKey: String): String = {
    properties.getProperty(propertyKey)
  }

  def mongoClient(connectionString: String): MongoClient = {
    val connString = new ConnectionString(connectionString)
    val mongoClient = MongoClients.create(connString)
    mongoClient
  }


  //creates a new user and returns their UUID
  //throws exception on duplicate username
  //null otherwise
  def create(login: String, real: String, pw: String): String = {
    val user = new User()
    user.setUserName(login)
    user.setRealName(real)
    user.setHashwd(pw)
    val uid = this.dao.addUser(user)
    if (uid == null){
      //error, user not created successfully
    }
    //need to handle exception on client side in case user already exists
    uid
  }

  //returns true if successful, false otherwise
  def delete(login: String, pw: String): Boolean = {
    dao.deleteUser(login, pw)
  }

  //returns true if successful, false if username not found or bad password
  //throws exception if attempt to modify username to existing username
  def modify(oldlogin: String, pw: String, newlogin: String): Boolean = {
    dao.updateUserProperty(oldlogin,pw,"userName",newlogin)
  }


  def all: Seq[User] = Seq()

  def users: Seq[String] = Seq()

  def UUIDs: Seq[String] = Seq()

  def lookup(login: String): User = null

  def reverse_lookup(uuid: String): User = this.dao.getUserByUUID(uuid)

}
