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
+-----------------------------------------------------------------+-------+
|Name                                                             |Average|
+-----------------------------------------------------------------+-------+
|Get - com_gmail_woodyc40_ffdsj_StructBenchmark$Get_list          |49.601 |
|Get - com_gmail_woodyc40_ffdsj_StructBenchmark$Get_struct        |47.576 |
|Iterate - com_gmail_woodyc40_ffdsj_StructBenchmark$Iterate_list  |440.676|
|Iterate - com_gmail_woodyc40_ffdsj_StructBenchmark$Iterate_struct|288.996|
|Put - com_gmail_woodyc40_ffdsj_StructBenchmark$Put_list          |63.693 |
|Put - com_gmail_woodyc40_ffdsj_StructBenchmark$Put_struct        |52.075 |
|Remove - com_gmail_woodyc40_ffdsj_StructBenchmark$Remove_struct  |113.798|
|Remove - com_gmail_woodyc40_ffdsj_StructBenchmark$Remove_list    |117.589|
|Search - com_gmail_woodyc40_ffdsj_StructBenchmark$Search_struct  |117.876|
|Search - com_gmail_woodyc40_ffdsj_StructBenchmark$Search_list    |119.091|
+-----------------------------------------------------------------+-------+

System info:
Running Mac OS X version 10.10.3 arch x86_64
Java version 1.8.0_45 JVM Oracle Corporation
Java flags: -Didea.launcher.port=7533 -Didea.launcher.bin.path=/Applications/IntelliJ IDEA 14.app/Contents/bin -Dfile.encoding=UTF-8
Memory total 17179869184 bytes, usable 8480411648 bytes
VM memory free 211986072 bytes, max 3817865216 bytes, total 257425408 bytes
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
  Macintosh HD (/) cap 249795969024 bytes, usable 205430296576 bytes
PSUs (1):
  InternalBattery-0: remaining cap 1.000000, time left -2.000000

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

        @Benchmark.Measure public long list(Benchmark.StatefulOp statefulOp) {
            statefulOp.op(LIST.get(idx));
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct(Benchmark.StatefulOp statefulOp) {
            statefulOp.op(STRUCT.read(idx));
            return System.nanoTime();
        }
    }

    public static class Iterate extends Benchmark.Unit {
        @Benchmark.Measure public long list(Benchmark.StatefulOp statefulOp) {
            for (Object o : LIST) {
                statefulOp.op(o.hashCode());
            }
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct(Benchmark.StatefulOp statefulOp) {
            STRUCT.iterate(new StructVisitor<Object>() {
                @Override
                public void accept(Object item) {
                    statefulOp.op(item.hashCode());
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

        @Benchmark.Measure public long list(Benchmark.StatefulOp statefulOp) {
            LIST.add(object);
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct(Benchmark.StatefulOp statefulOp) {
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

        @Benchmark.Measure public long list(Benchmark.StatefulOp statefulOp) {
            statefulOp.op(LIST.remove(remove));
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct(Benchmark.StatefulOp statefulOp) {
            statefulOp.op(STRUCT.delete(remove));
            return System.nanoTime();
        }
    }

    public static class Search extends Benchmark.Unit {
        Object find;
        public Search() {
            init();
            find = new Object();
        }

        @Benchmark.Measure public long list(Benchmark.StatefulOp statefulOp) {
            statefulOp.op(LIST.indexOf(find));
            return System.nanoTime();
        }

        @Benchmark.Measure public long struct(Benchmark.StatefulOp statefulOp) {
            statefulOp.op(STRUCT.indexOf(find));
            return System.nanoTime();
        }
    }
}