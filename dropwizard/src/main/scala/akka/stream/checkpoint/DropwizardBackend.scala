package akka.stream.checkpoint

import com.codahale.metrics.MetricRegistry

object DropwizardBackend {

  implicit def fromRegistry(implicit metricRegistry: MetricRegistry): CheckpointBackend = new CheckpointBackend {
    override def createRepository(name: String, labels: Map[String, String] = Map.empty): CheckpointRepository = DropwizardCheckpointRepository(name)
  }
}
