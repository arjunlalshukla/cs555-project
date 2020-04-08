import com.mongodb.MongoWriteException
import org.scalatest.funsuite.AnyFunSuite
import server.IdentityServer

class ServerTestSuite extends AnyFunSuite {
  val userName="wizard1"
  val realName="Harry Potter"
  val hashwd="somecrazyhashedpwd"

  val ids = new IdentityServer("IdentityServer")

  test("Should be able to delete a user with a password"){
    ids.create(userName,realName, hashwd)
    assert(ids.delete(userName, hashwd))
  }

  test("Should be able to add a user, realname, and with a password"){
    val uid = ids.create(userName, realName, hashwd)
    assert(uid != null)
    ids.delete(userName, hashwd)
  }

  test("Should be able to add a user, and a password"){
    val uid = ids.create(userName,null, hashwd)
    assert(uid != null)
    ids.delete(userName, hashwd)
  }

  test("Should be able to add a user with a realname but no password"){
    val uid = ids.create(userName, realName, null)
    assert(uid != null)
    ids.delete(userName,null)
  }

  test("Should be able to add a user with no password or realname"){
    val uid = ids.create(userName,null, null)
    assert(uid != null)
    ids.delete(userName,null)
  }

  test("Should get an exception when creating duplicate users"){
    val uid = ids.create(userName,realName, hashwd)
    assert(uid != null)
    assertThrows[MongoWriteException]{ids.create(userName,realName, hashwd)}
    assert(ids.delete(userName, hashwd))
  }

  test("Should not be able to create a user without a username"){
    assertThrows[MongoWriteException]{ids.create(null,realName, hashwd)}
    assertThrows[MongoWriteException]{ ids.create("",realName, hashwd)}
  }

  test("Should not be able to delete without proper credentials"){
    val uid = ids.create(userName, realName, hashwd)
    assert(!ids.delete(userName,"badpassword"))
    assert(!ids.delete(userName, null))
    assert(ids.delete(userName, hashwd))
  }

  test( "Should get a list of users back from server by asking for all"){
    ids.create(userName,realName,hashwd)
    ids.create("hgranger","Hermoine Granger", hashwd)
    val users = ids.all
    assert(users.size>=2)
    ids.delete(userName, hashwd)
    ids.delete("hgranger", hashwd)
  }

  test("Should be able to lookup a user by id"){
    val uid = ids.create(userName,realName, hashwd)
    val user = ids.reverse_lookup(uid)
    ids.delete(userName, hashwd)
    assert(user.getUserName==userName)
    assert(user.getId.toString.equals(uid.toString))
    assert(ids.lookup(uid)==null)
  }

  test("Should be able to lookup a user by username"){
    val uid = ids.create(userName,realName, hashwd)
    val user = ids.lookup(userName)
    ids.delete(userName, hashwd)
    assert(user.getUserName==userName)
    assert(user.getId.toString.equals(uid.toString))
    assert(ids.lookup(uid)==null)
  }

  test( "Should get a list of uuids back from server by asking for uids"){
    ids.create(userName,realName,hashwd)
    ids.create("hgranger","Hermoine Granger", hashwd)
    val users = ids.UUIDs
    ids.delete(userName, hashwd)
    ids.delete("hgranger", hashwd)
    assert(users.size>=2)
  }

  test( "Should get a list of usernames back from server by asking for names"){
    ids.create(userName,realName,hashwd)
    ids.create("hgranger","Hermoine Granger", hashwd)
    val users = ids.users
    ids.delete(userName, hashwd)
    ids.delete("hgranger", hashwd)
    assert(users.size>=2)
  }

}
