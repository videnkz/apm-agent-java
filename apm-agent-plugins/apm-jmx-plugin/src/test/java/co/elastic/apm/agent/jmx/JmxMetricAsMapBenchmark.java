package co.elastic.apm.agent.jmx;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("ForLoopReplaceableByForEach")
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class JmxMetricAsMapBenchmark {

    private JmxMetric jmxMetric;

//    @Param({"1", "10", "100"})
//    private int iterateCount;

    @Setup
    public void prepare() {
        jmxMetric = JmxMetric.valueOf("object_name[java.lang:type=GarbageCollector,name=*] attribute[CollectionCount:metric_name=collection_count] attribute[CollectionTime] attribute[CollectionAverageTime] attribute[CollectionSpendTime] attribute[CollectionCalculateTime]");
    }

    @Benchmark
    public void asMapOld(Blackhole fox) {
        for (int i = 0; i < 1000; i++) {
            fox.consume(jmxMetric.asMap());
        }
    }


    @Benchmark
    public void asMapOptimized(Blackhole fox) {
        for (int i = 0; i < 1000; i++) {
            fox.consume(jmxMetric.asMapOptimized());
        }
    }
}
