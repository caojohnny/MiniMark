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

import com.gmail.woodyc40.ffdsj.visitor.StructVisitor;

/**
 * Implements the IStruct class
 *
 * @author Pierre C
 */
public class Struct<E> implements IStruct<E> {
    /** This is the length to array length ration required to resize the array */
    private static final double DEFAULT_RESIZE_RATIO = 0.75;

    /** Represents an eagerly initialized array of objects representing this collection */
    private Object[] array;
    /** Represents the amount of non-null elements in the array */
    private int length;

    /** The ratio for this instance for array resizing */
    private double resizeRatio = DEFAULT_RESIZE_RATIO;

    /**
     * Creates a new struct with 10 elements
     */
    public Struct() {
        this(10);
    }

    /**
     * Creates a new struct with a specified number of starting elements
     *
     * <p>You may want to use this constructor with considerably larger values if
     * you do not want frequent resizing in the first few insertions.</p>
     *
     * <p>The struct prevents a 0 sized array because the resizing formula does not allow the
     * denominator to be 0 (or throw a DivideByZeroError).</p>
     *
     * @param size the size to initialize the
     * @throws IllegalArgumentException if the size is 0
     */
    public Struct(int size) {
        if (size < 1) throw new IllegalArgumentException("Size " + size + " is below 1");
        this.array = new Object[size];
    }

    /**
     * Sets the ratio of the collection size to the length of the array to resize
     *
     * <p>Resize proceeds when the ratio of elements to array size IS or GREATER THAN the ratio.
     * In essence: {@code length / array.length >= resizeRatio}</p>
     *
     * <p>By default, this number is {@code 0.75}. Lowering increases resize aggressiveness, and
     * increasing the ratio decreases the aggressiveness.</p>
     *
     * <p>The bounds for the ratio is between 1 and 0 inclusive.</p>
     *
     * @param ratio the ratio
     * @throws IllegalArgumentException if the ratio is not between 1 and 0 inclusive
     */
    public void setResizeRatio(double ratio) {
        if (ratio > 1) throw new IllegalArgumentException("Ratio " + ratio + " is over 1");
        if (ratio < 0) throw new IllegalArgumentException("Ratio " + ratio + " is under 0");
        this.resizeRatio = ratio;
    }

    /**
     * Obtains the ratio specified by {@link #setResizeRatio(double)}.
     *
     * @return the resize ratio
     */
    public double resizeRatio() {
        return resizeRatio;
    }

    @Override public E read(int index) {
        return (E) array[index];
    }

    @Override public int indexOf(Object item) {
        Object[] array = this.array;
        for (int i = 0; i < length; i++) {
            Object o = array[i];
            if (item.equals(o)) return i;
        }

        return -1;
    }

    @Override public boolean has(Object item) {
        return indexOf(item) != -1;
    }

    @Override public void insert(E item) {
        resize();
        array[length] = item;
        length++;
    }

    @Override public boolean delete(Object item) {
        Object[] array = this.array;

        for (int i = 0; i < length; i++) {
            Object o = array[i];
            if (item.equals(o)) {
                int shift = length - i - 1;
                System.arraycopy(array, i + 1, array, i, shift);

                length--;
                array[length] = null;
            }
        }

        return false;
    }

    @Override public void iterate(StructVisitor<E> visitor) {
        int start = visitor.from();
        int end = visitor.endOr(this.length - 1);
        // Subtraction of one required because it is inclusive

        for (int i = start; i <= end; i++) {
            E item = (E) array[i];
            visitor.accept(item);
        }
    }

    @Override public int length() {
        return this.length;
    }

    @Override public void purge() {
        array = new Object[16];
        length = 0;
    }

    /**
     * Resizes the array. This is required to allow more elements to be inserted, even after
     * the array fills up.
     */
    private void resize() {
        int arrayLength = array.length;
        double ratio = this.length / arrayLength;
        Object[] array = this.array;

        if (ratio >= resizeRatio) {
            Object[] objects = new Object[arrayLength << 1];
            System.arraycopy(array, 0, objects, 0, arrayLength);
            this.array = objects;
        }
    }
}