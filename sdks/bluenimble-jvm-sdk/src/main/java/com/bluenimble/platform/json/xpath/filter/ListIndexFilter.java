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
package com.bluenimble.platform.json.xpath.filter;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.bluenimble.platform.json.JsonArray;

/**
 * Created by IntelliJ IDEA.
 * User: kallestenflo
 * Date: 2/15/11
 * Time: 8:02 PM
 */
public class ListIndexFilter extends JsonPathFilterBase {

    public static final Pattern PATTERN = Pattern.compile("\\[(\\s?\\d+\\s?,?)+\\]");               //[1] OR [1,2,3]

    private final String pathFragment;

    public ListIndexFilter (String pathFragment) {
        this.pathFragment = pathFragment;
    }

    @Override
    public FilterOutput apply(FilterOutput filterItems) {

        Object result = null;

        Integer[] index = getArrayIndex();
        if (index.length > 1) {
        	List<Object> tmp = new JsonArray ();
            for (int i : index) {
                if (indexIsInRange(filterItems.getResultAsList(), i)) {
                    tmp.add(filterItems.getResultAsList().get(i));
                }
            }
            result = tmp;
        } else {
            if (indexIsInRange(filterItems.getResultAsList(), index[0])) {
                result = filterItems.getResultAsList().get(index[0]);
            }
        }
        return new FilterOutput (result);
    }

    @SuppressWarnings("rawtypes")
	private boolean indexIsInRange (List list, int index) {
        if (index < 0) {
            return false;
        } else if (index > list.size() - 1) {
            return false;
        } else {
            return true;
        }
    }


    private Integer[] getArrayIndex () {

        String prepared = pathFragment.replaceAll(" ", "");
        prepared = prepared.substring(1, prepared.length() - 1);

        List<Integer> index = new LinkedList<Integer>();

        String[] split = prepared.split(",");

        for (String s : split) {
            index.add(Integer.parseInt(s));
        }
        return index.toArray(new Integer[0]);
    }
}
