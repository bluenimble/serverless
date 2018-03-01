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

import java.io.IOException;

import com.bluenimble.platform.json.JsonException;
import com.bluenimble.platform.json.JsonParser;
import com.bluenimble.platform.json.xpath.filter.FilterOutput;
import com.bluenimble.platform.json.xpath.filter.JsonPathFilterChain;

public class JsonPath {

    public final static int STRICT_MODE = 0;
    public final static int SLACK_MODE = -1;

    private static int mode = SLACK_MODE;

    private JsonPathFilterChain filters;

    public static int getMode (){
        return mode;
    }

    /**
     * Creates a new JsonPath.
     *
     * @param jsonPath the path statement
     */
    private JsonPath (String jsonPath) {
        if (jsonPath == null ||
                jsonPath.trim().isEmpty() ||
                jsonPath.matches("new ") ||
                jsonPath.matches("[^\\?\\+\\=\\-\\*\\/\\!]\\(")) {

            throw new InvalidPathException("Invalid path");
        }
        this.filters = new JsonPathFilterChain (PathUtil.splitPath (jsonPath));
    }

    /**
     * Applies this json path to the provided object
     *
     * @param json a json Object
     * @param <T>
     * @return list of objects matched by the given path
     */
    @SuppressWarnings("unchecked")
	public <T> T read (Object json) {
        FilterOutput filterOutput = filters.filter (json);

        if (filterOutput == null || filterOutput.getResult() == null) {
            return null;
        }

        return (T) filterOutput.getResult ();
    }

    /**
     * Applies this json path to the provided object
     *
     * @param json a json string
     * @param <T>
     * @return list of objects matched by the given path
     * @throws JsonException 
     * @throws IOException 
     */
    @SuppressWarnings("unchecked")
	public <T> T read (String json) throws JsonException {
        return (T) read (JsonParser.parse (json));
    }

    /**
     * Compiles a JsonPath from the given string
     *
     * @param jsonPath to compile
     * @return compiled JsonPath
     */
    public static JsonPath compile (String jsonPath) {
        return new JsonPath (jsonPath);
    }

    /**
     * Creates a new JsonPath and applies it to the provided Json string
     *
     * @param json     a json string
     * @param jsonPath the json path
     * @param <T>
     * @return list of objects matched by the given path
     * @throws IOException 
     * @throws JsonException 
     */
    @SuppressWarnings("unchecked")
	public static <T> T read (String json, String jsonPath) throws JsonException {
        return (T) compile (jsonPath).read (json);
    }

    /**
     * Creates a new JsonPath and applies it to the provided Json object
     *
     * @param json     a json object
     * @param jsonPath the json path
     * @param <T>
     * @return list of objects matched by the given path
     */
    @SuppressWarnings("unchecked")
	public static <T> T read (Object json, String jsonPath) {
        return (T) compile (jsonPath).read(json);
    }
}
