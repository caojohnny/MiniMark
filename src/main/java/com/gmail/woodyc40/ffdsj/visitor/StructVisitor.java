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

package com.gmail.woodyc40.ffdsj.visitor;

/**
 * Visits elements in a struct
 *
 * @author Pierre C
 */
public abstract class StructVisitor<E> {
    /** The start index to iterate */
    private int start = 0;
    /** The end index to iterate */
    private int end = -1;

    /**
     * Specifies the start index to iterate
     *
     * <p>Defaults to 0, at the beginning</p>
     *
     * @param start the start index, inclusive
     * @return the current instance of the visitor
     */
    public StructVisitor<E> from(int start) {
        this.start = start;
        return this;
    }

    /**
     * Specifies the index which to end the iteration
     *
     * <p>Defaults to -1, indicating the end</p>
     *
     * @param end the end index, inclusive
     * @return the current instance
     */
    public StructVisitor<E> to(int end) {
        this.end = end;
        return this;
    }

    /**
     * Obtains the index which to start iterating
     *
     * @return the index
     */
    public int from() {
        return this.start;
    }

    /**
     * Obtains the index which to end iterating
     *
     * @param length the length to end at if the indicated length is the end
     * @return the index
     */
    public int endOr(int length) {
        if (end < 0) return length;
        else return this.end;
    }

    /**
     * Overridden to accept the item which the current iteration is on
     *
     * @param item the item
     */
    public abstract void accept(E item);
}
