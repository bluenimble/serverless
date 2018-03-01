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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.xpath.JsonUtil;
import com.bluenimble.platform.json.xpath.eval.ExpressionEvaluator;

public class ListEvalFilter extends JsonPathFilterBase {

    public static final Pattern PATTERN = Pattern.compile("\\[\\s?\\?\\s?\\(\\s?@.(\\w+)\\s?([=<>]+)\\s?(.*)\\s?\\)\\s?\\]");    //[?( @.title< 'ko')]

    private final String pathFragment;

    public ListEvalFilter(String pathFragment) {
        this.pathFragment = pathFragment;
    }


    @Override
    public FilterOutput apply (FilterOutput filterItems) {

    	List<Object> result = new JsonArray ();

        for (Object item : filterItems.getResultAsList ()) {
            if (isMatch (item)) {
                result.add (item);
            }
        }
        return new FilterOutput (result);
    }

    @SuppressWarnings({ "rawtypes" })
	private boolean isMatch (Object check) {
        Matcher matcher = PATTERN.matcher(pathFragment);

        if (matcher.matches ()) {
            String property = matcher.group(1);
            String operator = matcher.group(2);
            String expected = matcher.group(3);

            if (!JsonUtil.isMap (check)) {
                return false;
            }
            Map obj = JsonUtil.toMap (check);

            if (!obj.containsKey(property)) {
                return false;
            }

            Object propertyValue = obj.get (property);

            if (JsonUtil.isContainer (propertyValue)) {
                return false;
            }

            return ExpressionEvaluator.eval (propertyValue, operator, expected);

        }
        return false;
    }
}
