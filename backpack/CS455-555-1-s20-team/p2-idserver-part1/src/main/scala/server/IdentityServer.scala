package server

import java.net.InetAddress
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

/**
 * Defines the interface that will be exposed to remote clients
 */
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

/**
 * The server driver
 */
object IdentityServer {

  val rmiPort: Int = 5191

  def main(args: Array[String]): Unit = {
    System.setProperty("javax.net.ssl.keyStore", "Server_Keystore")
    System.setProperty("javax.net.ssl.keyStorePassword", "test123")
    System.setProperty("java.security.policy", "mysecurity.policy")
    new IdentityServer(args.headOption.getOrElse("IdentityServer")).bind()
  }
}

/**
 * Identity server class implements the exposed interface
 * @param name the name for this server to register with RMI
 */
final class IdentityServer(val name: String)  extends IdentityServerInterface {

  private[this] val properties: Properties = new Properties()
  properties.load(ClassLoader.getSystemResourceAsStream("application.properties"))
  private[this] val mongoUri: String = properties.getProperty("mongodb.uri")
  private[this] val databaseName: String = properties.getProperty("mongodb.database")
  private[this] val mongoClient: MongoClient = mongoClient(mongoUri)
  private[this] val dao: UserDao = new UserDao(mongoClient, databaseName)
  private[this] val mongoLogger : ch.qos.logback.classic.Logger = LoggerFactory.getLogger("org.mongodb.driver").asInstanceOf[ch.qos.logback.classic.Logger]
  mongoLogger.setLevel(Level.OFF)

  /**
   * client builder method
   * @param connectionString the URI string to use for connecting to the db
   * @return an instance of a client to communicate with the db
   */
  private[this] def mongoClient(connectionString: String): MongoClient = {
    val connString = new ConnectionString(connectionString)
    val mongoClient = MongoClients.create(connString)
    mongoClient
  }

  /**
   * get all the users from the database
   * @return an array of User objects from the database
   */
  private[this] def _allUsers: Array[User] =
    dao.getAllUsers.toArray.map(_.asInstanceOf[User])


  /**
   * creates a new user and returns their UUID
   * @param login the login name for the user
   * @param real the real name for the user
   * @param pw the password for the user
   * @return the UUID of the new user, or null if unsuccessful
   */
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


  /**
   * delete a user from the database
   * @param login the login to delete
   * @param pw the password for the account
   * @return true if successful, false otherwise
   */
  def delete(login: String, pw: String): Boolean = dao.deleteUser(login, pw)


  /**
   * modify an existing login
   * @param oldlogin the existing user login to change
   * @param pw the password for the user account
   * @param newlogin the new login name to use
   * @return true if successful, false otherwise
   */
  def modify(oldlogin: String, pw: String, newlogin: String): Boolean =
    try {dao.updateUserProperty(oldlogin,pw,"userName",newlogin)}
  catch {
    case e: MongoWriteException => false
  }


  /**
   * get all the users from the database
   * @return an array of User objects or null if unsuccessful
   */
  def all: Array[User] = _allUsers


  /**
   * get a list of all usernames in the server
   * @return an array of usernames, or null
   */
  def users: Array[String] = _allUsers.map(_.getUserName)

  /**
   * get a list of all UUIDs in the server
   * @return an array of UUIDs, or null
   */
  def UUIDs: Array[String] = _allUsers.map(_.getId.toString)

  /**
   * gets the user associated with a login
   * @param login the user login to retrieve
   * @return a User object representing the user in the database, or null
   */
  def lookup(login: String): User = dao.getUser(login)


  /**
   * gets the user associated with a UUID
   * @param uuid the user UUID to retrieve
   * @return a User object representing the user in the database, or null
   */
  def reverse_lookup(uuid: String): User = dao.getUserByUUID(uuid)

  /**
   * Bind the server to the RMI server to expose it for use
   */
  def bind(): Unit = {
    println(s"starting IdentityServer $name")
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
    println(s"IdentityServer '$name' started and registered' @ ${InetAddress.getLocalHost.getHostAddress}")
  }
}
