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

import static java.lang.String.format;

import java.util.List;

/**
 * User: kalle stenflo
 * Date: 2/9/11
 * Time: 12:28 PM
 */
public class FilterOutput {

    private final Object result;

    public FilterOutput (Object result) {
        this.result = result;
    }


    public boolean isList (){

        return (result instanceof List);

    }

    public Object getResult () {
        return result;
    }
    @SuppressWarnings("unchecked")
	public List<Object> getResultAsList () {
        if(!isList()){
            throw new RuntimeException(format("Can not convert a %s to a %s", result.getClass().getName(), List.class.getName()));
        }
        return (List<Object>)result;
    }



}
