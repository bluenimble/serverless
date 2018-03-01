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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.xpath.JsonUtil;

/**
 * Created by IntelliJ IDEA.
 * User: kallestenflo
 * Date: 2/15/11
 * Time: 8:23 PM
 */
public class ListPropertyFilter extends JsonPathFilterBase {

    public static final Pattern PATTERN = Pattern.compile("\\[\\s?\\?\\s?\\(\\s?@\\.(\\w+)\\s?\\)\\s?\\]");  //[?(@.title)]

    private final String pathFragment;

    public ListPropertyFilter(String pathFragment) {
        this.pathFragment = pathFragment;
    }

    @Override
    public FilterOutput apply(FilterOutput filterItems) {

    	List<Object> result = new JsonArray ();

        String prop = getFilterProperty();

        for (Object item : filterItems.getResultAsList()) {

            if (JsonUtil.isMap(item)) {
                if (JsonUtil.toMap(item).containsKey(prop)) {
                    result.add(item);
                }
            }
        }
        return new FilterOutput(result);
    }


    private String getFilterProperty() {
        Matcher matcher = PATTERN.matcher(pathFragment);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("invalid list filter property");
    }
}
