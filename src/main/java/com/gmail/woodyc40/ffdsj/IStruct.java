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
 * Struct(ure) represents the fastest conceivable list of generic items
 *
 * @author Pierre C
 * @param <E> the item type
 */
public interface IStruct<E> {
    /**
     * Reads the value at a given index of the array
     *
     * <p>This method performs no bounds checks</p>
     *
     * @param index the index to read
     * @return the element at that index, or {@code null} if it doesn't exist or if it is out of bounds
     */
    E read(int index);

    /**
     * Searches the array to find the index of the item provided
     *
     * @param item the item to search for
     * @return the index of the item, -1 if it is not found
     */
    int indexOf(Object item);

    /**
     * Checks to ensure that the collection contains the specified item
     *
     * @param item the item to ensure contains
     * @return {@code true} to indicate that it is present, {@code false} if it is not
     */
    boolean has(Object item);

    /**
     * Inserts the specified item into the collection
     *
     * @param item the item to insert
     */
    void insert(E item);

    /**
     * Removes the item from the collection
     *
     * @param item the item to remove
     * @return {@code true} if the collection was modified as a result of this operation,
     *         {@code false} otherwise
     */
    boolean delete(Object item);

    /**
     * Iterates the collection using the visitor to process each item
     *
     * @param visitor the processor
     */
    void iterate(StructVisitor<E> visitor);

    /**
     * Obtains the entries explicitly added to the collection
     *
     * @return the size of the collection
     */
    int length();

    /**
     * Removes all entries from the collection
     */
    void purge();
}
