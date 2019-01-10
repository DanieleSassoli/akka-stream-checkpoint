package akka.stream.checkpoint

import kamon.Kamon
import kamon.metric.MeasurementUnit

private[checkpoint] object KamonCheckpointRepository {

  def apply(name: String, labels: Map[String, String] = Map.empty): CheckpointRepository = new CheckpointRepository {

    private val pullLatency       = Kamon.histogram(name + "_pull_latency", MeasurementUnit.time.nanoseconds).refine(labels)
    private val pushLatency       = Kamon.histogram(name + "_push_latency", MeasurementUnit.time.nanoseconds).refine(labels)
    private val backpressureRatio = Kamon.histogram(name + "_backpressure_ratio", MeasurementUnit.percentage).refine(labels)
    private val throughput        = Kamon.counter(name + "_throughput").refine(labels)
    private val backpressured     = Kamon.gauge(name + "_backpressured").refine(labels)
    private val failures          = Kamon.gauge(name + "_failures").refine(labels)
    private val completions       = Kamon.gauge(name + "_completions").refine(labels)

    backpressured.increment()

    override def markPull(nanos: Long): Unit = {
      pullLatency.record(nanos)
      backpressured.decrement()
    }

    override def markPush(nanos: Long, ratio: Long): Unit = {
      pushLatency.record(nanos)
      backpressureRatio.record(ratio)
      throughput.increment()
      backpressured.increment()
    }

    override def markFailure(ex: Throwable): Unit = {
      failures.increment()
    }

    override def markCompletion(): Unit = {
      completions.increment()
    }
  }
}