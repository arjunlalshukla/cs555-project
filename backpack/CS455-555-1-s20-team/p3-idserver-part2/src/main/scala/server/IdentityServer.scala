package server

import java.net.{ConnectException, InetAddress}
import java.rmi.registry.LocateRegistry.getRegistry
import java.rmi.server.{RMISocketFactory, UnicastRemoteObject}
import java.rmi.{Remote, RemoteException}
import java.util.Properties

import ch.qos.logback.classic.Level
import org.slf4j._
import com.mongodb.{ConnectionString, MongoWriteException}
import com.mongodb.client.{MongoClient, MongoClients}
import javax.rmi.ssl.{SslRMIClientSocketFactory, SslRMIServerSocketFactory}
import mongo.{Server, ServerDao, User, UserDao}

import scala.util.Sorting

/**
 * Defines the interface that will be exposed to remote clients
 */
sealed trait IdentityServerInterface extends Remote {
  @throws(classOf[RemoteException])
  def create(login: String, real: String, pw: String): ServerResponse
  @throws(classOf[RemoteException])
  def delete(login: String, pw: String): ServerResponse
  @throws(classOf[RemoteException])
  def modify(oldlogin: String, pw: String, newlogin: String): ServerResponse
  @throws(classOf[RemoteException])
  def all: ServerResponse
  @throws(classOf[RemoteException])
  def users: ServerResponse
  @throws(classOf[RemoteException])
  def UUIDs: ServerResponse
  @throws(classOf[RemoteException])
  def lookup(login: String): ServerResponse
  @throws(classOf[RemoteException])
  def reverse_lookup(uuid: String): ServerResponse
  @throws(classOf[RemoteException])
  def heartbeat(): Unit
}

abstract sealed class ServerResponse
case class IAmNotTheCoor(ip: String) extends ServerResponse
case class CreateReturn(ret: Option[String]) extends ServerResponse
case class ModifyReturn(err: Boolean) extends ServerResponse
case class DeleteReturn(err: Boolean) extends ServerResponse
case class UsersReturn(vals: Array[String]) extends ServerResponse
case class UUIDsReturn(vals: Array[String]) extends ServerResponse
case class AllReturn(vals: Array[User]) extends ServerResponse
case class LookupReturn(user: User) extends ServerResponse
case class RevLookReturn(user: User) extends ServerResponse

/**
 * The server driver
 */
object IdentityServer {

  val rmiPort: Int = 5191

  def main(args: Array[String]): Unit = {
    System.setProperty("javax.net.ssl.keyStore", "Server_Keystore")
    System.setProperty("javax.net.ssl.keyStorePassword", "test123")
    System.setProperty("javax.net.ssl.trustStore", "Client_Truststore")
    System.setProperty("javax.net.ssl.trustStorePassword", "changeit")
    System.setProperty("java.security.policy", "mysecurity.policy")
    new IdentityServer(args.headOption.getOrElse("IdentityServer")).startUp()
  }
}

/**
 * Identity server class implements the exposed interface
 * @param name the name for this server to register with RMI
 */
final class IdentityServer(val name: String) extends IdentityServerInterface {

  private[this] val properties: Properties = new Properties()
  properties.load(ClassLoader.getSystemResourceAsStream("application.properties"))
  private[this] val mongoUri: String = properties.getProperty("mongodb.uri")
  private[this] val databaseName: String = properties.getProperty("mongodb.database")
  private[this] val mongoClient: MongoClient = mongoClient(mongoUri)
  private[this] val dao: UserDao = new UserDao(mongoClient, databaseName)
  private[this] val serverDao: ServerDao = new ServerDao(mongoClient, databaseName)
  private[this] val mongoLogger: ch.qos.logback.classic.Logger =
    LoggerFactory.getLogger("org.mongodb.driver")
      .asInstanceOf[ch.qos.logback.classic.Logger]
  mongoLogger.setLevel(Level.OFF)
  private[this] val ip: String = InetAddress.getLocalHost.getHostAddress

  sys.addShutdownHook {
    serverDao.deleteServer(ip)
    println("Shutting down the server")
  }

  def heartbeat(): Unit = ()

  def startUp(): Unit = {
    bind()
    val server = serverDao.getServer(ip)
    if (server == null) {
      serverDao.addServer(new Server(ip,false))
    }
    //start election
    electCoordinator()
  }

  private[this] def electCoordinator(): Unit = {
    //sorted list of ips
    val serverList = serverDao.getAllServers.toArray(Array[Server]()).toSeq
      .map(_.getServerIP)
      .map(_.split("\\.").map(_.toInt))
      .map(x => (x(0)<<24) + (x(1)<<16) + (x(2)<<8) + x(3))
      .sorted
      .map(x => s"${x>>24&0xff}.${x>>16&0xff}.${x>>8&0xff}.${x&0xff}")
    println(serverList)
    val responses = serverList.takeWhile(_ != ip).takeWhile { ipAddr =>
      lazy val stub = getRegistry(ipAddr, IdentityServer.rmiPort)
        .lookup("IdentityServer").asInstanceOf[IdentityServerInterface]
      try {
        //remote server is alive except when hearbeat throws exception
        println(s"heartbeating $ipAddr in election")
        stub.heartbeat()
        true
      } catch {
        case e @ (_: RemoteException |
             _: ConnectException) =>
          println(s"${e.getClass} ${e.getMessage}")
          println(s"heartbeat for $ipAddr did not return")
          false
      }
    }

    if (responses.isEmpty) {
      serverDao.promoteServer(ip)
      println(s"Set own ip to network primary: $ip")
    }
  }

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

  private[this] def primary(func: () => ServerResponse): ServerResponse = {
    val primaryServer = serverDao.getPrimary
    if (primaryServer == null) {
      electCoordinator()
      primary(func)
    } else {
      primaryServer.getServerIP match {
      case this.ip => func()
      case newIp =>
        try {
          lazy val stub = getRegistry(newIp, IdentityServer.rmiPort)
            .lookup("IdentityServer").asInstanceOf[IdentityServerInterface]
          stub.heartbeat()
          IAmNotTheCoor(newIp)
        } catch {
          case e@(_: RemoteException |
                  _: ConnectException) =>
            println(s"${e.getClass} ${e.getMessage}")
            println("Could not contact coordinator, starting election")
            electCoordinator()
            primary(func)
        }
      }
    }
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
  def create(login: String, real: String, pw: String): ServerResponse =
    primary(() =>
      try {
        //if addUser returns null, user not created successfully
        CreateReturn(Option(dao.addUser(new User(login,real,pw))))
      }
      catch{
        case e: MongoWriteException => CreateReturn(None)
      }
    )


  /**
   * delete a user from the database
   * @param login the login to delete
   * @param pw the password for the account
   * @return true if successful, false otherwise
   */
  def delete(login: String, pw: String): ServerResponse =
    primary(() => DeleteReturn(dao.deleteUser(login, pw)))


  /**
   * modify an existing login
   * @param oldlogin the existing user login to change
   * @param pw the password for the user account
   * @param newlogin the new login name to use
   * @return true if successful, false otherwise
   */
  def modify(oldlogin: String, pw: String, newlogin: String): ServerResponse =
    primary(() =>
      try {
        ModifyReturn(dao.updateUserProperty(oldlogin,pw,"userName",newlogin))
      } catch {
        case e: MongoWriteException => ModifyReturn(false)
      }
    )


  /**
   * get all the users from the database
   * @return an array of User objects or null if unsuccessful
   */
  def all: ServerResponse =  primary(() => AllReturn(_allUsers))


  /**
   * get a list of all usernames in the server
   * @return an array of usernames, or null
   */
  def users: ServerResponse =
    primary(() => UsersReturn(_allUsers.map(_.getUserName)))

  /**
   * get a list of all UUIDs in the server
   * @return an array of UUIDs, or null
   */
  def UUIDs: ServerResponse =
    primary(() => UUIDsReturn(_allUsers.map(_.getId.toString)))

  /**
   * gets the user associated with a login
   * @param login the user login to retrieve
   * @return a User object representing the user in the database, or null
   */
  def lookup(login: String): ServerResponse =
    primary(() => LookupReturn(dao.getUser(login)))



  /**
   * gets the user associated with a UUID
   * @param uuid the user UUID to retrieve
   * @return a User object representing the user in the database, or null
   */
  def reverse_lookup(uuid: String): ServerResponse =
    primary(() => RevLookReturn(dao.getUserByUUID(uuid)))

  /**
   * Bind the server to the RMI server to expose it for use
   */
  private[this] def bind(): Unit = {
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
