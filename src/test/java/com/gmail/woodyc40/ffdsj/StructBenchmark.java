/*
 * Copyright 2015 Pierre C
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gmail.woodyc40.ffdsj;

import com.gmail.woodyc40.ffdsj.visitor.StructVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
Benchmark                                 Mode   Samples        Score  Score error    Units
c.g.w.f.StructBenchmark.getList           avgt         5       39.028        6.748    ns/op
c.g.w.f.StructBenchmark.getStruct         avgt         5       39.152        2.891    ns/op
c.g.w.f.StructBenchmark.iterateList       avgt         5      448.754       77.533    ns/op
c.g.w.f.StructBenchmark.iterateStruct     avgt         5      364.938       87.646    ns/op
c.g.w.f.StructBenchmark.putList           avgt         5       49.892        4.725    ns/op
c.g.w.f.StructBenchmark.putStruct         avgt         5       46.625        2.691    ns/op
c.g.w.f.StructBenchmark.removeList        avgt         5       78.443       14.529    ns/op
c.g.w.f.StructBenchmark.removeStruct      avgt         5       78.730       15.734    ns/op
c.g.w.f.StructBenchmark.searchList        avgt         5       78.489       13.801    ns/op
c.g.w.f.StructBenchmark.searchStruct      avgt         5       79.791       26.549    ns/op
 */
public class StructBenchmark {
    private static final int iterations = 100;
    private static final List<Object> LIST = new ArrayList<>(iterations);
    private static final Struct<Object> STRUCT = new Struct<>(iterations);

    static {
        init();
    }

    private static void init() {
        for (int i = 0; i < iterations; i++) {
            LIST.add(new Object());
            STRUCT.insert(new Object());
        }
    }

    public static void main(String[] args) {
        Benchmark benchmark = new Benchmark();
        benchmark.setProfileIterations(1_000_000).setProfilePrintGranularity(10000);
        benchmark.group("Get").perform(new Get())
                .group("Iterate").perform(new Iterate())
                .group("Put").perform(new Put())
                .group("Remove").perform(new Remove())
                .group("Search").perform(new Search());
        benchmark.run();
    }

    public static class Get extends Benchmark.Unit {
        public int idx;
        public Get() {
            setup = () -> idx = new Random().nextInt(100);
        }

        @Benchmark.Measure public long list(Benchmark.Blackhole blackhole) {
            blackhole.consume(LIST.get(idx));
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct(Benchmark.Blackhole blackhole) {
            blackhole.consume(STRUCT.read(idx));
            return System.nanoTime();
        }
    }

    public static class Iterate extends Benchmark.Unit {
        @Benchmark.Measure public long list(Benchmark.Blackhole blackhole) {
            for (Object o : LIST) {
                blackhole.consume(o.hashCode());
            }
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct(Benchmark.Blackhole blackhole) {
            STRUCT.iterate(new StructVisitor<Object>() {
                @Override
                public void accept(Object item) {
                    blackhole.consume(item.hashCode());
                }
            });
            return System.nanoTime();
        }
    }

    public static class Put extends Benchmark.Unit {
        Object object;
        public Put() {
            setup = () -> object = new Object();
        }

        @Benchmark.Measure public long list(Benchmark.Blackhole blackhole) {
            LIST.add(object);
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct(Benchmark.Blackhole blackhole) {
            STRUCT.insert(object);
            return System.nanoTime();
        }
    }

    public static class Remove extends Benchmark.Unit {
        Object remove;
        public Remove() {
            if (LIST.size() < 100) {
                init();
                LIST.add(new Object());
            }

            setup = () -> remove = LIST.get(new Random().nextInt(10));
            teardown = () -> {
                LIST.add(new Object());
            };
        }

        @Benchmark.Measure public long list(Benchmark.Blackhole blackhole) {
            blackhole.consume(LIST.remove(remove));
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct(Benchmark.Blackhole blackhole) {
            blackhole.consume(STRUCT.delete(remove));
            return System.nanoTime();
        }
    }

    public static class Search extends Benchmark.Unit {
        Object find;
        public Search() {
            if (LIST.size() < 100) {
                init();
            }

            setup = () -> find = LIST.get(new Random().nextInt(LIST.size()));
        }

        @Benchmark.Measure public long list(Benchmark.Blackhole blackhole) {
            blackhole.consume(LIST.indexOf(find));
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct(Benchmark.Blackhole blackhole) {
            blackhole.consume(STRUCT.indexOf(find));
            return System.nanoTime();
        }
    }
}