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
+-----------------------------------------------------------------+------------+
|Name                                                             |Average     |
+-----------------------------------------------------------------+------------+
|reflection - com_gmail_woodyc40_ffdsj_BenchmarkTest_NormalInvoke |9.369311962 |
|reflection - com_gmail_woodyc40_ffdsj_BenchmarkTest_ReflectInvoke|11.529781562|
|reflection - com_gmail_woodyc40_ffdsj_BenchmarkTest_SunInvoke    |10.154492632|
+-----------------------------------------------------------------+------------+

System info:
Running Mac OS X version 10.10.3 arch x86_64
Java version 1.8.0_45 JVM Oracle Corporation
Java flags: -Didea.launcher.port=7536 -Didea.launcher.bin.path=/Applications/IntelliJ IDEA 14.app/Contents/bin -Dfile.encoding=UTF-8
Memory total 17179869184 bytes, usable 8662167552 bytes
VM memory free 239733920 bytes, max 3817865216 bytes, total 257425408 bytes
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
  Macintosh HD (/) cap 249795969024 bytes, usable 205454323712 bytes
PSUs (1):
  InternalBattery-0: remaining cap 1.000000, time left -2.000000

=============================================================================
 */
public class BenchmarkTest extends Benchmark.Unit {
    private static final Dummy dummy = new Dummy();
    private static Method method;
    private static MethodAccessor accessor;

    public static void main(String... args) {
        new Benchmark().group("reflection").perform(new BenchmarkTest()).run();
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

    @Benchmark.Measure public void NormalInvoke(Benchmark.Blackhole blackhole) {
        blackhole.consume(dummy.doWork());
    }

    @Benchmark.Measure public void ReflectInvoke(Benchmark.Blackhole blackhole) {
        try {
            blackhole.consume(method.invoke(dummy));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static final Object[] args = new Object[0];
    @Benchmark.Measure public void SunInvoke(Benchmark.Blackhole blackhole) {
        try {
            blackhole.consume(accessor.invoke(dummy, args));
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static class Dummy {
        int i;

        public int doWork() {
            return this.i++;
        }
    }
}
