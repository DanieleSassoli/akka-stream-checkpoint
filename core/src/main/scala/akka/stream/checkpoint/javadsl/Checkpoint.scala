package akka.stream.checkpoint.javadsl

import akka.NotUsed
import akka.stream.checkpoint.{CheckpointBackend, CheckpointStage, SystemClock}
import akka.stream.javadsl
import scala.collection.JavaConverters._

object Checkpoint {

  /**
    * Java API
    * Creates a checkpoint Flow.
    *
    * @param name checkpoint identification
    * @param tags set of key, values to add to metrics
    * @param backend backend to store the checkpoint readings
    * @tparam T pass-through type of the elements that will flow through the checkpoint
    * @return a newly created checkpoint Flow
    */
  def create[T](name: String, backend: CheckpointBackend, tags: java.util.Map[String,String]): javadsl.Flow[T, T, NotUsed] =
    javadsl.Flow.fromGraph(CheckpointStage[T](repository = backend.createRepository(name, tags.asScala.toMap), clock = SystemClock))

  def create[T](name: String, backend: CheckpointBackend): javadsl.Flow[T, T, NotUsed] =
    javadsl.Flow.fromGraph(CheckpointStage[T](repository = backend.createRepository(name), clock = SystemClock))

}
