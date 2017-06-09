/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class with methods that ease creation of common parametrized
 * collection types. For example, to declare a list of strings, normally you
 * would need to do this:
 * 
 * <pre>
 * &#064;code
 * import java.util.*;
 * ...
 * List&lt;String&gt; strings = new ArrayList&lt;String&gt;();
 * }
 * </pre>
 * <p>
 * With this class, you can simplify somewhat with:
 * 
 * <pre>
 * &#064;code
 * import edu.unc.lib.dl.util.CollectionUtil;
 * List&lt;String&gt; strings = DRY.newArrayList();
 * }
 * </pre>
 * 
 * </p>
 * <p>
 * More significant cognitive and typing savings come when initializing map
 * types:
 * </p>
 * 
 * <pre>
 * &#064;code
 * Map&lt;String, HttpClient&gt; clients = new HashMap&lt;String,HttpClient&gt;();
 * }
 * </pre>
 * <p>
 * Which, with this class, becomes:
 * 
 * <pre>
 * &#064;code
 * Map&lt;String,HttpClient&gt; clients = DRY.newHashMap();
 * }
 * </pre>
 * 
 * </p>
 * 
 * @author count0
 */
public final class CollectionUtil {

    // private constructor to prevent instantiation.
    private CollectionUtil() {
    }

    /**
     * Creates a new parametrized <code>ArrayList</code>
     * 
     * @param <E>
     *            the type that is to be stored in the list.
     * @return a new parametrized <code>ArrayList</code>
     */
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<E>();
    }

    /**
     * Creates a new parameterized <code>ArrayList</code> with a specified
     * initial capacity.
     * 
     * @param <E>
     *            the type that is to be stored in the list.
     * @param capacity
     *            the initial capacity of the underlying array.
     * @return a new parameterized <code>ArrayList</code>.
     **/
    public static <E> ArrayList<E> newArrayList(final int capacity) {
        return new ArrayList<E>(capacity);
    }

    /**
     * Creates a new parameterized <code>HashMap</code> with default capacity.
     * 
     * @param <K>
     *            the key type for the map.
     * @param <V>
     *            the value type for the map.
     * @return a new parameterized HashMap instance.
     */
    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<K, V>();
    }

    /**
     * Creates a new parameterized <code>HashMap</code> with a specified initial
     * capacity. * @param <K> the key type for the map.
     * 
     * @param <V>
     *            the value type for the map.
     * @param initCapacity
     *            the number of 'buckets' to create for the <code>HashMap</code>
     * @return a new parametrized HashMap instance with the specified initial
     *         capacity.
     */
    public static <K, V> HashMap<K, V> newHashMap(final int initCapacity) {
        return new HashMap<K, V>(initCapacity);
    }

    /**
     * Creates a new parameterized <code>ConcurrentHashMap</code> instance.
     * 
     * @param <K>
     *            the key type for the map.
     * @param <V>
     *            the value type for the map.
     * @return a new parameterized <code>ConcurrentHashMap</code> instance.
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() {
        return new ConcurrentHashMap<K, V>();
    }

}
