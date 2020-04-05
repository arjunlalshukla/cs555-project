object IdentityServer {
  def create(login: String, real: String, pw: String): Unit = {}

  def delete(login: String, pw: String): Unit = {}

  def modify(oldlogin: String, pw: String, newlogin: String): Unit = {}

  def all: Seq[User] = Seq()

  def users: Seq[String] = Seq()

  def UUIDs: Seq[String] = Seq()

  def lookup(login: String): User = new User()

  def reverse_lookup(uuid: String): User = new User()
}
