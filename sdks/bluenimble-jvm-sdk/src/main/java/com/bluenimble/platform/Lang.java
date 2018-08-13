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
package com.bluenimble.platform;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.regex.WildcardCompiler;
import com.bluenimble.platform.regex.WildcardMatcher;
import com.bluenimble.platform.scripting.ScriptingEngineException;

import jdk.nashorn.api.scripting.NashornException;

@SuppressWarnings("restriction")
public class Lang {
	
	public static final Null		Null 					= new Null ();
	
	private static final String 	ExpStart				= "{";
	private static final String 	ExpEnd					= "}";
	
	public static final String 		SNULL 					= "_X_";

	public static final String 		BLANK 					= "";
	public static final String 		SPACE 					= " ";
	public static final String 		PLUS 					= "+";
	public static final String 		STAR 					= "*";
	public static final String 		COLON 					= ":";
	public static final String 		SEMICOLON 				= ";";
	public static final String 		COMMA 					= ",";
	public static final String 		SLASH					= "/";
	public static final String 		BACKSLASH				= "\\";
	public static final String 		PIPE					= "|";
	public static final String 		DOT 					= ".";
	public static final String 		EQUALS 					= "=";
	public static final String 		DASH 					= "-";
	public static final String 		UNDERSCORE 				= "_";
	public static final String 		ENDLN 					= "\n";
	public static final String 		TAB 					= "\t";
	public static final String 		AMP 					= "&";
	public static final String 		QMARK 					= "?";
	public static final String 		XMARK					= "!";
	public static final String 		PERCENT					= "%";
	public static final String 		DOLLAR					= "$";
	public static final String 		SHARP					= "#";
	public static final String 		LESS					= "<";
	public static final String 		GREATER					= ">";
	public static final String 		TILDE 					= "~";

	public static final String 		NULL 					= "null";
	public static final String 		URL_ACCESSOR 			= "://";

	public static final String 		DATA 					= "\"Data\"";

	public static final String 		OBJECT_OPEN				= "{";
	public static final String 		OBJECT_CLOSE			= "}";
	public static final String 		ARRAY_OPEN				= "[";
	public static final String 		ARRAY_CLOSE				= "]";
	public static final String 		PARENTH_OPEN			= "(";
	public static final String 		PARENTH_CLOSE			= ")";

	public static final String 		QUOTE					= "\"";
	public static final String 		APOS					= "'";
	public static final String 		QUESTION				= "?";
	
	public static final String 	 	FALSE 					= "false";
	public static final String 	 	TRUE 					= "true";

	public static final String 		EMTPY_OBJECT			= "{}";
	public static final String 		EMTPY_ARRAY				= "[]";
	public static final String 		EMTPY_DATA 				= "{\"items\": []}";

	public static final byte [] 	B_JSON_OBJECT_OPEN		= OBJECT_OPEN.getBytes ();
	public static final byte [] 	B_JSON_OBJECT_CLOSE		= OBJECT_CLOSE.getBytes ();
	public static final byte [] 	B_JSON_ARRAY_OPEN		= ARRAY_OPEN.getBytes ();
	public static final byte [] 	B_JSON_ARRAY_CLOSE		= ARRAY_CLOSE.getBytes ();

	public static final byte []		B_QUOT					= QUOTE.getBytes ();
	public static final byte [] 	B_COLON 				= COLON.getBytes ();
	public static final byte [] 	B_COMMA 				= COMMA.getBytes ();
	public static final byte [] 	B_JSON_DATA 			= DATA.getBytes ();

	public static final byte [] 	B_EMTPY_OBJECT			= EMTPY_OBJECT.getBytes ();
	public static final byte [] 	B_EMTPY_ARRAY			= EMTPY_ARRAY.getBytes ();
	public static final byte [] 	B_EMTPY_DATA 			= EMTPY_DATA.getBytes ();
	
	public static final String 		API_ENCODING			= "paas.encoding";
	public static final String 		API_DATE_FORMAT			= "paas.dateFormat";
	public static final String 		API_DEBUG_MODE			= "paas.debugMode";
	
	public static final String		DEFAULT_DATE_FORMAT		= "yyyy-MM-dd";
	public static final String		DEFAULT_DATE_TIME_FORMAT= "dd-MM-yyyy HH:mm:ss";

	public static final String		UTC_DATE_FORMAT			= "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	public static final TimeZone	UTC_TZ					= TimeZone.getTimeZone ("GMT");
	
	public static final Set<String> BooleanValues				= new HashSet<String> ();
	static {
		BooleanValues.add ("1"); BooleanValues.add ("y"); BooleanValues.add ("on"); BooleanValues.add ("yes"); BooleanValues.add ("true");
		BooleanValues.add ("0"); BooleanValues.add ("n"); BooleanValues.add ("off"); BooleanValues.add ("no"); BooleanValues.add ("false");
	}
	public static final Set<String> TrueValues				= new HashSet<String> ();
	static {
		TrueValues.add ("1"); TrueValues.add ("y"); TrueValues.add ("on"); TrueValues.add ("yes"); TrueValues.add ("true");
	}

	public static final Set<String> FalseValues				= new HashSet<String> ();
	static {
		FalseValues.add ("0"); FalseValues.add ("n"); FalseValues.add ("off"); FalseValues.add ("no"); FalseValues.add ("false");
	}
	
	//private static final String HMACSHA1 = "HMACSHA1";

	private static final String RandDigits = 
        	"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
	
	private static final String RandDigitsX = 
			RandDigits + "-+_/";
	
	private static final String RandNumbers = "123456789";
	
    private static final char[] DIGITS_LOWER =
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Used to build output as Hex
     */
    private static final char[] DIGITS_UPPER =
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    
    private static final String NashornScript 		= "Script$Recompilation$";
    private static final String PlatformPath 		= "platform/core/";
    private static final String ApiResourcePath 	= "resoures/";
    
    private static final String EvalFile 			= "<eval>";
    private static final String UndefinedMessage 	= "undefined";
    private static final String TypeError 			= "TypeError: ";

   public static boolean isNullOrEmpty (String str) {
		if (str == null) {
			return true;
		}
		return str.trim ().isEmpty ();
	}
	
    public static String rand () {
    	return UUID.randomUUID ().toString ();
    }
    
    public static String pin (int length) {
    	return UUID (RandNumbers, length);
    }
    
    public static String UUID (int length) {
    	return UUID (RandDigits, length);
    }
    
    public static String UUID (String rd, int length) {
        if (length <= 0) {
            length = 10;
        }
        Random rand = new Random (System.nanoTime ());
        StringBuilder sb = new StringBuilder ();
        for (int i = 0; i < length; i++) {
            int pos = rand.nextInt (rd.length ());
            sb.append (String.valueOf (rd.charAt (pos)));
        }
        String s = sb.toString ();
        sb.setLength (0);
        return s;
    }
    
	public static String [] split (String str, String separator, boolean trim) {
		if (str == null) {
			return null;
		}
		if (separator == null || str.indexOf (separator) < 0) {
			return new String [] { trim ? str.trim () : str};
		}

		StringTokenizer st = new StringTokenizer (str, separator);
		
		String [] values = new String [st.countTokens ()];
		
		int pos = 0;
		while (st.hasMoreTokens ()) {
			values [pos++] = (trim ? st.nextToken ().trim () : st.nextToken ()) ;
		}
		
		return values;
	}

	public static String [] split (String str, String separator) {
		return split (str, separator, false);
	}    
    
    public static String join (String [] arr) {
    	return join (arr, COMMA);
    }
    
    public static String join (String [] arr, String sep) {
    	
    	if (arr == null || arr.length == 0) {
    		return null;
    	}
    	
    	StringBuilder sb = new StringBuilder ();
    	
    	for (int i = 0; i < arr.length; i++) {
    		sb.append (arr [i]);
    		if (i < (arr.length - 1)) {
    			sb.append (sep);
    		}
    	}
    	
    	String s = sb.toString ();
    	sb.setLength (0);
    	
    	return s;
    	
    }
    
    public static String join (List<String> arr) {
    	return join (arr, COMMA);
    }
    
    public static String join (List<String> arr, String sep) {
    	
    	if (arr == null || arr.isEmpty ()) {
    		return null;
    	}
    	
    	StringBuilder sb = new StringBuilder ();
    	
    	for (int i = 0; i < arr.size (); i++) {
    		sb.append (arr.get (i));
    		if (i < (arr.size () - 1)) {
    			sb.append (sep);
    		}
    	}
    	
    	String s = sb.toString ();
    	sb.setLength (0);
    	
    	return s;
    	
    }
    
    public static String [] moveLeft (String [] resource, int step) {
    	if (step < 1) {
    		return resource;
    	}
    	if (resource == null || step >= resource.length) {
    		return null;
    	}
    	
    	String [] result = new String [resource.length - step];
    	for (int i = 0; i < result.length; i++) {
    		result [i] = resource [i + step];
    	}
    	return result;
    }
    
    public static String [] moveRight (String [] resource, int step) {
    	if (step < 1) {
    		return resource;
    	}
    	if (resource == null || step >= resource.length) {
    		return null;
    	}
    	
    	String [] result = new String [resource.length - step];
    	for (int i = 0; i < result.length; i++) {
    		result [i] = resource [i];
    	}
    	return result;
    }
    
    public static String [] add (String [] a, String [] b) {
    	
    	if (a == null || a.length == 0) {
    		return b;
    	}
    	if (b == null || b.length == 0) {
    		return a;
    	}
    	
    	String [] sum = new String [a.length + b.length];
    	for (int i = 0; i < a.length; i++) {
    		sum [i] = a [i];
    	}
    	for (int i = 0; i < b.length; i++) {
    		sum [i + a.length] = b [i];
    	}
    	
    	return sum;
    	
    }
    
	public static boolean existsIn(String str, String[] searchIn) {
		return existsIn(str, searchIn, false);
	}
	
	public static boolean existsIn(String str, String[] searchIn, boolean ignoreCase) {
		boolean return_value = false;
		if (!ignoreCase) {
			for (int i = 0; i < searchIn.length; i++) {
				if (str.equals(searchIn[i])) {
					return_value = true;
					break;
				} else {
					return_value = false;
				}
			}
		} else {
			for (int i = 0; i < searchIn.length; i++) {
				if (str.equalsIgnoreCase(searchIn[i])) {
					return_value = true;
					break;
				} else {
					return_value = false;
				}
			}
		}
		return return_value;
	}

	public static String utc () {
		SimpleDateFormat formatter = new SimpleDateFormat (UTC_DATE_FORMAT);
		formatter.setTimeZone (UTC_TZ);
		return formatter.format (utcTime ());
	}

	public static Date utcTime () {
		return Calendar.getInstance (UTC_TZ).getTime ();
	}

    public static String md5 (byte [] data) {
        MessageDigest m;
		try {
			m = MessageDigest.getInstance ("MD5");
		} catch (NoSuchAlgorithmException e) {
			return new String (data);
		}
        m.update (data, 0, data.length);
        BigInteger i = new BigInteger (1, m.digest ());
        return String.format ("%1$032X", i);
    }
    
    public static String password (String pwd) {
    	byte [] bytes = null;
    	try {
			bytes = pwd.getBytes (Encodings.UTF8);
		} catch (UnsupportedEncodingException e) {
			bytes = pwd.getBytes ();
		}
    	return md5 (bytes);
    }
    
	public static Date toDate (String date, String format) throws ParseException {
		return toDate (date, format, (Locale)null);
	}
	
	public static Date toDate (String date, String format, Locale locale) throws ParseException {
        return toDate (date, format, locale, null);
    }

	public static Date toDate (String date, String format, TimeZone tz) throws ParseException {
        return toDate (date, format, null, tz);
    }

	public static Date toUTC (String date) throws ParseException {
        return toDate (date, UTC_DATE_FORMAT, UTC_TZ);
    }

	public static Date toDate (String date, String format, Locale locale, TimeZone tz) throws ParseException {
        SimpleDateFormat formatter = null;
        if (locale != null) {
        	formatter = new SimpleDateFormat (format, locale);
        } else {
        	formatter = new SimpleDateFormat (format);
        }
        formatter.setLenient (false);
        if (tz == null) {
        	tz = TimeZone.getDefault ();
        }
        formatter.setTimeZone (tz);
        return formatter.parse (date);
    }

	public static String toUTC (Date date) {
        return toString (date, UTC_DATE_FORMAT, UTC_TZ);
    }
	
	public static String toString (Date date, String format) {
        return toString (date, format, (Locale)null);
    }
	
	public static String toString (Date date, String format, TimeZone tz) {
        return toString (date, format, null, tz);
    }
	
	public static String toString (Date date, String format, Locale locale) {
        return toString (date, format, locale, null);
    }
	
	public static String toString (Date date, String format, Locale locale, TimeZone tz) {
        SimpleDateFormat formatter = null;
        if (locale != null) {
        	formatter = new SimpleDateFormat (format, locale);
        } else {
        	formatter = new SimpleDateFormat (format);
        }
        if (tz == null) {
        	tz = TimeZone.getDefault ();
        }
        formatter.setTimeZone (tz);
        return formatter.format (date);
    }
	
	public static Throwable getRootCause (Throwable th) {
		Throwable cause = th;
		while (true) {
			Throwable c = cause.getCause ();
			if (c == null) {
				return cause;
			}
			cause = c;
		}
	}
	
	public static void printTree (Throwable th) {
		Throwable cause = th;
		while (true) {
			System.out.println (cause.getClass ().getName () + " > " + cause.getMessage());
			Throwable c = cause.getCause ();
			if (c == null) {
				return;
			}
			cause = c;
		}
	}
	
	public static String toString (Throwable th) {
		if (th == null) {
			return null;
		}
		if (!isDebugMode ()) {
			String message = th.getMessage ();
			if (message == null) {
				StackTraceElement ste = th.getStackTrace () [0];
				message = 
					th.getClass ().getSimpleName () + Lang.GREATER + Lang.SPACE + 
					ste.getClassName () + ste.getMethodName () + ste.getLineNumber ();
			}
			return message;
		}
		StringWriter sw = new StringWriter ();
		PrintWriter pw = new PrintWriter (sw);
		th.printStackTrace (pw);
		
		return sw.toString ();
	}
	
	public static String [] toMessage (Throwable th) {
		if (th == null) {
			return null;
		}
		String [] arr = new String [2];
		
		String message = th.getMessage ();
		if (message == null) {
			StackTraceElement ste = th.getStackTrace () [0];
			message = 
				th.getClass ().getSimpleName () + Lang.GREATER + Lang.SPACE + 
				ste.getClassName () + Lang.SPACE + ", Method: " + ste.getMethodName () + ", Line: " + ste.getLineNumber ();
		}

		arr [0] = message;
		
		if (isDebugMode ()) {
			StringWriter sw = new StringWriter ();
			PrintWriter pw = new PrintWriter (sw);
			th.printStackTrace (pw);
			
			arr [1] = sw.toString ();
		} else {
			arr [1] = Lang.BLANK;
		}
		
		return arr;
	}
	
	public static JsonObject toError (Throwable th) {
		if (th == null) {
			return null;
		}
		
		Throwable first = th;
		
		th = Lang.getRootCause (th);

		String message = th.getMessage ();
		if (message == null) {
			StackTraceElement ste = th.getStackTrace () [0];
			message = 
				th.getClass ().getSimpleName () + Lang.GREATER + Lang.SPACE + 
				ste.getClassName () + Lang.SPACE + ", Method: " + ste.getMethodName () + ", Line: " + ste.getLineNumber ();
		}
		
		JsonObject error = new JsonObject ();
		error.set (ApiResponse.Error.Message, message);
		
		if (!Lang.isDebugMode ()) {
			return error;
		}
		
		JsonArray trace = new JsonArray ();
		error.set (ApiResponse.Error.Trace, trace);
		
		if (th instanceof NashornException) {
			if (UndefinedMessage.equals (message)) {
				error.set (ApiResponse.Error.Message, UndefinedMessage + " variable");
			} else if (message.startsWith (TypeError)) {
				error.set (ApiResponse.Error.Message, Lang.replace (message, TypeError, Lang.BLANK));
			}
			readNashornError (th, first, trace);
			return error;
		} 
		
		StackTraceElement [] stack = th.getStackTrace ();
		for (StackTraceElement line : stack) {
			JsonObject oLine = new JsonObject ();
			trace.add (oLine);
			
			oLine.set (ApiResponse.Error.Line, line.getLineNumber ());
			oLine.set (ApiResponse.Error.Function, line.getMethodName ());
			
			String className = line.getClassName ();

			int indeOfNashornScript = className.indexOf (NashornScript);
			if (indeOfNashornScript > 0) {
				String file = line.getFileName ();
				
				// if  the error is coming from the platform core scripts
				int indexOfPath = file.indexOf (PlatformPath); 
				if (indexOfPath <= 0) {
					indexOfPath = file.indexOf (ApiResourcePath); 
				}
				if (indexOfPath > 0) {
					file = file.substring (indexOfPath);
				}
				oLine.set (ApiResponse.Error.File, file);
				
				// break here
				break;
				
			} else {
				int indexOfDot = className.lastIndexOf (Lang.DOT); 
				if (indexOfDot > 0) {
					className = className.substring (indexOfDot + 1);
				}
				oLine.set (ApiResponse.Error.Clazz, className);
			}
			
		}
		
		return error;
	}
	
	public static String encode (String value) {
		try {
			return URLEncoder.encode (value, Encodings.UTF8);
		} catch (UnsupportedEncodingException e) {
			return value;
		}
	}
	
	public static String decode (String value) {
		try {
			return URLDecoder.decode (value, Encodings.UTF8);
		} catch (UnsupportedEncodingException e) {
			return value;
		}
	}
	
	public static String replace (String source, String os, String ns) {
        if (source == null) {
            return null;
        }
        if (os == null || ns == null) {
            return source;
        }
        int i = 0;
        // Make sure that oldString appears at least once before doing any processing.
        if ((i = source.indexOf(os, i)) >= 0) {
            // Use char []'s, as they are more efficient to deal with.
            char[] sourceArray = source.toCharArray();
            char[] nsArray = ns.toCharArray();
            int oLength = os.length();
            StringBuilder buf = new StringBuilder (sourceArray.length);
            buf.append (sourceArray, 0, i).append(nsArray);
            i += oLength;
            int j = i;
            // Replace all remaining instances of oldString with newString.
            while ((i = source.indexOf(os, i)) > 0) {
                buf.append (sourceArray, j, i - j).append(nsArray);
                i += oLength;
                j = i;
            }
            buf.append (sourceArray, j, sourceArray.length - j);
            source = buf.toString();
            buf.setLength (0);
        }
        return source;
	}

	public static boolean like (String str, String expr) {
		String regex = quotemeta(expr);
		regex = regex.replace (UNDERSCORE, DOT).replace (PERCENT, ".*?");
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE
				| Pattern.DOTALL);
		return p.matcher (str).matches ();
	}
	
	public interface VariableResolver {
		String resolve (String namespace, String name);
	}
	
	public static String resolve (String exp, String expStart, String expEnd, VariableResolver resolver) {
		if (exp == null) {
			return null;
		}
		
		int indexOfStart = exp.indexOf (expStart);
		while (indexOfStart > -1) {
			int indexOfEnd = exp.indexOf (expEnd, indexOfStart + expStart.length ());
			if (indexOfEnd <= -1) {
				indexOfStart = exp.indexOf (expStart, indexOfStart + expStart.length ());
				continue;
			}
			
			String prop = exp.substring (indexOfStart + expStart.length (), indexOfEnd);
			
			String ns = null;
			String var = prop;
			
			int indexOfDot = prop.indexOf (Lang.DOT);
			if (indexOfDot > 0) {
				ns 	= prop.substring (0, indexOfDot);
				var = prop.substring (indexOfDot + 1);
			}
			
			String value = resolver.resolve (ns, var);
			
			exp = replace (exp, expStart + prop + expEnd, value);
			
			indexOfStart = exp.indexOf (expStart, indexOfStart + expStart.length ());
		}
		
		return exp;
	}
	
	public static String resolve (String exp, VariableResolver resolver) {
		return resolve (exp, ExpStart, ExpEnd, resolver);
	}

	private static String quotemeta (String s) {
		if (s == null) {
			throw new IllegalArgumentException("String cannot be null");
		}

		int len = s.length ();
		if (len == 0) {
			return Lang.BLANK;
		}

		StringBuilder sb = new StringBuilder(len * 2);
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if ("[](){}.*+?$^|#\\".indexOf(c) != -1) {
				sb.append (Lang.BACKSLASH);
			}
			sb.append(c);
		}
		return sb.toString();
	}
	
	public static String md5 (String message) throws Exception {
		MessageDigest md = MessageDigest.getInstance ("MD5"); 
		byte [] hash = md.digest (message.getBytes ("UTF-8")); 

		StringBuilder sb = new StringBuilder (2 * hash.length); 
		for (byte b : hash) { 
			sb.append (String.format ("%02x", b&0xff)); 
		} 
		
		String md5 = sb.toString ();
		sb.setLength (0);
		
		return md5;
	}

	public static String [] keys () throws Exception {
		return keys (30, 40);
	}
	
	public static String [] keys (int akl, int skl) throws Exception {
		return new String [] { UUID (RandDigits, akl).toUpperCase (), UUID (RandDigitsX, skl) };
	}

	public static void setDebugMode (boolean debugMode) {
		System.setProperty (API_DEBUG_MODE, String.valueOf (debugMode));
	}

	public static boolean isDebugMode () {
		String dm = System.getProperty (API_DEBUG_MODE);
		if (dm == null) {
			return false;
		}
		return TrueValues.contains (dm);
	}
	
	/*
	private static String hash (byte [] key, String encoding) throws Exception {
		return new String (Base64.encodeBase64 (key), encoding).trim ();
	}
	*/
	
    public static byte[] decodeHex(final char[] data) throws Exception {

        final int len = data.length;

        if ((len & 0x01) != 0) {
            throw new Exception ("Odd number of characters.");
        }

        final byte[] out = new byte[len >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j], j) << 4;
            j++;
            f = f | toDigit(data[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }

    public static char[] encodeHex (final byte[] data) {
        return encodeHex(data, true);
    }

    public static char[] encodeHex (final byte[] data, final boolean toLowerCase) {
        return encodeHex (data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
    }

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data
     *            a byte[] to convert to Hex characters
     * @param toDigits
     *            the output alphabet
     * @return A char[] containing hexadecimal characters
     * @since 1.4
     */
    private static char[] encodeHex(final byte[] data, final char[] toDigits) {
        final int l = data.length;
        final char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
        return out;
    }
	
    private static int toDigit (final char ch, final int index) throws Exception {
        final int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new Exception ("Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }
    
	private static void readNashornError (Throwable err, Throwable first, JsonArray trace) {
        NashornException exc = (NashornException) err;
        
        StackTraceElement [] stack = exc.getStackTrace ();
		for (StackTraceElement line : stack) {
			String className = line.getClassName ();
			
			int indeOfNashornScript = className.indexOf (NashornScript);
			if (indeOfNashornScript < 0) {
				continue;
			}
			
	        JsonObject oLine = new JsonObject ();
	        
	        String originalScript = getScriptFile (first);
	        
	        String file = line.getFileName ();
	        
	        if (EvalFile.equals (file)) {
	        	file = originalScript;
	        } else {
	            file = removeScriptPath (file);
	        }
			
			oLine.set (ApiResponse.Error.File, file);
	        
	        oLine.set (ApiResponse.Error.Line, line.getLineNumber ());
	        
	        trace.add (0, oLine);
		}
		
	}
	
	private static String getScriptFile (Throwable first) {
		Throwable cause = first;
		while (true) {
			if (cause instanceof ScriptingEngineException) {
				return removeScriptPath (((ScriptingEngineException)cause).getScript ());
			}
			Throwable c = cause.getCause ();
			if (c == null) {
				return null;
			}
			cause = c;
		}
	} 
	
	private static String removeScriptPath (String file) {
		if (file == null) {
			return null;
		}
		int indexOfPath = file.indexOf (PlatformPath); 
		if (indexOfPath <= 0) {
			indexOfPath = file.indexOf (ApiResourcePath); 
		}
		if (indexOfPath > 0) {
			file = file.substring (indexOfPath);
		}
		return file;
	}
	
	public static String [] toArray (Set<String> set1, Set<String> set2) {
		if (set1 == null) {
			return null;
		}
		if (set1.isEmpty ()) {
			return new String [] {};
		}
		
		String [] array = new String [set1.size ()]; 
		
		set1.toArray (array);
		
		if (set2 != null && !set2.isEmpty ()) {
			String [] a2 = new String [set2.size ()]; 
			set2.toArray (a2);
			array = Lang.add (array, a2);
		}
		
		return array;
	}
	
	public static String capitalizeFirst (String name) {
		if (Lang.isNullOrEmpty (name)) {
			return name;
		}
		if (name.length () == 1) {
			return name.toUpperCase ();
		}
		return name.substring (0, 1).toUpperCase () + name.substring (1);
	}
	
	public static String pluralize (String name) {
		if (Lang.isNullOrEmpty (name)) {
			return name;
		}
		return (name.endsWith ("y") ? (name.substring (0, name.length () - 1) + "ies") : name + "s");
	}
	
	public static String singularize (String name) {
		if (Lang.isNullOrEmpty (name) || !name.endsWith ("s")) {
			return name;
		}
		return (name.endsWith ("ies") ? (name.substring (0, name.length () - 3) + "y") : name.substring (0, name.length () - 1));
	}
	
	public static boolean wmatches (String wildcard, String value) {
		WildcardMatcher matcher = new WildcardMatcher (WildcardCompiler.compile (wildcard), value);
		return matcher.find ();
	}
	
	public static void setTimeout (Runnable runnable, int delay){
	    new Thread(() -> {
	        try {
	            Thread.sleep (delay);
	            runnable.run ();
	        } catch (Exception e){
	            // Ignore
	        }
	    }).start();
	}
	
}