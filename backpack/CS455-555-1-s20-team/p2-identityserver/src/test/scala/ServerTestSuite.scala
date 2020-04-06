import com.mongodb.MongoWriteException
import org.scalatest.funsuite.AnyFunSuite

class ServerTestSuite extends AnyFunSuite {
  val userName="wizard1"
  val realName="Harry Potter"
  val hashwd="somecrazyhashedpwd"

  test("Should be able to delete a user with a password"){
    IdentityServer.create(userName,realName, hashwd)
    assert(IdentityServer.delete(userName, hashwd))
  }

  test("Should be able to add a user, realname, and with a password"){
    val uid = IdentityServer.create(userName, realName, hashwd)
    assert(uid != null)
    IdentityServer.delete(userName, hashwd)
  }

  test("Should be able to add a user, and a password"){
    val uid = IdentityServer.create(userName,null, hashwd)
    assert(uid != null)
    IdentityServer.delete(userName, hashwd)
  }

  test("Should be able to add a user with a realname but no password"){
    val uid = IdentityServer.create(userName, realName, null)
    assert(uid != null)
    IdentityServer.delete(userName,null)
  }

  test("Should be able to add a user with no password or realname"){
    val uid = IdentityServer.create(userName,null, null)
    assert(uid != null)
    IdentityServer.delete(userName,null)
  }

  test("Should get an exception when creating duplicate users"){
    val uid = IdentityServer.create(userName,realName, hashwd)
    assert(uid != null)
    assertThrows[MongoWriteException]{IdentityServer.create(userName,realName, hashwd)}
    assert(IdentityServer.delete(userName, hashwd))
  }

  test("Should not be able to create a user without a username"){
    assertThrows[MongoWriteException]{IdentityServer.create(null,realName, hashwd)}
    assertThrows[MongoWriteException]{ IdentityServer.create("",realName, hashwd)}
  }

  test("Should not be able to delete without proper credentials"){
    val uid = IdentityServer.create(userName, realName, hashwd)
    assert(!IdentityServer.delete(userName,"badpassword"))
    assert(!IdentityServer.delete(userName, null))
    assert(IdentityServer.delete(userName, hashwd))
  }

  test( "Should get a list of users back from server by asking for all"){
    IdentityServer.create(userName,realName,hashwd)
    IdentityServer.create("hgranger","Hermoine Granger", hashwd)
    val users = IdentityServer.all
    assert(users.size>=2)
    IdentityServer.delete(userName, hashwd)
    IdentityServer.delete("hgranger", hashwd)
  }

  test("Should be able to lookup a user by id"){
    val uid = IdentityServer.create(userName,realName, hashwd)
    val user = IdentityServer.reverse_lookup(uid)
    IdentityServer.delete(userName, hashwd)
    assert(user.getUserName==userName)
    assert(user.getId.toString.equals(uid.toString))
    assert(IdentityServer.lookup(uid)==null)
  }

  test("Should be able to lookup a user by username"){
    val uid = IdentityServer.create(userName,realName, hashwd)
    val user = IdentityServer.lookup(userName)
    IdentityServer.delete(userName, hashwd)
    assert(user.getUserName==userName)
    assert(user.getId.toString.equals(uid.toString))
    assert(IdentityServer.lookup(uid)==null)
  }

  test( "Should get a list of uuids back from server by asking for uids"){
    IdentityServer.create(userName,realName,hashwd)
    IdentityServer.create("hgranger","Hermoine Granger", hashwd)
    val users = IdentityServer.UUIDs
    IdentityServer.delete(userName, hashwd)
    IdentityServer.delete("hgranger", hashwd)
    assert(users.size>=2)
  }

  test( "Should get a list of usernames back from server by asking for names"){
    IdentityServer.create(userName,realName,hashwd)
    IdentityServer.create("hgranger","Hermoine Granger", hashwd)
    val users = IdentityServer.users
    IdentityServer.delete(userName, hashwd)
    IdentityServer.delete("hgranger", hashwd)
    assert(users.size>=2)
  }

}
