package akka.stream.checkpoint.benchmarks

import java.util.concurrent.TimeUnit

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.checkpoint.{CheckpointBackend, CheckpointRepository, DropwizardBackend, KamonBackend}
import akka.stream.checkpoint.scaladsl.Checkpoint
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import com.codahale.metrics.MetricRegistry
import org.openjdk.jmh.annotations._

import scala.concurrent._
import scala.concurrent.duration._

/*
[info] Benchmark                                               (numberOfFlows)  (repositoryType)   Mode  Cnt      Score    Error   Units
[info] CheckpointBenchmark.map_with_checkpoints_100k_elements                1              none  thrpt   20  13003.954 ± 92.242  ops/ms
[info] CheckpointBenchmark.map_with_checkpoints_100k_elements                1              noop  thrpt   20   5435.375 ± 75.783  ops/ms
[info] CheckpointBenchmark.map_with_checkpoints_100k_elements                1        dropwizard  thrpt   20   2678.496 ± 34.669  ops/ms
[info] CheckpointBenchmark.map_with_checkpoints_100k_elements                1             kamon  thrpt   20   4475.396 ± 67.713  ops/ms
[info] CheckpointBenchmark.map_with_checkpoints_100k_elements                5              none  thrpt   20   5673.200 ± 74.576  ops/ms
[info] CheckpointBenchmark.map_with_checkpoints_100k_elements                5              noop  thrpt   20   1295.719 ± 62.642  ops/ms
[info] CheckpointBenchmark.map_with_checkpoints_100k_elements                5        dropwizard  thrpt   20    575.954 ± 16.919  ops/ms
[info] CheckpointBenchmark.map_with_checkpoints_100k_elements                5             kamon  thrpt   20   1046.249 ± 12.271  ops/ms
[info] CheckpointBenchmark.map_with_checkpoints_100k_elements               20              none  thrpt   20   1770.884 ± 24.702  ops/ms
[info] CheckpointBenchmark.map_with_checkpoints_100k_elements               20              noop  thrpt   20    352.160 ±  7.212  ops/ms
[info] CheckpointBenchmark.map_with_checkpoints_100k_elements               20        dropwizard  thrpt   20    130.600 ±  3.547  ops/ms
[info] CheckpointBenchmark.map_with_checkpoints_100k_elements               20             kamon  thrpt   20    224.513 ±  6.194  ops/ms
*/
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class CheckpointBenchmark {

  implicit val system = ActorSystem("CheckpointBenchmark")
  implicit val materializer = ActorMaterializer()
  implicit val ctx = system.dispatcher

  implicit val metricRegistry = new MetricRegistry()

  val noopBackend = new CheckpointBackend {
    override def createRepository(name: String): CheckpointRepository = new CheckpointRepository {
      override def markPush(latencyNanos: Long, backpressureRatio: Long): Unit = ()
      override def markPull(latencyNanos: Long): Unit = ()
      override def markFailure(ex: Throwable): Unit = ()
      override def markCompletion(): Unit = ()
    }
  }

  val numberOfElements = 100000

  @Param(Array("1", "5", "20"))
  var numberOfFlows = 1

  @Param(Array("none", "noop", "dropwizard", "kamon"))
  var repositoryType = "none"

  var graph: RunnableGraph[Future[Done]] = _

  @Setup
  def setup(): Unit = {
    val source = Source.repeat(1).take(numberOfElements)
    val flow = Flow[Int].map(_ + 1)

    def viaCheckpoint(n: Int) = {
      def withBackend(backend: CheckpointBackend) = flow.via(Checkpoint[Int](n.toString)(backend))

      repositoryType match {
        case "none" ⇒ flow
        case "noop" ⇒ withBackend(noopBackend)
        case "dropwizard" ⇒ withBackend(DropwizardBackend.fromRegistry)
        case "kamon" ⇒ withBackend(KamonBackend.instance)
      }
    }

    val flows = (1 to numberOfFlows).map(viaCheckpoint).reduce(_ via _)
    graph = source.via(flows).toMat(Sink.ignore)(Keep.right)
  }

  @TearDown
  def shutdown(): Unit = {
    Await.result(system.terminate(), 5.seconds)
  }

  @Benchmark
  @OperationsPerInvocation(100000) // Note: needs to match NumberOfElements.
  def map_with_checkpoints_100k_elements(): Unit = {
    Await.result(graph.run(), Duration.Inf)
  }
}
