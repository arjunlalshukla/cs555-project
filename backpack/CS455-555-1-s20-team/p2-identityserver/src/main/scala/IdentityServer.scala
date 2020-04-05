case class User(login: String, real: String, uuid: String)

object IdentityServer {
  def create(login: String, real: String, pw: String): Unit = {}

  def delete(login: String, pw: String): Unit = {}

  def modify(oldlogin: String, pw: String, newlogin: String): Unit = {}

  def all: Seq[User] = Seq()

  def users: Seq[String] = Seq()

  def UUIDs: Seq[String] = Seq()

  def lookup(login: String): User = User("", "", "")

  def reverse_lookup(uuid: String): User = User("", "", "")
}
