import com.mongodb.MongoWriteException
import org.scalatest.funsuite.AnyFunSuite
import server.{AllReturn, CreateReturn, DeleteReturn, IdentityServer, LookupReturn, RevLookReturn, UUIDsReturn, UsersReturn}

class ServerTestSuite extends AnyFunSuite {
  val userName="wizard1"
  val realName="Harry Potter"
  val hashwd="somecrazyhashedpwd"

  var ids = new IdentityServer("IdentityServer")

  test("Should be able to delete a user with a password"){
    ids.create(userName,realName, hashwd)
    assert(ids.delete(userName, hashwd).asInstanceOf[DeleteReturn] == DeleteReturn(true))
  }

  test("Should be able to add a user, realname, and with a password"){
    val uid = ids.create(userName, realName, hashwd).asInstanceOf[CreateReturn]
    assert(uid.ret.orNull != null)
    ids.delete(userName, hashwd)
  }

  test("Should be able to add a user, and a password"){
    val uid = ids.create(userName,null, hashwd).asInstanceOf[CreateReturn]
    assert(uid.ret.orNull != null)
    ids.delete(userName, hashwd)
  }

  test("Should be able to add a user with a realname but no password"){
    val uid = ids.create(userName, realName, null).asInstanceOf[CreateReturn]
    assert(uid.ret.orNull != null)
    ids.delete(userName,null)
  }

  test("Should be able to add a user with no password or realname"){
    val uid = ids.create(userName,null, null).asInstanceOf[CreateReturn]
    assert(uid.ret.orNull != null)
    ids.delete(userName,null)
  }

  test("Should not be able to create duplicate users"){
    val uid = ids.create(userName,realName, hashwd).asInstanceOf[CreateReturn]
    assert(uid.ret.orNull != null)
    assert(null==ids.create(userName,realName, hashwd).asInstanceOf[CreateReturn].ret.orNull)
    assert(ids.delete(userName, hashwd).asInstanceOf[DeleteReturn]==DeleteReturn(true))
  }

  test("Should not be able to create a user without a username"){
    assert(null==ids.create(null,realName, hashwd).asInstanceOf[CreateReturn].ret.orNull)
    assert(null==ids.create("",realName, hashwd).asInstanceOf[CreateReturn].ret.orNull)
  }

  test("Should not be able to delete without proper credentials"){
    val uid = ids.create(userName, realName, hashwd).asInstanceOf[CreateReturn]
    assert(ids.delete(userName,"badpassword").asInstanceOf[DeleteReturn] == DeleteReturn(false))
    assert(ids.delete(userName, null).asInstanceOf[DeleteReturn] == DeleteReturn(false))
    assert(ids.delete(userName, hashwd).asInstanceOf[DeleteReturn] == DeleteReturn(true))
  }

  test( "Should get a list of users back from server by asking for all"){
    ids.create(userName,realName,hashwd)
    ids.create("hgranger","Hermoine Granger", hashwd)
    val users = ids.all.asInstanceOf[AllReturn]
    assert(users.vals.size>=2)
    ids.delete(userName, hashwd)
    ids.delete("hgranger", hashwd)
  }

  test("Should be able to lookup a user by id"){
    val uid = ids.create(userName,realName, hashwd).asInstanceOf[CreateReturn]
    val user = ids.reverse_lookup(uid.ret.orNull).asInstanceOf[RevLookReturn]
    ids.delete(userName, hashwd)
    assert(user.user.getUserName == userName)
    assert(user.user.getId.toString.equals(uid.ret.orNull))
    assert(ids.lookup(uid.ret.orNull).asInstanceOf[LookupReturn].user == null)
  }

  test("Should be able to lookup a user by username"){
    val uid = ids.create(userName,realName, hashwd).asInstanceOf[CreateReturn]
    val user = ids.lookup(userName).asInstanceOf[LookupReturn]
    ids.delete(userName, hashwd)
    assert(user.user.getUserName == userName)
    assert(user.user.getId.toString.equals(uid.ret.orNull))
    assert(ids.lookup(uid.ret.orNull).asInstanceOf[LookupReturn].user == null)
  }

  test( "Should get a list of uuids back from server by asking for uids"){
    ids.create(userName,realName,hashwd)
    ids.create("hgranger","Hermoine Granger", hashwd)
    val users = ids.UUIDs.asInstanceOf[UUIDsReturn]
    ids.delete(userName, hashwd)
    ids.delete("hgranger", hashwd)
    assert(users.vals.size>=2)
  }

  test( "Should get a list of usernames back from server by asking for names"){
    ids.create(userName,realName,hashwd)
    ids.create("hgranger","Hermoine Granger", hashwd)
    val users = ids.users.asInstanceOf[UsersReturn]
    ids.delete(userName, hashwd)
    ids.delete("hgranger", hashwd)
    assert(users.vals.size>=2)
  }

}
