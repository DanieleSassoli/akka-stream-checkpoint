package akka.stream.checkpoint

import com.codahale.metrics.MetricRegistry
import org.scalatest.{MustMatchers, WordSpec}

class DropwizardCheckpointRepositorySpec extends WordSpec with MustMatchers {

  "DropwizardCheckpointRepository" should {

    val registry = new MetricRegistry()

    "store readings in aptly named metrics" when {
      val repository = DropwizardCheckpointRepository("test")(registry)

      "elements are pulled into the checkpoint" in {
        val latency = 42L
        repository.markPull(latency)

        registry.histogram("test_pull_latency").getCount must ===(1)
        registry.histogram("test_pull_latency").getSnapshot.getValues must ===(Array(latency))

        registry.counter("test_backpressured").getCount must ===(0)
      }

      "elements are pushed through the checkpoint" in {
        val latency = 64L
        val backpressureRatio = 54L
        repository.markPush(latency, backpressureRatio)

        registry.histogram("test_push_latency").getCount must ===(1)
        registry.histogram("test_push_latency").getSnapshot.getValues must ===(Array(latency))

        registry.histogram("test_backpressure_ratio").getCount must ===(1)
        registry.histogram("test_backpressure_ratio").getSnapshot.getValues must ===(Array(backpressureRatio))

        registry.meter("test_throughput").getCount must ===(1)

        registry.counter("test_backpressured").getCount must ===(1)
      }

      "the stage fails" in {
        repository.markFailure(new RuntimeException("total failure"))

        registry.counter("test_failures").getCount must ===(1)
      }

      "the stage completes" in {
        repository.markCompletion()

        registry.counter("test_completions").getCount must ===(1)
      }
    }

    "add labels to the metrics" when {
      val repoName = "label_test"
      val repository = DropwizardCheckpointRepository(repoName, Map("aLabel" -> "aValue"))(registry)

      "elements are pulled into the checkpoint" in {
        val latency = 42L
        repository.markPull(latency)

        registry.histogram(s"${repoName}_pull_latency.aLabel.aValue").getCount must ===(1)
        registry.histogram(s"${repoName}_pull_latency.aLabel.aValue").getSnapshot.getValues must ===(Array(latency))
        registry.counter(s"${repoName}_backpressured.aLabel.aValue").getCount must ===(0)
      }
    }
  }
}
