package server

import java.rmi.registry.LocateRegistry.getRegistry
import java.rmi.server.UnicastRemoteObject
import java.rmi.{Remote, RemoteException}
import java.util.Properties
import ch.qos.logback.classic.Level
import org.slf4j._
import com.mongodb.{ConnectionString, MongoWriteException}
import com.mongodb.client.{MongoClient, MongoClients}
import javax.rmi.ssl.{SslRMIClientSocketFactory, SslRMIServerSocketFactory}
import mongo.{User, UserDao}

sealed trait IdentityServerInterface extends Remote {
  @throws(classOf[RemoteException])
  def create(login: String, real: String, pw: String): String
  @throws(classOf[RemoteException])
  def delete(login: String, pw: String): Boolean
  @throws(classOf[RemoteException])
  def modify(oldlogin: String, pw: String, newlogin: String): Boolean
  @throws(classOf[RemoteException])
  def all: Array[User]
  @throws(classOf[RemoteException])
  def users: Array[String]
  @throws(classOf[RemoteException])
  def UUIDs: Array[String]
  @throws(classOf[RemoteException])
  def lookup(login: String): User
  @throws(classOf[RemoteException])
  def reverse_lookup(uuid: String): User
}

object IdentityServer {

  val rmiPort: Int = 5191

  def main(args: Array[String]): Unit = {
    System.setProperty("javax.net.ssl.keyStore", "Server_Keystore")
    System.setProperty("javax.net.ssl.keyStorePassword", "test123")
    System.setProperty("java.security.policy", "mysecurity.policy")
    new IdentityServer(args.headOption.getOrElse("server.IdentityServer")).bind()
  }
}
final class IdentityServer(val name: String)  extends IdentityServerInterface {

  private[this] val properties: Properties = new Properties()
  properties.load(ClassLoader.getSystemResourceAsStream("application.properties"))
  private[this] val mongoUri: String = properties.getProperty("mongodb.uri")
  private[this] val databaseName: String = properties.getProperty("mongodb.database")
  private[this] val mongoClient: MongoClient = mongoClient(mongoUri)
  private[this] val dao: UserDao = new UserDao(mongoClient, databaseName)
  private[this] val mongoLogger : ch.qos.logback.classic.Logger = LoggerFactory.getLogger("org.mongodb.driver").asInstanceOf[ch.qos.logback.classic.Logger]
  mongoLogger.setLevel(Level.OFF)


  private[this] def mongoClient(connectionString: String): MongoClient = {
    val connString = new ConnectionString(connectionString)
    val mongoClient = MongoClients.create(connString)
    mongoClient
  }

  private[this] def _allUsers: Array[User] =
    dao.getAllUsers.toArray.map(_.asInstanceOf[User])

  //creates a new user and returns their UUID
  //null otherwise
  def create(login: String, real: String, pw: String): String = {
    val user = new User(login,real,pw)
    try {val uid = dao.addUser(user)
      if (uid == null){
        //error, user not created successfully
      }
      uid
    }
    catch{
      case e: MongoWriteException => null
    }
  }

  //returns true if successful, false otherwise
  def delete(login: String, pw: String): Boolean = dao.deleteUser(login, pw)

  //returns true if successful, false if username not found or bad password
  //throws exception if attempt to modify username to existing username
  def modify(oldlogin: String, pw: String, newlogin: String): Boolean =
    try {dao.updateUserProperty(oldlogin,pw,"userName",newlogin)}
  catch {
    case e: MongoWriteException => false
  }


  //returns all users or null
  def all: Array[User] = _allUsers

  //returns all usernames or null
  def users: Array[String] = _allUsers.map(_.getUserName)

  //returns all userID's or null
  def UUIDs: Array[String] = _allUsers.map(_.getId.toString)

  //returns a user by login
  def lookup(login: String): User = dao.getUser(login)

  //returns a user by userid
  def reverse_lookup(uuid: String): User = dao.getUserByUUID(uuid)

  def bind(): Unit = {
    println(s"starting server.IdentityServer $name")
    getRegistry(IdentityServer.rmiPort)
      .bind(
        name,
        UnicastRemoteObject.exportObject(
          this,
          0,
          new SslRMIClientSocketFactory,
          new SslRMIServerSocketFactory
        )
      )
    println(s"server.IdentityServer '$name' started and registered'")
  }
}
