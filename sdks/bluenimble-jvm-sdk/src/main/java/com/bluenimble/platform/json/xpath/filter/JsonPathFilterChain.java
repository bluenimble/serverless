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

import com.bluenimble.platform.json.xpath.InvalidPathException;

/**
 * User: kallestenflo
 * Date: 2/2/11
 * Time: 2:00 PM
 */
public class JsonPathFilterChain {

    private List<JsonPathFilterBase> filters;

    public JsonPathFilterChain(List<String> pathFragments) {
        filters = configureFilters(pathFragments);
    }

    private List<JsonPathFilterBase> configureFilters(List<String> pathFragments) {

        List<JsonPathFilterBase> configured = new LinkedList<JsonPathFilterBase>();

        for (String pathFragment : pathFragments) {
            configured.add(JsonPathFilterFactory.createFilter(pathFragment));
        }
        return configured;
    }

    public FilterOutput filter(Object root) {

        FilterOutput out = new FilterOutput(root);

        for (JsonPathFilterBase filter : filters) {
            if (filter == null) {
                throw new InvalidPathException();
            }
            if(out.getResult() == null){
                return null;
            }
            out = filter.apply(out);
        }

        return out;
    }
}
