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


  def create(login: String, real: String, pw: String): Unit = {
    val user = new User()
    user.setUserName(login)
    user.setRealName(real)
    user.setHashwd(pw)
    val uid = this.dao.addUser(user)
    if (uid == null){
      //error, user not created successfully
    }
    //need to handle exception in case user already exists
  }

  def delete(login: String, pw: String): Unit = {}

  def modify(oldlogin: String, pw: String, newlogin: String): Unit = {}

  def all: Seq[User] = Seq()

  def users: Seq[String] = Seq()

  def UUIDs: Seq[String] = Seq()

  def lookup(login: String): User = null

  def reverse_lookup(uuid: String): User = null

}
