/*
 * Copyright 2015 Pierre C
 * FFDSJ - Fast Fing Data Structures Java
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

import com.google.common.collect.Lists;
import javassist.*;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;
import oshi.hardware.PowerSource;
import oshi.hardware.Processor;
import oshi.software.os.OSFileStore;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Benchmarks code correctly
 *
 * <p>Notes/Lessons learned creating this class:
 *   - Benchmarking is very hard. Even this class is not close to perfect.
 *   - Bytecode synthetics is very hard. It took a few days to make this class.
 *   - Remember to initialize to null
 *   - Remember to set your classpath and get all class dependencies
 *   - Remember to make all dependent methods public or you get IllegalAccess
 *   - Set the classpath in the ClassPool to get previous synthetic classes
 *   - Don't give up
 *   - Don't be afraid to use other libraries
 *   - Watch for license incompatibilities
 *   - Run each test in a separate JVM
 *   - RMI is good, but use Sockets instead to transfer data via localhost
 *   - Poison pills work well for closing the Sockets
 *   - Scan your crash dumps
 *   - Always paste your system information
 *   - Don't be afraid to ask for help
 *   - Test, don't theorize</p>
 *
 * <p>In seeing the shortcomings of traditional benchmarking tools, this is a new, self-contained
 * benchmarking framework using javassist, OSHI, and ASM. Test are synthetic classes used to call
 * methods from a superclass which contains the test methods. Separate JVMs test every method. No
 * concurrency support yet.</p>
 *
 * <p>Classes which hold benchmarks (those extending Unit) must be public, and non-final. Methods
 * must have parameter StatefulOp, and return a long. The long must be System.nanoTime() called
 * IMMEDIATELY after the operation executes. The blackhole consumption can be included if desired.</p>
 *
 * <p>Example:
 * <pre>{@code
 *     public class Bench extends Benchmark.Unit {
 *         public static void main(String args[]) {
 *             new Benchmark().group("Benchmark").perform(new Bench()).run();
 *         }
 *
 *         &#64;Benchmark.Unit public long time(Benchmark.StatefulOp op) {
 *             op.op(...);
 *             return System.nanoTime();
 *         }
 *
 *         // Minimal overhead idiom
 *         &#64;Benchmark.Unit public long timeAnother(Benchmark.StatefulOp op) {
 *             Object o = ...
 *             long time = System.nanoTime();
 *
 *             op.op(o);
 *             return time;
 *         }
 *     }
 * }</pre></p>
 *
 * @author Pierre C
 */
public class Benchmark {
    /**
     * Marks a method which provides a long when finished to profile it
     *
     * @author Pierre C
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    public @interface Measure {
    }

    /** Used for the setup and teardown runnables when nothing is set */
    private static final Runnable NO_OP = () -> {
    };
    /** A cached version of 100 used to calculate percentiles */
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** The iterations to run the profiler */
    private int profileIterations = 500_000_000;
    /** The iterations to run the warmup */
    private int warmupIterations = 100_000;

    /** Insertion order mapping of the group -> benchmarks in that group */
    private final Map<String, Map<String, Mark>> benchmarks = new LinkedHashMap<>();
    /** The name of the current group being edited */
    private String string;

    private String[] args;

    /**
     * Creates a new group which compares the execution time of the methods provided in the units
     *
     * @param string the group name
     * @return the current instance
     */
    public Benchmark group(String string) {
        this.string = string;
        benchmarks.put(string, new HashMap<>());
        return this;
    }

    /**
     * Scans for the methods to test in the unit
     *
     * @param unit the unit of methods
     * @return the current instance
     */
    public Benchmark perform(Unit unit) {
        if (string == null) throw new IllegalStateException("Group cannot be null");

        for (Method s : unit.getClass().getDeclaredMethods()) {
            if (!s.isAnnotationPresent(Measure.class)) continue;

            String key = s.getDeclaringClass().getName().replaceAll("\\.", "_") + "_" + s.getName();
            benchmarks.get(string).put(key, new Mark(key, unit, s.getName()));
        }

        return this;
    }

    // ugh yolo run performance it works

    /**
     * Runs the benchmarks. If your result is empty, it's probably because you aren't calling this method.
     *
     * @param args the jvm args if needed
     * @return the results of running the test
     */
    public Collection<Result> run(String... args) {
        this.args = args;

        Collection<Result> sink = new ArrayList<>();

        for (String s : benchmarks.keySet()) {
            Map<String, Mark> marks = benchmarks.get(s);
            for (String st : marks.keySet()) {
                Mark ma = marks.get(st);
                sink.add(ma.test());
            }
        }

        System.out.println();
        System.out.println("=============================== 8< (Cut here) ===============================");
        System.out.println();
        System.out.println("Results:");

        Table table = new Table();
        table.setNames("Name", "Average");
        for (String s : benchmarks.keySet()) {
            Map<String, Mark> marks = benchmarks.get(s);
            for (Mark mark : marks.values()) {
                Result result = mark.result;
                if (result == null) result = Result.compile(this, mark.name, -1D);
                result.addValues(table, s);
            }
        }
        table.print(System.out);

        System.out.println();
        printSystemInfo();
        System.out.println();
        System.out.println("=============================================================================");

        // agh

        for (String dep : depped) {
            File file = new File(dep);
            file.delete();
        }

        // Remove tld?

        return sink;
    }

    /**
     * Finds the percentile
     *
     * @param outOfHundred 90, or whatever percentage you want
     * @param doubles the data to find the percentile. Must be a TreeMultiset.
     * @return the percentile
     */
    private static double findPercentile(int outOfHundred, Collection<Double> doubles) {
        BigDecimal value = BigDecimal.valueOf(outOfHundred);
        BigDecimal sub = value.multiply(BigDecimal.valueOf(doubles.size()));
        int idx = sub.divide(HUNDRED, BigDecimal.ROUND_HALF_EVEN).intValue();

        // Essentially - we can't use a List - no ordering
        // Can't sort - would take too long, and runs out of heap
        // therefore, we have to do it the long way
        // and use a sorted collection
        // Remember to use a List or other collection which allows dupes!!!
        Iterator<Double> iterator = doubles.iterator();
        for (int i = 0; i < idx; i++) {
            Double l = iterator.next();
            if (i == idx - 1) {
                return l;
            }
        }

        return 0L;
    }

    /**
     * Sets the iterations to warm up the JVM
     *
     * <p>This number is not recommended to be changed. 10.000 and up is good.</p>
     *
     * @param warmupIterations the iterations
     * @return the current instance
     */
    public Benchmark setWarmupIterations(int warmupIterations) {
        this.warmupIterations = warmupIterations;
        return this;
    }

    /**
     * Sets the iteration to profile the methods
     *
     * <p>This number is preferably in the millions or hundred thousands at least</p>
     *
     * @param profileIterations the iterations
     * @return the current instance
     * @deprecated wtf?
     */
    @Deprecated
    public Benchmark setProfileIterations(int profileIterations) {
        this.profileIterations = profileIterations;
        return this;
    }

    /**
     * Stalks your computer and prints the information about it using OSHI.
     *
     * <p>Just kidding, you don't need to call this, but it's helpful if you juset want to know.</p>
     */
    public void printSystemInfo() {
        // Obtains
        // OS, java version, flags, CPU, disks, RAM, power
        System.out.println("System info:");

        String os = System.getProperty("os.name");
        String osVer = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        System.out.printf("Running %s version %s arch %s\n", os, osVer, osArch);

        String javaVer = System.getProperty("java.version");
        String vmVendor = System.getProperty("java.vendor");
        System.out.printf("Java version %s JVM %s\n", javaVer, vmVendor);

        System.out.print("Java flags: ");
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        for (String string : arguments) {
            System.out.print(string + " ");
        }
        for (String arg : args) {
            System.out.print(arg + " ");
        }
        System.out.println();

        SystemInfo info = new SystemInfo();
        HardwareAbstractionLayer layer = info.getHardware();

        Memory memory = layer.getMemory();
        System.out.printf("Memory total %d bytes, usable %d bytes\n", memory.getTotal(), memory.getAvailable());

        Runtime runtime = Runtime.getRuntime();
        System.out.printf("VM memory free %d bytes, max %d bytes, total %d bytes\n", runtime.freeMemory(),
                runtime.maxMemory(), runtime.totalMemory());

        Processor[] procs = layer.getProcessors();
        System.out.println("CPUs (" + procs.length + "):");
        for (Processor p : procs) {
            System.out.printf("  %s\n", p.getName());
        }

        OSFileStore[] stores = layer.getFileStores();
        System.out.print("Disks (" + stores.length + "):\n");
        for (OSFileStore store : stores) {
            System.out.printf("  %s cap %d bytes, usable %d bytes\n",
                    store.getName(), store.getTotalSpace(), store.getUsableSpace());
        }

        PowerSource[] sources = layer.getPowerSources();
        System.out.println("PSUs (" + sources.length + "):");
        for (PowerSource source : sources) {
            System.out.printf("  %s: remaining cap %f, time left %f\n", source.getName(), source.getRemainingCapacity(),
                    source.getTimeRemaining());
        }
    }

    /** The dependencies already loaded by the benchmark */
    private final Set<String> depped = new HashSet<>();

    /**
     * Represents a test of a given method
     *
     * @author Pierre C
     */
    private class Mark {
        private final String name;
        private final Unit unit;
        private final String meName;
        private String invoker;
        private Result result;

        public Mark(String name, Unit unit, String meName) {
            this.name = name;
            this.unit = unit;
            this.meName = meName;
            try {
                try {
                    instrument();
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            } catch (NotFoundException | CannotCompileException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        /**
         * Not really instrumentation... Rather creating a new synthetic class to execute in another JVM
         *
         * @throws NotFoundException
         * @throws CannotCompileException
         * @throws NoSuchMethodException
         * @throws IllegalAccessException
         * @throws InvocationTargetException
         * @throws InstantiationException
         */
        private void instrument() throws NotFoundException, CannotCompileException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            ClassPool classPool = ClassPool.getDefault();
            CtClass superclass = classPool.get(unit.getClass().getName());
            CtClass file = classPool.makeClass(unit.getClass().getPackage().getName() + ".Benchmark_" + name + "_implInvoker",
                    superclass);
            invoker = file.getName();

            CtClass benchmark = classPool.get(Benchmark.class.getName());
            CtClass unit = classPool.get(Unit.class.getName());
            CtClass result = classPool.get(Result.class.getName());

            // javassist bug, too lazy to test if it works on GA 20
            CtConstructor constructor = CtNewConstructor.make("public " + file.getSimpleName() + "() {}", file);
            file.addConstructor(constructor);

            CtMethod warmup;
            CtMethod measure;

            // Make it easier and not need to return null for no reason... resulting in this boilerplate...
            if (this.unit.getClass().getDeclaredMethod(meName, StatefulOp.class).getReturnType().equals(void.class)) {
                warmup = CtNewMethod.make("public long warmup() {\n" +
                        "int done = 0;\n" +
                        "long start = System.nanoTime();\n" +
                        "do {\n" +
                        "    super." + meName + "();\n" +
                        "    done += 1;\n" +
                        "} while (done < " + warmupIterations + ");\n" +
                        "long end = System.nanoTime();\n" +
                        "long elapsed = end - start;\n" +
                        "return (long) (elapsed / " + warmupIterations + ");\n" +
                        "}\n", file);

                measure = CtNewMethod.make("public long measure(int reps) {" +
                        "int done = 0;\n" +
                        "long start = System.nanoTime();\n" +
                        "do {\n" +
                        "    super." + meName + "();\n" +
                        "} while (++done < reps);\n" +
                        "long end = System.nanoTime();" +
                        "return (long) (end - start);}", file);
            } else {
                warmup = CtNewMethod.make("public long warmup() {\n" +
                        "int done = 0;\n" +
                        "long start = System.nanoTime();\n" +
                        "do {\n" +
                        "    op.op(super." + meName + "());\n" +
                        "    done += 1;\n" +
                        "} while (done < " + warmupIterations + ");\n" +
                        "long end = System.nanoTime();\n" +
                        "long elapsed = end - start;\n" +
                        "return (long) (elapsed / " + warmupIterations + ");\n" +
                        "}\n", file);
                measure = CtNewMethod.make("public long measure(int reps) {" +
                        "int done = 0;\n" +
                        "long start = System.nanoTime();\n" +
                        "do {\n" +
                        "    op.op(super." + meName + "());\n" +
                        "} while (++done < reps);\n" +
                        "long end = System.nanoTime();" +
                        "return (long) (end - start);}", file);
            }
            file.addMethod(warmup);
            file.addMethod(measure);

            CtMethod method = CtNewMethod.make("public void doTest() {java.io.DataOutputStream stream = null;\n" +
                    "java.net.Socket socket = null;\n" +
                    "try {System.out.println(\"Starting test " + name + "\");\n" +
                    "System.out.println();\n" +
                    "System.out.println(\"Warming up " + name + "\");\n" +
                    "long warmTime = warmup();\n" +
                    "System.out.println(\"Finished warmup for " + name + "\");\n" +
                    "System.out.println();\n" +

                    "socket = new java.net.Socket(\"localhost\", 5000);\n" +
                    "stream = new java.io.DataOutputStream(socket.getOutputStream());\n" +
                    "System.out.println(\"Starting profile for " + name + "\");\n" +

                    /*
                    In order to get a quantifiable time
                           in the case that warmTime, which is the time
                           taken for each operation in the warmup to complete,
                           is less than the granularity or latency value
                           for the system clock, we need to iterate a few
                           times and get the value of that. The reps is calculated
                           by the amount of times the operation will complete before
                           the system clock updates, given a generous 100 ns.
                    */
                    "\n" +
                    "long nanoAcc = nanoAccuracy();\n" +
                    "int reps = calcIterations(warmTime, nanoAcc, 0);\n" +
                    "\n" +
                    // Do not divide this number
                    // attempting to do so will result in a massive deviation of the
                    // actual time due to rounding retardation
                    // Therefore pass this raw to the socket pipeline
                    "java.math.BigDecimal totalTime = new java.math.BigDecimal(0);\n" +
                    "int done = 0;\n" +

                    "do {\n" +
                    "    long time = 0L;\n" +
                    "    int finished = 0;\n" +
                    "    setup.run();\n" +
                    "    do {\n" +
                    "        reps++;\n" +
                    // The determination method breaks very often, requiring this check
                    "        if (reps + done >= " + profileIterations + ")\n" +
                    "            reps = 1;\n" +
                    "        time = measure(reps);\n" +
                    "        finished = reps;\n" +
                    // Calculate the reps for the next measurement loop
                    "        reps = calcIterations(time / reps, nanoAcc = nanoAccuracy(), (" + profileIterations + " - (done + finished)) - 1);\n" +
                    "    } while (time <= nanoAcc);\n" +
                    "    teardown.run();\n" +
                    "    totalTime = totalTime.add(java.math.BigDecimal.valueOf(time));\n" +
                    "    done += finished;\n" +
                    "} while (done < " + profileIterations + ");\n" +

                    "op = null;\n" +
                    "stream.writeDouble(totalTime.doubleValue());\n" +
                    "System.out.println(\"Finished testing of " + name + "\");\n" +
                    "System.out.println();\n" +
                    "System.out.println(\"Done test for " + name + "\");\n" +
                    "System.out.println();\n" +
                    "} catch (Exception e) {\n" +
                    "    e.printStackTrace();\n" +
                    "    if (stream != null) {\n" +
                    "        stream.writeDouble(-1D);\n" +
                    "    }\n" +
                    "    if (socket != null) socket.close();\n" +
                    "} finally {\n" +
                    "    if (socket != null) socket.close();\n" +
                    "}}", file);
            file.addMethod(method);

            CtMethod main = CtNewMethod.make("public static void main(String[] args) {\n" +
                    invoker + " test = new " + invoker + "();\n" +
                    "int done = 0;\n" + // Settle in the JVM, warmups may not be enough to completely
                                        // transition the VM into ready state
                    "System.out.println(\"Settling VM for " + name + "\");\n" +
                    // Hashcode is a good, fast, psn number
                    "for (int i = 0; i < 5000000; i++) { done += new Object().hashCode(); }\n" +
                    // hi jit
                    "if (done / 2 == 6) { System.out.println(); }\n" +
                    // extra runs to take up time
                    "long time = System.currentTimeMillis();" +
                    "do {" +
                    "test.warmup();" +
                    "} while (System.currentTimeMillis() - time < 2000L);" +
                    "test.doTest();\n" +
                    "}", file);
            file.addMethod(main);

            Class<?> $result = Result.class;
            file.toClass($result.getClassLoader(), $result.getProtectionDomain());
            try {
                file.writeFile();

                ClassReader reader = new ClassReader(file.toBytecode());
                DependencyVisitor visitor = new DependencyVisitor();
                reader.accept(visitor, 0);
                Set<String> classes = visitor.getClasses();
                for (String string : classes) {
                    if (string.equals(Benchmark.class.getName().replaceAll("/", "\\.")))
                        break;
                    getDep(string);
                }

                superclass.writeFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // clean up
            file.detach();
            benchmark.detach();
            unit.detach();
            result.detach();
        }

        /**
         * Recursively obtains the dependencies of the class
         *
         * @param s the class name
         * @throws NotFoundException ...
         * @throws IOException ...
         * @throws CannotCompileException ...
         */
        private void getDep(String s) throws NotFoundException, IOException, CannotCompileException {
            ClassPool.getDefault().appendClassPath(".");
            CtClass cls = ClassPool.getDefault().get(s);
            ClassReader reader = new ClassReader(cls.toBytecode());
            DependencyVisitor visitor = new DependencyVisitor();
            reader.accept(visitor, 0);
            for (String dep : visitor.getClasses()) {
                if (string.equals(Benchmark.class.getName().replaceAll("\\.", "/"))) return;
                if (dep.startsWith("java")) continue;
                if (dep.startsWith("com/sun")) continue;
                if (dep.startsWith("sun")) continue;
                if (dep.startsWith("org/objectweb/asm")) continue;
                if (dep.startsWith("oshi")) continue;
                if (dep.startsWith("com/google/common")) continue;
                if (depped.contains(dep)) continue;

                CtClass depend = ClassPool.getDefault().get(dep);
                depend.writeFile();
                depend.detach();
                depped.add(dep);

                getDep(dep);
            }
        }

        /**
         * Profiles the method
         *
         * <p>Synchronization on the class is necessary in order to prevent the output from becoming
         * screwed up, also prevents a ton of process errors related to IO.</p>
         *
         * @return the result of the test
         */
        public Result test() {
            synchronized (Mark.class) {
                DataInputStream stream = null;
                Socket conn = null;
                ServerSocket socket = null;
                try {
                    String s = System.getProperty("os.name").contains("Windows") ? ".exe" : "";
                    String javaCmd = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java" + s;
                    List<String> args = Lists.newArrayList(javaCmd);
                    Collections.addAll(args, Benchmark.this.args);
                    args.addAll(Lists.newArrayList("-classpath", ".", invoker));

                    ProcessBuilder builder = new ProcessBuilder(args);
                    builder.directory(new File(".")).inheritIO().start();

                    socket = new ServerSocket(5000);
                    while ((conn = socket.accept()) == null) ;

                    stream = new DataInputStream(conn.getInputStream());

                    double d = stream.readDouble();

                    return result = Result.compile(Benchmark.this, name, d / profileIterations);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (conn != null) conn.close();
                        if (stream != null) stream.close();
                        if (socket != null) socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }
    }

    /**
     * Swallows up unused references to make stateful changes and cheat the JIT
     *
     * @author Pierre C
     */
    public static class StatefulOp {
        private int random = prng(0);

        // Only instantiable in this class
        StatefulOp() {
        }

        private int prng(int seed) {
            return Math.abs((int) (System.currentTimeMillis()) - (0x61732 & seed)) / new Object().hashCode();
        }

        // The idea of this class is to check a bunch of random events and see if they are true
        // if they are, then update the state of the internal random value
        // predominantly, it checks if a value is equal to the max value of the data type, and if
        // the random also equals the particular number.
        // Even if the number is the maximum, the random is highly, highly unlikely to be so
        // primarily because currentTimeMillis in the prng function will always return a higher number
        // than the last random's currentTimeMillis. If the subtracted number happens to be negative,
        // then the hashCode needs to be negative as well. I haven't seen many negative hashCodes,
        // and even then, if the number is positive, the hashcode needs to be FRACTIONAL to increase
        // currentTimeMillis to the max integer

        // I am hoping the JIT doesn't DCE the op methods because the last value must be known
        // and even a decent compiler isn't stupid enough to determine the last 0-millions of
        // milliseconds worth just to compute the time...

        // Consumption methods

        public void op(Object o) {
            op(o.hashCode());
        }

        // Each method given for the primitive
        // primitive boxing is very expensive at times
        // otherwise use Object cast as necessary

        // Please java please inline the constants
        public void op(int i) {
            if (i == Integer.MAX_VALUE && i == random) {
                random = prng(random);
            }
        }

        // This can't happen because only 0/x will equal 0
        public void op(float i) {
            if (i != 0 && i / random == 0) {
                random = prng(random);
            }
        }

        // This works because rounding on integer types
        // will round. This won't, so having the numerator
        // equal to zero and equal a quotient of zero isn't
        // happening
        public void op(double i) {
            if (i != 0 && i / random == 0) {
                random = prng(random);
            }
        }

        public void op(char i) {
            if (i == Character.MAX_VALUE && i == random) {
                random = prng(random);
            }
        }

        public void op(short i) {
            if (i == Short.MAX_VALUE && i == random) {
                random = prng(random);
            }
        }

        public void op(long i) {
            if (i == Long.MAX_VALUE && i == random) {
                random = prng(random);
            }
        }

        public void op(byte i) {
            if (i == Byte.MAX_VALUE && i == random) {
                random = prng(random);
            }
        }

        public void op(boolean i) {
            if (!i && random == Integer.MAX_VALUE) {
                random = prng(random);
            }
        }
    }

    /**
     * Extend this class to represent a Unit that has methods which are becnhmarked
     *
     * @author Pierre C
     */
    public static class Unit {
        public StatefulOp op = new StatefulOp();
        public Runnable setup = NO_OP;
        public Runnable teardown = NO_OP;

        /**
         * Determines the latency + granularity of nanoTime
         *
         * <p>Starts the timer, then sets the stop when the next invocation of nanoTime doesn't equal the start.</p>
         *
         * <p>Called internally</p>
         *
         * @return an arbitrary number passed to the iteration determination method
         */
        public final long nanoAccuracy() {
            long start = System.nanoTime();
            long stop;

            while ((stop = System.nanoTime()) == start);

            return stop - start;
        }

        /**
         * Complex method determines the next amount of iterations to run the test iterations
         *
         * <p>Nice to play around with, but calling this method won't do anything. Mostly used internally. Please don't proxy it.</p>
         *
         * <p>The implementation is described in the source</p>
         *
         * @param speculatedTimePerOp the time of the last run per opertion
         * @param nanoAcc the granularity + latency of the nanoTime method
         * @param left the amount of runs left
         * @return the iterations to run next
         */
        public final int calcIterations(long speculatedTimePerOp, long nanoAcc, int left) {
            double timeMustPass = Math.ceil(nanoAcc * 2); // Compensates for fluctuations in
                                                          // accuracy of nanotime

            // This could happen if the rounding becomes retarded
            // resulting in divide by zero error
            // therefore we must set the minimum at the smallest positive integer
            if (speculatedTimePerOp == 0) speculatedTimePerOp = 1;

            // Avoids rechecking too often
            // This number must be small enough such that the runs of this test method
            // will setup/teardown with at least some frequency
            int shouldDo = 4;
            if (speculatedTimePerOp < timeMustPass) {
                // Calculates amount that can be usefully done in the required time
                // minus one because the increment occurs first
                int might = (int) Math.ceil(timeMustPass / speculatedTimePerOp) - 1;

                // If it breaks the minimum, we've got the value
                if (might > shouldDo) {
                    shouldDo = might;
                }

                // Otherwise if it is too small, handle the minimum value
            }

            // If the amount of repetitions left is less than the current amount
            // add that too so we can finish evenly
            // sometimes this will fail... check has been added in the synthetic method
            if (left > 0) {
                if (left <= shouldDo) {
                    shouldDo = left;
                }
            }

            return shouldDo;
        }
    }

    /**
     * Represents the data collected by the benchmark
     *
     * @author Pierre C
     */
    public static class Result {
        // Data
        private final double avg;
        private final String name;

        private Result(Benchmark benchmark, String name, double data) {
            this.name = name;
            this.avg = data;
        }

        public static Result compile(Benchmark benchmark, String name, double data) {
            return new Result(benchmark, name, data);
        }

        public void addValues(Table table, String group) {
            Row row = table.createRow();
            row.setColumn(0, group + " - " + name).setColumn(1, BigDecimal.valueOf(avg)
                    .divide(BigDecimal.ONE, 3, BigDecimal.ROUND_HALF_UP).toString() + " ns");
        }

        /**
         * average time in ns
         */
        public double avg() {
            return avg;
        }

        /**
         * The name of the benchmark
         */
        public String name() {
            return name;
        }
    }

    // Below is ASCII tables (documentation removed)
    // I owned Table, Row, and RowEntry
    // no license needed
    // plus it is apache 2.0 from BukkitCommons
    // Please repect the license header of this file
    // if you are using only the below classes

    class Table {
        private final Collection<Row> rowEntries = new ArrayList<>();
        private int[] max = new int[0];
        private int columns;
        private String[] names = { };

        public Row createRow() {
            Row row = new RowEntry(this);
            this.rowEntries.add(row);
            return row;
        }

        public void setNames(String... names) {
            this.names = names.clone();
            this.max = new int[this.columns = names.length];
        }

        public void print(PrintStream stream) {
            StringBuilder format = new StringBuilder();
            format.append('|');

            StringBuilder dashes = new StringBuilder();

            for (int i1 = 0; i1 < this.max.length; i1++) {
                int maximum = this.max[i1] > this.names[i1].length() ? this.max[i1] : this.names[i1].length();
                format.append("%-").append(maximum).append('s');
                format.append('|');

                if (i1 == 0) dashes.append('+');
                for (int i = 0; i < maximum; i++) {
                    dashes.append('-');
                    if (i == maximum - 1) dashes.append('+');
                }
            }

            stream.println(dashes);
            stream.printf(format.toString(), this.names);
            System.out.println();
            stream.println(dashes);

            for (Row rowEntry : this.rowEntries) {
                stream.printf(format.toString(), rowEntry.getEntries());
                stream.print("\n");
            }
            stream.println(dashes);
        }
    }

    interface Row {
        Row setColumn(int column, String entry);

        String[] getEntries();
    }

    class RowEntry implements Row {
        private final String[] entries;
        private final Table    table;

        public RowEntry(Table table) {
            this.table = table;
            this.entries = new String[table.columns];
        }

        @Override public Row setColumn(int column, String entry) {
            this.entries[column] = entry;

            int max = this.table.max[column];
            if (entry.length() > max)
                this.table.max[column] = entry.length();
            return this;
        }

        @Override public String[] getEntries() {
            return entries;
        }
    }

    ////////////////////////////// WARNING //////////////////////////////
    // The code below is not part of FFDSJ nor is the property of the license holder of this file
    ///////////////////////////////////////////////////////////////////// READ ^
    ///////////////////////////////////////////////////////////////////// READ ^
    ///////////////////////////////////////////////////////////////////// READ ^
    ///////////////////////////////////////////////////////////////////// READ ^
    ///////////////////////////////////////////////////////////////////// READ ^
    ///////////////////////////////////////////////////////////////////// READ ^
    ///////////////////////////////////////////////////////////////////// READ ^
    ///////////////////////////////////////////////////////////////////// READ ^

    /***
     * ASM examples: examples showing how ASM can be used
     * Copyright (c) 2000-2011 INRIA, France Telecom
     * All rights reserved.
     *
     * Redistribution and use in source and binary forms, with or without
     * modification, are permitted provided that the following conditions
     * are met:
     * 1. Redistributions of source code must retain the above copyright
     *    notice, this list of conditions and the following disclaimer.
     * 2. Redistributions in binary form must reproduce the above copyright
     *    notice, this list of conditions and the following disclaimer in the
     *    documentation and/or other materials provided with the distribution.
     * 3. Neither the name of the copyright holders nor the names of its
     *    contributors may be used to endorse or promote products derived from
     *    this software without specific prior written permission.
     *
     * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
     * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
     * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
     * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
     * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
     * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
     * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
     * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
     * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
     * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
     * THE POSSIBILITY OF SUCH DAMAGE.
     */
    /**
     * DependencyVisitor
     *
     * @author Eugene Kuleshov
     */
    // https://github.com/miho/asm-playground/blob/master/ASMSample01/jars/asm-4.0/examples/dependencies/src/org/objectweb/asm/depend/DependencyVisitor.java
    public class DependencyVisitor extends ClassVisitor
    {
        // FFDSJ start diamond operator
        Set<String> packages = new HashSet<>();

        Map<String, Map<String, Integer>> groups = new HashMap<>();
        // FFDSJ end

        Map<String, Integer> current;

        public Map<String, Map<String, Integer>> getGlobals() {
            return groups;
        }

        public Set<String> getClasses() { // FFDSJ getPackages -> getClasses()
            return packages;
        }

        public DependencyVisitor() {
            super(Opcodes.ASM4);
        }

        // ClassVisitor

        @Override
        public void visit(
                final int version,
                final int access,
                final String name,
                final String signature,
                final String superName,
                final String[] interfaces)
        {
            String p = getGroupKey(name);
            current = groups.get(p);
            if (current == null) {
                current = new HashMap<>(); // FFDSJ diamond operator
                groups.put(p, current);
            }

            if (signature == null) {
                if (superName != null) {
                    addInternalName(superName);
                }
                addInternalNames(interfaces);
            } else {
                addSignature(signature);
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(
                final String desc,
                final boolean visible)
        {
            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }

        @Override
        public FieldVisitor visitField(
                final int access,
                final String name,
                final String desc,
                final String signature,
                final Object value)
        {
            if (signature == null) {
                addDesc(desc);
            } else {
                addTypeSignature(signature);
            }
            if (value instanceof Type) {
                addType((Type) value);
            }
            return new FieldDependencyVisitor();
        }

        @Override
        public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String desc,
                final String signature,
                final String[] exceptions)
        {
            if (signature == null) {
                addMethodDesc(desc);
            } else {
                addSignature(signature);
            }
            addInternalNames(exceptions);
            return new MethodDependencyVisitor();
        }

        class AnnotationDependencyVisitor extends AnnotationVisitor {

            public AnnotationDependencyVisitor() {
                super(Opcodes.ASM4);
            }

            @Override
            public void visit(final String name, final Object value) {
                if (value instanceof Type) {
                    addType((Type) value);
                }
            }

            @Override
            public void visitEnum(
                    final String name,
                    final String desc,
                    final String value)
            {
                addDesc(desc);
            }

            @Override
            public AnnotationVisitor visitAnnotation(
                    final String name,
                    final String desc)
            {
                addDesc(desc);
                return this;
            }

            @Override
            public AnnotationVisitor visitArray(final String name) {
                return this;
            }
        }

        class FieldDependencyVisitor extends FieldVisitor {

            public FieldDependencyVisitor() {
                super(Opcodes.ASM4);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                addDesc(desc);
                return new AnnotationDependencyVisitor();
            }
        }

        class MethodDependencyVisitor extends MethodVisitor {

            public MethodDependencyVisitor() {
                super(Opcodes.ASM4);
            }

            @Override
            public AnnotationVisitor visitAnnotationDefault() {
                return new AnnotationDependencyVisitor();
            }

            @Override
            public AnnotationVisitor visitAnnotation(
                    final String desc,
                    final boolean visible)
            {
                addDesc(desc);
                return new AnnotationDependencyVisitor();
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(
                    final int parameter,
                    final String desc,
                    final boolean visible)
            {
                addDesc(desc);
                return new AnnotationDependencyVisitor();
            }

            @Override
            public void visitTypeInsn(final int opcode, final String type) {
                addType(Type.getObjectType(type));
            }

            @Override
            public void visitFieldInsn(
                    final int opcode,
                    final String owner,
                    final String name,
                    final String desc)
            {
                addInternalName(owner);
                addDesc(desc);
            }

            @Override
            public void visitMethodInsn(
                    final int opcode,
                    final String owner,
                    final String name,
                    final String desc)
            {
                addInternalName(owner);
                addMethodDesc(desc);
            }

            @Override
            public void visitInvokeDynamicInsn(
                    String name,
                    String desc,
                    Handle bsm,
                    Object... bsmArgs)
            {
                addMethodDesc(desc);
                addConstant(bsm);
                for(int i=0; i<bsmArgs.length; i++) {
                    addConstant(bsmArgs[i]);
                }
            }

            @Override
            public void visitLdcInsn(final Object cst) {
                addConstant(cst);
            }

            @Override
            public void visitMultiANewArrayInsn(final String desc, final int dims) {
                addDesc(desc);
            }

            @Override
            public void visitLocalVariable(
                    final String name,
                    final String desc,
                    final String signature,
                    final Label start,
                    final Label end,
                    final int index)
            {
                addTypeSignature(signature);
            }

            @Override
            public void visitTryCatchBlock(
                    final Label start,
                    final Label end,
                    final Label handler,
                    final String type)
            {
                if (type != null) {
                    addInternalName(type);
                }
            }
        }

        class SignatureDependencyVisitor extends SignatureVisitor {

            String signatureClassName;

            public SignatureDependencyVisitor() {
                super(Opcodes.ASM4);
            }

            @Override
            public void visitClassType(final String name) {
                signatureClassName = name;
                addInternalName(name);
            }

            @Override
            public void visitInnerClassType(final String name) {
                signatureClassName = signatureClassName + "$" + name;
                addInternalName(signatureClassName);
            }
        }

        // ---------------------------------------------

        private String getGroupKey(String name) {
            // FFDSJ remove package adding logic
            packages.add(name);
            return name;
        }

        private void addName(final String name) {
            if (name == null) {
                return;
            }
            String p = getGroupKey(name);
            if (current.containsKey(p)) {
                current.put(p, current.get(p) + 1);
            } else {
                current.put(p, 1);
            }
        }

        void addInternalName(final String name) {
            addType(Type.getObjectType(name));
        }

        private void addInternalNames(final String[] names) {
            for (int i = 0; names != null && i < names.length; i++) {
                addInternalName(names[i]);
            }
        }

        void addDesc(final String desc) {
            addType(Type.getType(desc));
        }

        void addMethodDesc(final String desc) {
            addType(Type.getReturnType(desc));
            Type[] types = Type.getArgumentTypes(desc);
            for (int i = 0; i < types.length; i++) {
                addType(types[i]);
            }
        }

        void addType(final Type t) {
            switch (t.getSort()) {
                case Type.ARRAY:
                    addType(t.getElementType());
                    break;
                case Type.OBJECT:
                    addName(t.getInternalName());
                    break;
                case Type.METHOD:
                    addMethodDesc(t.getDescriptor());
                    break;
            }
        }

        private void addSignature(final String signature) {
            if (signature != null) {
                new SignatureReader(signature).accept(new SignatureDependencyVisitor());
            }
        }

        void addTypeSignature(final String signature) {
            if (signature != null) {
                new SignatureReader(signature).acceptType(new SignatureDependencyVisitor());
            }
        }

        void addConstant(final Object cst) {
            if (cst instanceof Type) {
                addType((Type) cst);
            } else if (cst instanceof Handle) {
                Handle h = (Handle) cst;
                addInternalName(h.getOwner());
                addMethodDesc(h.getDesc());
            }
        }
    }
}