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
=============================== 8< (Cut here) ===============================

Results:
+-----------------------------------------------------------------+----------+
|Name                                                             |Average   |
+-----------------------------------------------------------------+----------+
|Get - com_gmail_woodyc40_ffdsj_StructBenchmark$Get_list          |29.199 ns |
|Get - com_gmail_woodyc40_ffdsj_StructBenchmark$Get_struct        |28.376 ns |
|Iterate - com_gmail_woodyc40_ffdsj_StructBenchmark$Iterate_list  |388.589 ns|
|Iterate - com_gmail_woodyc40_ffdsj_StructBenchmark$Iterate_struct|257.645 ns|
|Put - com_gmail_woodyc40_ffdsj_StructBenchmark$Put_list          |27.080 ns |
|Put - com_gmail_woodyc40_ffdsj_StructBenchmark$Put_struct        |30.409 ns |
|Remove - com_gmail_woodyc40_ffdsj_StructBenchmark$Remove_struct  |99.837 ns |
|Remove - com_gmail_woodyc40_ffdsj_StructBenchmark$Remove_list    |97.327 ns |
|Search - com_gmail_woodyc40_ffdsj_StructBenchmark$Search_struct  |98.123 ns |
|Search - com_gmail_woodyc40_ffdsj_StructBenchmark$Search_list    |98.151 ns |
+-----------------------------------------------------------------+----------+

System info:
Running Linux version 3.2.0-4-amd64 arch amd64
Java version 1.8.0_40 JVM Oracle Corporation
Java flags: -Didea.launcher.port=7533 -Didea.launcher.bin.path=/home/agenttroll/idea/bin -Dfile.encoding=UTF-8
Memory total 16810151936 bytes, usable 15470944256 bytes
VM memory free 192317792 bytes, max 3736076288 bytes, total 253231104 bytes
CPUs (4):
  Intel(R) Core(TM) i3-3240 CPU @ 3.40GHz
  Intel(R) Core(TM) i3-3240 CPU @ 3.40GHz
  Intel(R) Core(TM) i3-3240 CPU @ 3.40GHz
  Intel(R) Core(TM) i3-3240 CPU @ 3.40GHz
Disks (3):
  / cap 566741975040 bytes, usable 527316275200 bytes
  / cap 566741975040 bytes, usable 527316275200 bytes
  rpc_pipefs cap 0 bytes, usable 0 bytes
PSUs (0):

=============================================================================
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

        @Benchmark.Measure public long list() {
            op.op(LIST.get(idx));
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct() {
            op.op(STRUCT.read(idx));
            return System.nanoTime();
        }
    }

    public static class Iterate extends Benchmark.Unit {
        @Benchmark.Measure public long list() {
            for (Object o : LIST) {
                op.op(o.hashCode());
            }
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct() {
            STRUCT.iterate(new StructVisitor<Object>() {
                @Override
                public void accept(Object item) {
                    op.op(item.hashCode());
                }
            });
            return System.nanoTime();
        }
    }

    public static class Put extends Benchmark.Unit {
        Object object;
        public Put() {
            setup = () -> object = new Object();
            teardown = () -> {
                LIST.clear();
                STRUCT.purge();
            };
        }

        @Benchmark.Measure public long list() {
            LIST.add(object);
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct() {
            STRUCT.insert(object);
            return System.nanoTime();
        }
    }

    public static class Remove extends Benchmark.Unit {
        Object remove;
        public Remove() {
            init();
            remove = LIST.get(2);
        }

        @Benchmark.Measure public long list() {
            op.op(LIST.remove(remove));
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct() {
            op.op(STRUCT.delete(remove));
            return System.nanoTime();
        }
    }

    public static class Search extends Benchmark.Unit {
        Object find;
        public Search() {
            init();
            find = new Object();
        }

        @Benchmark.Measure public long list() {
            op.op(LIST.indexOf(find));
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct() {
            op.op(STRUCT.indexOf(find));
            return System.nanoTime();
        }
    }
}