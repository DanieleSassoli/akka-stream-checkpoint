package akka.stream.checkpoint

import kamon.Kamon
import kamon.testkit.MetricInspection
import org.scalatest.{MustMatchers, WordSpec}

class KamonCheckpointRepositorySpec extends WordSpec with MustMatchers with MetricInspection {

  "KamonCheckpointRepository" should {
    "store readings in aptly named metrics" when {
      val repository = KamonCheckpointRepository("test")

      "elements are pulled into the checkpoint" in {
        val latency = 42L
        repository.markPull(latency)

        val distribution = Kamon.histogram("test_pull_latency").distribution()
        distribution.count must ===(1)
        distribution.max   must ===(latency)

        Kamon.gauge("test_backpressured").value() must ===(0)
      }

      "elements are pushed through the checkpoint" in {
        val latency = 64L
        val backpressureRatio = 54L
        repository.markPush(latency, backpressureRatio)

        val latencyDistro = Kamon.histogram("test_push_latency").distribution()
        latencyDistro.count must ===(1)
        latencyDistro.max   must ===(latency)

        val backpressureDistro = Kamon.histogram("test_backpressure_ratio").distribution()
        backpressureDistro.count must ===(1)
        backpressureDistro.max   must ===(backpressureRatio)

        Kamon.counter("test_throughput").value() must ===(1)

        Kamon.gauge("test_backpressured").value() must ===(1)
      }

      "the stage fails" in {
        repository.markFailure(new RuntimeException("total failure"))

        Kamon.gauge("test_failures").value() must ===(1)
      }

      "the stage completes" in {
        repository.markCompletion()

        Kamon.gauge("test_completions").value() must ===(1)
      }
    }
  }

  "add tags to metrics" when {
    val testTagValue = Map("aTag" -> "aValue")
    val repository = KamonCheckpointRepository("tag_test", testTagValue)

    "elements are pulled into the checkpoint" in {
      repository.markCompletion()

      Kamon.gauge("tag_test_completions").refine(testTagValue).value() must ===(1)
    }
  }
}
