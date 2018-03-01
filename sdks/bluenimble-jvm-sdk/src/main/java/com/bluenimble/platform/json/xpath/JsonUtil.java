/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluenimble.platform.json.xpath;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class JsonUtil {
    /**
     * checks if object is <code>instanceof</code> <code>java.util.List</code> or <code>java.util.Map</code>
     *
     * @param obj object to check
     * @return true if List or Map
     */
    public static boolean isContainer(Object obj) {
        return (isList(obj) || isMap(obj));
    }

    /**
     * checks if object is <code>instanceof</code> <code>java.util.List</code>
     *
     * @param obj object to check
     * @return true if List
     */
    public static boolean isList(Object obj) {
        return (obj instanceof List);
    }

    /**
     * checks if object is <code>instanceof</code> <code>java.util.Map</code>
     *
     * @param obj object to check
     * @return true if Map
     */
    public static boolean isMap(Object obj) {
        return (obj instanceof Map);
    }

    /**
     * converts casts to <code>java.util.List</code>
     *
     * @param obj
     * @return the list
     */
    @SuppressWarnings("rawtypes")
	public static List<Object> toList(Object obj) {
        return (List) obj;
    }

    /**
     * converts casts to <code>java.util.Map</code>
     *
     * @param obj
     * @return the Map
     */
    @SuppressWarnings("rawtypes")
	public static Map<Object, Object> toMap (Object obj) {
        return (Map) obj;
    }
}
