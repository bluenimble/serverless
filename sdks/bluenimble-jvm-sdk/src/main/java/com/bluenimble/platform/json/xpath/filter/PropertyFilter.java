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
import java.util.regex.Pattern;

import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.xpath.JsonUtil;

/**
 * Created by IntelliJ IDEA.
 * User: kallestenflo
 * Date: 2/2/11
 * Time: 2:32 PM
 */
public class PropertyFilter extends JsonPathFilterBase {

    public final static Pattern PATTERN = Pattern.compile("(.*)|\\['(.*?)'\\]");


    private final String pathFragment;

    public PropertyFilter(String pathFragment) {
        this.pathFragment = pathFragment;
    }

    @Override
    public FilterOutput apply(FilterOutput filter) {

    	List<Object> result = new JsonArray ();

        if (filter.isList()) {
            for (Object current : filter.getResultAsList ()) {
                if (JsonUtil.toMap (current).containsKey (pathFragment)) {
                    Object o = JsonUtil.toMap (current).get (pathFragment);
                    if (JsonUtil.isList (o)) {
                        result.addAll (JsonUtil.toList (o));

                    } else {
                        result.add(JsonUtil.toMap(current).get(pathFragment));
                    }
                }
            }
            return new FilterOutput(result);
        } else {
            Object mapValue = null;
            if (JsonUtil.toMap(filter.getResult()).containsKey(pathFragment)) {
                mapValue = JsonUtil.toMap(filter.getResult()).get(pathFragment);
            }
            return new FilterOutput(mapValue);
        }
    }
}
