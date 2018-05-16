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
package com.bluenimble.platform.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import com.bluenimble.platform.IOUtils;

public class JsonParser implements Serializable {

	private static final long serialVersionUID = 1434415316242903735L;

    private int position;

    private String source;


    public JsonParser (String source) {
        this.position = 0;
        this.source = source;
    }
    
    public JsonParser (InputStream stream) throws JsonException {
	    this.position = 0;
    	try {
    	    this.source = IOUtils.toString (stream);
		} catch (IOException e) {
			throw new JsonException (e, e.getMessage ());
		} 
    }
    
   public static JsonEntity parse (String s) throws JsonException {
    	if (s == null) {
    		return null;
    	}
        s = s.trim ();
        if (s.startsWith ("{")) {
        	return new JsonObject (new JsonParser (s));
        } else if (s.startsWith ("[")) {
        	return new JsonArray (new JsonParser (s));
        } else {
        	throw new JsonException ("invalid json string");
        }
    }


    /**
     * Back up one character. This provides a sort of lookahead capability,
     * so that you can test for a digit or letter before attempting to parse
     * the next number or identifier.
     */
    public void back () {
        if (this.position > 0) {
            this.position -= 1;
        }
    }

    /**
     * Get the hex value of a character (base16).
     * @param c A character between '0' and '9' or between 'A' and 'F' or
     * between 'a' and 'f'.
     * @return  An int between 0 and 15, or -1 if c was not a hex digit.
     */
    public static int dehexchar (char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - ('A' - 10);
        }
        if (c >= 'a' && c <= 'f') {
            return c - ('a' - 10);
        }
        return -1;
    }


    /**
     * Determine if the source string still contains characters that next()
     * can consume.
     * @return true if not yet at the end of the source.
     */
    public boolean more() {
        return this.position < this.source.length();
    }


    /**
     * Get the next character in the source string.
     *
     * @return The next character, or 0 if past the end of the source string.
     */
    public char next() {
        if (more()) {
            char c = this.source.charAt(this.position);
            this.position += 1;
            return c;
        }
        return 0;
    }


    /**
     * Consume the next character, and check that it matches a specified
     * character.
     * @param c The character to match.
     * @return The character.
     * @throws JsonException if the character does not match.
     */
    public char next(char c) throws JsonException {
        char n = next();
        if (n != c) {
            throw syntaxError("Expected '" + c + "' and instead saw '" +
                    n + "'");
        }
        return n;
    }


    /**
     * Get the next n characters.
     *
     * @param n     The number of characters to take.
     * @return      A string of n characters.
     * @throws JsonException
     *   Substring bounds error if there are not
     *   n characters remaining in the source string.
     */
     public String next(int n) throws JsonException {
         int i = this.position;
         int j = i + n;
         if (j >= this.source.length()) {
            throw syntaxError("Substring bounds error");
         }
         this.position += n;
         return this.source.substring(i, j);
     }


    public char nextClean() throws JsonException {
        for (;;) {
            char c = next();
            if (c == '/') {
                switch (next()) {
                case '/':
                    do {
                        c = next();
                    } while (c != '\n' && c != '\r' && c != 0);
                    break;
                case '*':
                    for (;;) {
                        c = next();
                        if (c == 0) {
                            throw syntaxError("Unclosed comment");
                        }
                        if (c == '*') {
                            if (next() == '/') {
                                break;
                            }
                            back();
                        }
                    }
                    break;
                default:
                    back();
                    return '/';
                }
            } else if (c == '#') {
                do {
                    c = next();
                } while (c != '\n' && c != '\r' && c != 0);
            } else if (c == 0 || c > ' ') {
                return c;
            }
        }
    }


    /**
     * Return the characters up to the next close quote character.
     * Backslash processing is done. The formal JSON format does not
     * allow strings in single quotes, but an implementation is allowed to
     * accept them.
     * @param quote The quoting character, either
     *      <code>"</code>&nbsp;<small>(double quote)</small> or
     *      <code>'</code>&nbsp;<small>(single quote)</small>.
     * @return      A String.
     * @throws JsonException Unterminated string.
     */
    public String nextString(char quote) throws JsonException {
        char c;
        StringBuilder sb = new StringBuilder ();
        for (;;) {
            c = next();
            switch (c) {
            case 0:
            case '\n':
            case '\r':
                throw syntaxError("Unterminated string");
            case '\\':
                c = next();
                switch (c) {
                case 'b':
                    sb.append('\b');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 'u':
                    sb.append((char)Integer.parseInt(next(4), 16));
                    break;
                case 'x' :
                    sb.append((char) Integer.parseInt(next(2), 16));
                    break;
                default:
                    sb.append(c);
                }
                break;
            default:
                if (c == quote) {
        			String s = sb.toString ();
        			sb.setLength (0);
        			sb = null;
        			return s;
                }
                sb.append(c);
            }
        }
    }



    /**
     * Get the text up but not including the specified character or the
     * end of line, whichever comes first.
     * @param  d A delimiter character.
     * @return   A string.
     */
    public String nextTo(char d) {
    	StringBuilder sb = new StringBuilder();
        for (;;) {
            char c = next();
            if (c == d || c == 0 || c == '\n' || c == '\r') {
                if (c != 0) {
                    back();
                }
    			String s = sb.toString ();
    			sb.setLength (0);
    			sb = null;
    			return s.trim();
            }
            sb.append(c);
        }
    }


    /**
     * Get the text up but not including one of the specified delimeter
     * characters or the end of line, whichever comes first.
     * @param delimiters A set of delimiter characters.
     * @return A string, trimmed.
     */
    public String nextTo(String delimiters) {
        char c;
        StringBuilder sb = new StringBuilder();
        for (;;) {
            c = next();
            if (delimiters.indexOf(c) >= 0 || c == 0 ||
                    c == '\n' || c == '\r') {
                if (c != 0) {
                    back();
                }
    			String s = sb.toString ();
    			sb.setLength (0);
    			sb = null;
    			return s.trim();
           }
            sb.append(c);
        }
    }


    /**
     * Get the next value. The value can be a Boolean, Double, Integer,
     * JsonArray, JsonObject, Long, or String, or the JsonObject.NULL object.
     * @throws JsonException If syntax error.
     *
     * @return An object.
     */
    public Object nextValue() throws JsonException {
        char c = nextClean();
        String s;

        switch (c) {
            case '"':
            case '\'':
                return nextString(c);
            case '{':
                back();
                return new JsonObject (this);
            case '[':
            case '(':
                back();
                return new JsonArray (this);
        }

        /*
         * Handle unquoted text. This could be the values true, false, or
         * null, or it can be a number. An implementation (such as this one)
         * is allowed to also accept non-standard forms.
         *
         * Accumulate characters until we reach the end of the text or a
         * formatting character.
         */

        StringBuilder sb = new StringBuilder();
        char b = c;
        while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
            sb.append(c);
            c = next();
        }
        back();

        /*
         * If it is true, false, or null, return the proper value.
         */

        s = sb.toString ().trim();
		sb.setLength (0);
		sb = null;

        if (s.equals("")) {
            throw syntaxError("Missing value");
        }
        if (s.equalsIgnoreCase(JsonObject.TRUE)) {
            return Boolean.TRUE;
        }
        if (s.equalsIgnoreCase(JsonObject.FALSE)) {
            return Boolean.FALSE;
        }
        if (s.equalsIgnoreCase (JsonObject.NULL_VALUE)) {
            return JsonObject.NULL;
        }

        /*
         * If it might be a number, try converting it. We support the 0- and 0x-
         * conventions. If a number cannot be produced, then the value will just
         * be a string. Note that the 0-, 0x-, plus, and implied string
         * conventions are non-standard. A JSON parser is free to accept
         * non-JSON forms as long as it accepts all correct JSON forms.
         */
        if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+') {
            if (b == '0') {
                if (s.length() > 2 &&
                        (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
                    try {
                        return new Integer(Integer.parseInt(s.substring(2),
                                16));
                    } catch (Exception e) {
                        /* Ignore the error */
                    }
                } else {
                    try {
                        return new Integer(Integer.parseInt(s, 8));
                    } catch (Exception e) {
                        /* Ignore the error */
                    }
                }
            }
            try {
                return new Integer(s);
            } catch (Exception e) {
                try {
                    return new Long(s);
                } catch (Exception f) {
                    try {
                        return new Double(s);
                    }  catch (Exception g) {
                        return s;
                    }
                }
            }
        }
        return s;
    }


    /**
     * Skip characters until the next character is the requested character.
     * If the requested character is not found, no characters are skipped.
     * @param to A character to skip to.
     * @return The requested character, or zero if the requested character
     * is not found.
     */
    public char skipTo(char to) {
        char c;
        int index = this.position;
        do {
            c = next();
            if (c == 0) {
                this.position = index;
                return c;
            }
        } while (c != to);
        back();
        return c;
    }


    /**
     * Skip characters until past the requested string.
     * If it is not found, we are left at the end of the source.
     * @param to A string to skip past.
     */
    public boolean skipPast(String to) {
        this.position = this.source.indexOf(to, this.position);
        if (this.position < 0) {
            this.position = this.source.length();
            return false;
        }
        this.position += to.length();
        return true;

    }


    /**
     * Make a JsonException to signal a syntax error.
     *
     * @param message The error message.
     * @return  A JsonException object, suitable for throwing
     */
    public JsonException syntaxError(String message) {
        return new JsonException(message + toString());
    }


    /**
     * Make a printable string of this JsonTokener.
     *
     * @return " at character [this.position] of [this.mySource]"
     */
    public String toString () {
        return " at character " + this.position + " of " + this.source;
    }
}