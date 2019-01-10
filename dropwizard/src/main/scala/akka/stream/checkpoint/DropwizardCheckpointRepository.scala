package akka.stream.checkpoint

import com.codahale.metrics.{Histogram, MetricRegistry}
import org.mpierce.metrics.reservoir.hdrhistogram.HdrHistogramResetOnSnapshotReservoir

private[checkpoint] object DropwizardCheckpointRepository {

  def apply(name: String, labels: Map[String, String] = Map.empty)(implicit metricRegistry: MetricRegistry): CheckpointRepository = new CheckpointRepository {

    def newHistogram = new Histogram(new HdrHistogramResetOnSnapshotReservoir())

    private val dottedLabels = labels.map{
      case (k,v) => s"$k.$v"
    }.toList
    private val pullLatency       = metricRegistry.register((name + "_pull_latency" :: dottedLabels).mkString("."), newHistogram)
    private val pushLatency       = metricRegistry.register(name + "_push_latency", newHistogram)
    private val backpressureRatio = metricRegistry.register(name + "_backpressure_ratio", newHistogram)
    private val throughput        = metricRegistry.meter(name + "_throughput")
    private val backpressured     = metricRegistry.counter(name + "_backpressured")
    private val failures          = metricRegistry.counter(name + "_failures")
    private val completions       = metricRegistry.counter(name + "_completions")

    backpressured.inc()

    override def markPull(nanos: Long): Unit = {
      pullLatency.update(nanos)
      backpressured.dec()
    }

    override def markPush(nanos: Long, ratio: Long): Unit = {
      pushLatency.update(nanos)
      backpressureRatio.update(ratio)
      throughput.mark()
      backpressured.inc()
    }

    override def markFailure(ex: Throwable): Unit = {
      failures.inc()
    }

    override def markCompletion(): Unit = {
      completions.inc()
    }
  }
}