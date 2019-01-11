package com.example.javadsl;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.checkpoint.CheckpointBackend;
import akka.stream.checkpoint.CheckpointRepository;
import akka.stream.checkpoint.javadsl.Checkpoint;
import akka.stream.javadsl.Source;

import java.util.concurrent.CompletionStage;

public class CustomBackendExample {

    public static void main(String[] args) {
        final ActorSystem system        = ActorSystem.create("DropwizardExample");
        final Materializer materializer = ActorMaterializer.create(system);

        // #custom
        final CheckpointBackend backend = new CheckpointBackend() {
            @Override
            public CheckpointRepository createRepository(String name, scala.collection.immutable.Map<String, String> labels) {
                return new CheckpointRepository() {
                    @Override
                    public void markPush(long latencyNanos, long backpressureRatio) {
                        System.out.println(String.format("PUSH - %s: latency:%d, backpressure ratio: %d",
                                name, latencyNanos, backpressureRatio));
                    }

                    @Override
                    public void markPull(long latencyNanos) {
                        System.out.println(String.format("PULL - %s: latency:%d",
                                name, latencyNanos));
                    }

                    @Override
                    public void markFailure(Throwable ex) {
                        System.out.println(String.format("FAILED - %s: cause:%s",
                                name, ex.toString()));
                    }

                    @Override
                    public void markCompletion() {
                        System.out.println(String.format("COMPLETED - %s", name));
                    }
                };
            }
        };

        final CompletionStage<Done> done = Source.range(1, 100)
                .via(Checkpoint.create("produced", backend))
                .filter(x -> x % 2 == 0)
                .via(Checkpoint.create("filtered", backend))
                .runForeach(System.out::println, materializer);
        // #custom

        done.thenApply(d -> system.terminate());
    }
}
