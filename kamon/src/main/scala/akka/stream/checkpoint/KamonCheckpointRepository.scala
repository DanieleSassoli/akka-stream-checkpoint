package akka.stream.checkpoint

import kamon.Kamon
import kamon.metric.MeasurementUnit

private[checkpoint] object KamonCheckpointRepository {

  def apply(name: String): CheckpointRepository = new CheckpointRepository {

    private val pullLatency       = Kamon.histogram(name + "_pull_latency", MeasurementUnit.time.nanoseconds)
    private val pushLatency       = Kamon.histogram(name + "_push_latency", MeasurementUnit.time.nanoseconds)
    private val backpressureRatio = Kamon.histogram(name + "_backpressure_ratio", MeasurementUnit.percentage)
    private val throughput        = Kamon.counter(name + "_throughput")
    private val backpressured     = Kamon.gauge(name + "_backpressured")
    private val failures          = Kamon.gauge(name + "_failures")
    private val completions       = Kamon.gauge(name + "_completions")

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