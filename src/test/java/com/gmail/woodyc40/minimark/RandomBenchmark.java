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

package com.gmail.woodyc40.minimark;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomBenchmark extends MiniMark.Unit {
    public static void main(String[] args) {
        new MiniMark().group("random").perform(new RandomBenchmark()).run();
    }

    private static final ThreadLocalRandom CACHED_THREAD_LOCAL_RANDOM = ThreadLocalRandom.current();
    private static final Random CACHED_RANDOM = new Random();

    @MiniMark.Measure public void cachedThreadLocal() {
        op.op(CACHED_THREAD_LOCAL_RANDOM.nextInt());
    }

    @MiniMark.Measure public void newThreadLocal() {
        op.op(ThreadLocalRandom.current().nextInt());
    }

    @MiniMark.Measure public void cachedRandom() {
        op.op(CACHED_RANDOM.nextInt());
    }

    @MiniMark.Measure public void newRandom() {
        op.op(new Random().nextInt());
    }
}
