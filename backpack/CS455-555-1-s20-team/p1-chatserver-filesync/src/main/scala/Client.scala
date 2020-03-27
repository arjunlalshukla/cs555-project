import java.io.{File, IOException}
import java.net.Socket
import java.nio.file.{Files, Paths}

import scala.jdk.StreamConverters._


/**
 * Start a client
 * @param args a server host address and a local folder to synchronize
 */
final class Client(args: Seq[String]) {
  if (args.length != 2) {
    throw new FileSyncException(Seq(FileSync.usage))
  }
  val port: Int = Server.validPorts.head
  val syncServer: String = args.head
  val syncFolder: File = new File(args(1))

  if (syncFolder.exists && !syncFolder.isDirectory){
    throw new FileSyncException(Seq("sync folder must be a directory"))
  } else if (!syncFolder.exists){
    syncFolder.mkdir
  }

  val ios: IOStream = IOStream(new Socket(syncServer, port))
  val accepted: Boolean = ios.in.readBoolean
  private[this] val get_path =
    (s: String) => Paths.get(syncFolder.getAbsolutePath, s)
  if (accepted){
    println(s"connected to server $syncServer")
    try {
      while (true) {
        sync(ios)
      }
    } catch {
      case e: IOException =>
        e.printStackTrace()
        println("Connection with server lost")
    }
  } else{
    println(s"could not connect to server $syncServer, unauthorized")
  }

  /**
   * do the synchronization with the server
   * @param io the socket, input and output streams to communicate with
   */
  def sync(io: IOStream): Unit = {
    io.read[IntervalNotify]
    //generate list of files in syncFolder
    val fileList = Files.walk(syncFolder.toPath).toScala(Set).map { path =>
      syncFolder.toPath.relativize(path).toString ->
        FileProperties(path.toFile.lastModified, path.toFile.length)
    }.filter(_._1 != "").toMap
    io.write(fileList)
    val delete = io.read[Set[String]]
    val fileUpdates = io.read[Set[String]]
    val dirUpdates = io.read[Set[String]]
    println(s"To delete:\n${delete.toSeq.sorted.mkString("\n")}\n")
    println(s"Files to update:\n${fileUpdates.toSeq.sorted.mkString("\n")}\n")
    println(s"Dirs to update:\n${dirUpdates.toSeq.sorted.mkString("\n")}\n")

    delete.foreach { path =>
      val p = get_path(path)
      // you cannot delete a directory that has files,
      // so we set recursively walk through in reverse order,
      // because walk() lists from the top down, depth first
      if (p.toFile.exists) {
        Files.walk(p).toScala(LazyList).reverse.foreach(_.toFile.delete())
      }
      println(s"deleted ${path.toString}")
    }

    fileUpdates.foreach { path =>
      io.write(Some(path))
      val contents = io.read[FileContents]
      val file = get_path(path).toFile
      file.getParentFile.mkdirs()
      Files.write(file.toPath, contents.bytes)
      file.setLastModified(contents.ts)
      println(s"Updated ${path.toString}")
    }
    io.write(None)

    dirUpdates.foreach { path =>
      io.write(Some(path))
      val ts = io.in.readLong
      val file = get_path(path).toFile
      file.getParentFile.mkdirs()
      file.mkdir()
      file.setLastModified(ts)
      println(s"updated ${path.toString}")
    }
    io.write(None)
  }
}