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

/**
 * User: kallestenflo
 * Date: 2/2/11
 * Time: 2:03 PM
 */
public class JsonPathFilterFactory {

    public static JsonPathFilterBase createFilter(String pathFragment) {

        if (RootFilter.PATTERN.matcher(pathFragment).matches()) {
            return new RootFilter();
        } else if (ListIndexFilter.PATTERN.matcher(pathFragment).matches()) {
            return new ListIndexFilter(pathFragment);
        } else if (ListFrontFilter.PATTERN.matcher(pathFragment).matches()) {
            return new ListFrontFilter(pathFragment);
        } else if (ListWildcardFilter.PATTERN.matcher(pathFragment).matches()) {
            return new ListWildcardFilter();
        } else if (ListTailFilter.PATTERN.matcher(pathFragment).matches()) {
            return new ListTailFilter(pathFragment);
        } else if (ListPropertyFilter.PATTERN.matcher(pathFragment).matches()) {
            return new ListPropertyFilter(pathFragment);
        } else if (ListEvalFilter.PATTERN.matcher(pathFragment).matches()) {
            return new ListEvalFilter(pathFragment);
        } else if (TraverseFilter.PATTERN.matcher(pathFragment).matches()) {
            return new TraverseFilter();
        } else if (WildcardPropertyFilter.PATTERN.matcher(pathFragment).matches()) {
            return new WildcardPropertyFilter();
        } else if (PropertyFilter.PATTERN.matcher(pathFragment).matches()) {
            return new PropertyFilter(pathFragment);
        }
        return null;

    }

}
