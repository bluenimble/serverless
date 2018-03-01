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
package com.bluenimble.platform.regex;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WildcardMatcher implements StringMatcher {

    // the pattern for finding $1 $2 etc
    private static Pattern variablePattern = Pattern.compile("(?<!\\\\)\\$([0-9])");
    private static Pattern escapedVariablePattern = Pattern.compile("\\\\(\\$[0-9])");

    private int[] 				pattern;
    private String 				match;
    private Map<String, String> resultMap = new HashMap<String, String> ();
    private boolean 			found = false;

    public WildcardMatcher (int [] pattern, String match) {
        this.pattern = pattern;
        this.match = match;
    }

    @Override
    public boolean find () {
        return WildcardCompiler.match (resultMap, match, pattern);
    }

    public String replaceAll (String subjectOfReplacement) {
        find();

        int lastCondMatcherGroupCount = this.groupCount();

        Matcher variableMatcher = variablePattern.matcher(subjectOfReplacement);

        StringBuffer sb = new StringBuffer();

        while (variableMatcher.find()) {
            int groupCount = variableMatcher.groupCount();
            if (groupCount < 1) {
                continue;
            }
            String varStr = variableMatcher.group(1);

            boolean validVariable = false;
            int varInt = 0;
            try {
                varInt = Integer.parseInt(varStr);
                if (varInt <= lastCondMatcherGroupCount) {
                    validVariable = true;
                }
            } catch (NumberFormatException nfe) {
                // Ignore
            }
            String conditionMatch = "";
            if (validVariable) {
                conditionMatch = this.group(varInt);
            }
            if (conditionMatch.contains("$")) {
                // ensure any remaining $'s in the matched string are escaped before appending the replacement
                conditionMatch = conditionMatch.replace("$", "\\$");
            }
            variableMatcher.appendReplacement(sb, conditionMatch);
        }
        variableMatcher.appendTail(sb);
        String result = sb.toString();

        Matcher escapedVariableMatcher = escapedVariablePattern.matcher(result);
        result = escapedVariableMatcher.replaceAll("$1");

        return result;
    }

    @Override
    public int groupCount () {
        if (resultMap == null) 
        	return 0;
        return resultMap.size () == 0 ? 0 : resultMap.size() - 1;
    }

    @Override
    public String group (int groupId) {
        if (resultMap == null) return null;
        return resultMap.get (String.valueOf (groupId));
    }

    @Override
	public int end () {
		if(found) {
			return match.length();
		}
		return -1;
	}

    @Override
	public void reset () {
	}

    @Override
	public int start () {
		if (found) {
			return 0;
		}
		return -1;
	}
	
    @Override
	public boolean isMultipleMatchingSupported() {
		return false;
	}

	public String getMatchedString () {
		return match;
	}
	
	public static void main (String[] args) {
		WildcardMatcher matcher = new WildcardMatcher (WildcardCompiler.compile ("/invoices/teta/beta/*"), "/invoices/teta/beta/v7126-5128-5129-5130-EBA1-1FAAA-0-16961-0-16961-85-68-0-1-0-^%0170002%0120090226%01788914%0189701%01BUSY+SUPPLY%012%2F10NET30%01579.420000%010%0116961");
		System.out.println (matcher.find ());
	}

}
