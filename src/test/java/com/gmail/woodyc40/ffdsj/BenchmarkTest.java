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

import sun.reflect.MethodAccessor;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
=============================== 8< (Cut here) ===============================

Results:
+--------------------------------------------------------------+-------+------+
|Name                                                          |Average|Pctile|
+--------------------------------------------------------------+-------+------+
|reflection - com_gmail_woodyc40_ffdsj_StructTest_ReflectInvoke|42.917 |44    |
|reflection - com_gmail_woodyc40_ffdsj_StructTest_NormalInvoke |37.452 |39    |
|reflection - com_gmail_woodyc40_ffdsj_StructTest_SunInvoke    |38.988 |39    |
+--------------------------------------------------------------+-------+------+

System info:
Running Mac OS X version 10.10.3 arch x86_64
Java version 1.8.0_45 JVM Oracle Corporation
Java flags: -Didea.launcher.port=7535 -Didea.launcher.bin.path=/Applications/IntelliJ IDEA 14.app/Contents/bin -Dfile.encoding=UTF-8 -Xms8G
Memory total 17179869184 bytes, usable 8915877888 bytes
VM memory free 187903640 bytes, max 3817865216 bytes, total 257425408 bytes
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
  Macintosh HD (/) cap 249795969024 bytes, usable 205686804480 bytes
PSUs (1):
  InternalBattery-0: remaining cap 0.391308, time left 2940.000000

=============================================================================
 */
public class BenchmarkTest extends Benchmark.Unit {
    private static final Dummy dummy = new Dummy();
    private static Method method;
    private static MethodAccessor accessor;

    public static void main(String... args) {
        new Benchmark().setProfileIterations(5_000_000).setProfilePrintGranularity(100_000)
                .group("reflection").perform(new BenchmarkTest()).run("-Xms8G");
    }

    static {
        try {
            method = Dummy.class.getDeclaredMethod("doWork");
            method.setAccessible(true);
            accessor = ReflectionFactory.getReflectionFactory().newMethodAccessor(method);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Benchmark.Measure public long NormalInvoke(Benchmark.Blackhole blackhole) {
        int i = dummy.doWork();
        long l = System.nanoTime();
        blackhole.consume(i);
        return l;
    }

    @Benchmark.Measure public long ReflectInvoke(Benchmark.Blackhole blackhole) {
        long l = 0;
        try {
            Object o = method.invoke(dummy);
            l = System.nanoTime();
            blackhole.consume(o);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return l;
    }

    private static final Object[] args = new Object[0];
    @Benchmark.Measure public long SunInvoke(Benchmark.Blackhole blackhole) {
        long l = 0;
        try {
            Object o = accessor.invoke(dummy, args);
            l = System.nanoTime();
            blackhole.consume(o);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return l;
    }

    private static class Dummy {
        int i;

        public int doWork() {
            return this.i++;
        }
    }
}
