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
+-----------------------------------------------------------------+-------+------+
|Name                                                             |Average|Pctile|
+-----------------------------------------------------------------+-------+------+
|Get - com_gmail_woodyc40_ffdsj_StructBenchmark$Get_list          |43.442 |40    |
|Get - com_gmail_woodyc40_ffdsj_StructBenchmark$Get_struct        |41.408 |39    |
|Iterate - com_gmail_woodyc40_ffdsj_StructBenchmark$Iterate_list  |406.412|392   |
|Iterate - com_gmail_woodyc40_ffdsj_StructBenchmark$Iterate_struct|282.134|272   |
|Put - com_gmail_woodyc40_ffdsj_StructBenchmark$Put_list          |45.453 |41    |
|Put - com_gmail_woodyc40_ffdsj_StructBenchmark$Put_struct        |47.832 |45    |
|Remove - com_gmail_woodyc40_ffdsj_StructBenchmark$Remove_struct  |84.222 |81    |
|Remove - com_gmail_woodyc40_ffdsj_StructBenchmark$Remove_list    |63.25  |61    |
|Search - com_gmail_woodyc40_ffdsj_StructBenchmark$Search_struct  |79.906 |73    |
|Search - com_gmail_woodyc40_ffdsj_StructBenchmark$Search_list    |70.442 |56    |
+-----------------------------------------------------------------+-------+------+

System info:
Running Mac OS X version 10.10.3 arch x86_64
Java version 1.8.0_45 JVM Oracle Corporation
Java flags: -Didea.launcher.port=7535 -Didea.launcher.bin.path=/Applications/IntelliJ IDEA 14.app/Contents/bin -Dfile.encoding=UTF-8
Memory total 17179869184 bytes, usable 8692142080 bytes
VM memory free 330471200 bytes, max 3817865216 bytes, total 783810560 bytes
CPUs (8):
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz x64
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz x64
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz x64
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz x64
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz x64
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz x64
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz x64
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz x64
Disks (1):
  Macintosh HD (/) cap 249795969024 bytes, usable 205665140736 bytes
PSUs (1):
  InternalBattery-0: remaining cap 0.331436, time left -2.000000

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
        benchmark.setProfileIterations(5_000_000);
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