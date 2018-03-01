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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bluenimble.platform.json.JsonArray;

/**
 * Created by IntelliJ IDEA.
 * User: kallestenflo
 * Date: 2/15/11
 * Time: 8:20 PM
 */
public class ListFrontFilter extends JsonPathFilterBase {


    public static final Pattern PATTERN = Pattern.compile("\\[\\s?:(\\d+)\\s?\\]");               //[ :2 ]

    private final String pathFragment;

    public ListFrontFilter(String pathFragment) {
        this.pathFragment = pathFragment;
    }

    @Override
    public FilterOutput apply (FilterOutput filterItems) {

    	List<Object> result = new JsonArray ();

        Integer[] index = getListPullIndex();
        for (int i : index) {
            if (indexIsInRange(filterItems.getResultAsList(), i)) {
                result.add(filterItems.getResultAsList().get(i));
            }
        }
        return new FilterOutput(result);
    }


    private Integer[] getListPullIndex() {
        Matcher matcher = PATTERN.matcher(pathFragment);
        if (matcher.matches()) {

            int pullCount = Integer.parseInt(matcher.group(1));

            List<Integer> result = new LinkedList<Integer>();

            for (int y = 0; y < pullCount; y++) {
                result.add(y);
            }
            return result.toArray(new Integer[0]);
        }
        throw new IllegalArgumentException("invalid list index");
    }

    @SuppressWarnings({ "rawtypes" })
	private boolean indexIsInRange (List list, int index) {
        if (index < 0) {
            return false;
        } else if (index > list.size() - 1) {
            return false;
        } else {
            return true;
        }
    }
}
