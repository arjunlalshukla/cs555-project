import com.mongodb.MongoWriteException
import org.scalatest.funsuite.AnyFunSuite

class ServerTestSuite extends AnyFunSuite {
  val userName="wizard1"
  val realName="Harry Potter"
  val hashwd="somecrazyhashedpwd"

  test("Should be able to delete a user with a password"){
    IdentityServer.create(userName,realName,hashwd)
    assert(IdentityServer.delete(userName, hashwd))
  }

  test("Should be able to add a user, realname, and with a password"){
    val uid = IdentityServer.create(userName,realName,hashwd)
    assert(uid != null)
    IdentityServer.delete(userName, hashwd)
  }

  test("Should be able to add a user, and a password"){
    val uid = IdentityServer.create(userName,null, hashwd)
    assert(uid != null)
    IdentityServer.delete(userName, hashwd)
  }

  test("Should be able to add a user with a realname but no password"){
    val uid = IdentityServer.create(userName,realName, null)
    assert(uid != null)
    IdentityServer.delete(userName,null)
  }

  test("Should be able to add a user with no password or realname"){
    val uid = IdentityServer.create(userName,null, null)
    assert(uid != null)
    IdentityServer.delete(userName,null)
  }

  test("Should get an exception when creating duplicate users"){
    val uid = IdentityServer.create(userName,realName,hashwd)
    assert(uid != null)
    assertThrows[MongoWriteException]{IdentityServer.create(userName,realName,hashwd)}
  }

  test("Should not be able to create a user without a username"){
    assertThrows[MongoWriteException]{IdentityServer.create(null,realName,hashwd)}
    assertThrows[MongoWriteException]{ IdentityServer.create("",realName,hashwd)}
  }



}
