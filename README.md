# Fastest-Fing-Data-Structure-Java
Really fast data structures

# Usage

Using the data structures

## Struct

```java
IStruct<Object> struct = new Struct<>();

// Reads the struct at the nth index
Object o = struct.read(n);

// Checks for existance of the object in the struct
boolean contains = struct.has(new Object());

// Places an element into the struct
struct.insert(new Object());

// Removes an element
boolean done = struct.remove(new Object());

// Visits each element in the struct within the specified parameters
// Check the StructVisitor for information
struct.iterate(new StructVisitor<Object>() {
    @Override public void accept(Object visit) {
        // Perform something
    }
}.from(10).to(-1));

// Clears the struct
struct.purge();
```

# Benchmarking

A little special class I made for my own purposees that you can use too.

## Usage

```java
// Public, non final
public class Bench extends Benchmark.Unit {
    // Field members may be private
    private final List<Object> list = new ArrayList<>();
    private Object add;

    // Default constructor
    public Bench() {
        // Sets the setup and teardown runnables
        setup = () -> {
            list.add(new Object());
            add = new Object();
        };
        teardown = () -> list.clear();
    }

    public static void main(String[] args) {
        Benchmark mark = new Benchmark();
        // Runs the benchmark
        mark.group("ArrayList Speed").profile(new Bench()).run();
    }
    
    // Returns void, has no args
    @Benchmark.Measure public void add() {
        op.op(list.add(add));
    }
    
    // Same as add, but returns something
    @Benchmark.Measure public Object remove() {
        return internalRemove();
    }
    
    // Internally used methods can be private
    private Object internalRemove() {
        return list.remove(0);
    }
}
```

Benchmark classes must be public, non-final, and have a public, default constructor. The class must extend `Benchmark.Unit`. This is so the synthesiser can create a synthetic class that extends the unit. All benchmark methods are annotated with `@Benchmark.Measure`, and should have no parameters. You may choose any return type. Use the return type to automatically consume operations that need to change the state of the application to prevent Dead Code Elimination.

To run the benchmark, setup the `main` method, instantiate `new Benchmark()`, then add a group, then profile the instance of the class, and invoke `run`. To provide JVM arguments for the runners, specify each as a varargs array in the `run` method.

`op` is a StatefulOp which allows you to explicitly consume objects rather than removing them to prevent DCE.

## Reading benchmarks

```
=============================== 8< (Cut here) ===============================

Results:
+-----------------------------------------------------------------+--------+
|Name                                                             |Average |
+-----------------------------------------------------------------+--------+
|reflection - com_gmail_woodyc40_ffdsj_BenchmarkTest_NormalInvoke |1.025 ns|
|reflection - com_gmail_woodyc40_ffdsj_BenchmarkTest_ReflectInvoke|8.216 ns|
|reflection - com_gmail_woodyc40_ffdsj_BenchmarkTest_SunInvoke    |7.223 ns|
+-----------------------------------------------------------------+--------+

System info:
Running Mac OS X version 10.10.3 arch x86_64
Java version 1.8.0_45 JVM Oracle Corporation
Java flags: -Didea.launcher.port=7533 -Didea.launcher.bin.path=/Applications/IntelliJ IDEA 14.app/Contents/bin -Dfile.encoding=UTF-8 
Memory total 17179869184 bytes, usable 9059049472 bytes
VM memory free 223826768 bytes, max 3817865216 bytes, total 257425408 bytes
CPUs (8):
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz
  Intel(R) Core(TM) i7-4770HQ CPU @ 2.20GHz
Disks (1):
  Macintosh HD (/) cap 249795969024 bytes, usable 205422809088 bytes
PSUs (1):
  InternalBattery-0: remaining cap 1.000000, time left -2.000000

=============================================================================
```

The start and end equals (`=`) signs make it easier to see where you want to copy all of your information.

Here is a row of the table: `|reflection - com_gmail_woodyc40_ffdsj_BenchmarkTest_SunInvoke    |7.223 ns|`
The shows you many things: The test is in the `reflection` group. The enclosing class is `BenchmarkTest`. The method name is `SunInvoke`. The time for that method is `7.223 ns`.

You also get a wealth of system information. You have the OS name, version, and architecture. You also have the Java version and JVM vendor, as well as the runtime flags. You have the memory and allocated RAM included in the Memory/VM memory. Then there is the typical CPU printouts, vendor, name, and clock, while each CPU may not be one of its own, it is either the threads or cores of the CPU. Then there is the disk, provided in case you are doing IO. And then there is power, at `1.0000...` because the battery is 100%, and `-2` time (charging). Battery throttling of the CPU can affect performance.
