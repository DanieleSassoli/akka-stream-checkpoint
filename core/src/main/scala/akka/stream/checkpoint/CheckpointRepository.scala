package akka.stream.checkpoint

import scala.collection.JavaConverters._

/**
  * Represents a storage interface to allow a single checkpoint to store its readings
  */
trait CheckpointRepository {

  /**
    * Allows to store readings following a pull signal
    * @param latencyNanos latency between the last push signal and the registered pull signal in nanoseconds
    */
  def markPull(latencyNanos: Long): Unit

  /**
    * Allows to store readings following a push signal
    * @param latencyNanos latency between the last pull signal and the registered push signal in nanoseconds
    * @param backpressureRatio ratio between the pull latency and the entire last push-push cycle
    */
  def markPush(latencyNanos: Long, backpressureRatio: Long): Unit

  /**
    * Allows to store a failure event
    * @param ex the failure cause
    */
  def markFailure(ex: Throwable): Unit

  /**
    * Allows to store a completion event
    */
  def markCompletion(): Unit
}

/**
  * Factory to create checkpoint repositories. Represents a storage interface to allow
  * multiple checkpoints to store their readings
  */
trait CheckpointBackend {

  def createRepository(name: String, tags: Map[String, String] = Map.empty): CheckpointRepository
}

trait JavaCheckpointBackend extends CheckpointBackend {
  override def createRepository(name: String, tags: Map[String, String] = Map.empty): CheckpointRepository = createRepository(name, tags.asJava)

  def createRepository(name: String, tags: java.util.Map[String, String]): CheckpointRepository
}


