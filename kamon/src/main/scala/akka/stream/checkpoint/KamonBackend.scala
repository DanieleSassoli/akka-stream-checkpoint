package akka.stream.checkpoint

object KamonBackend {

  implicit val instance: CheckpointBackend = new CheckpointBackend {
    override def createRepository(name: String, labels: Map[String, String]): CheckpointRepository = KamonCheckpointRepository(name, labels)
  }
}
