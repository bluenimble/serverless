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
package com.bluenimble.platform.api.media;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.Lang;

public class MediaTypeUtils {
	
	static final Map<String, String> MediaTypes = new HashMap<String, String> ();
	
	public static void install (InputStream mimes) throws IOException {
		
		String line;
		
		BufferedReader br = new BufferedReader (new InputStreamReader (mimes));
		while ((line = br.readLine ()) != null) {
			line = line.trim ();
			if (Lang.BLANK.equals (line) || line.startsWith (Lang.SHARP)) {
				continue;
			}
			int indexOfEq = line.lastIndexOf (Lang.EQUALS);
			if (indexOfEq < 1) {
				continue;
			}
			String 		mime 		= line.substring (0, indexOfEq);
			String [] 	extensions 	= Lang.split (line.substring (indexOfEq + 1), Lang.COMMA);
			
			for (String extension : extensions) {
				if (MediaTypes.containsKey (extension)) {
					System.out.println ("extension: " + extension + " found twice. Line " + line);
				}
				MediaTypes.put (extension, mime);
			}
			
			
		}

	}
	
	public static String getMediaForFile (String ext) {
		if (Lang.isNullOrEmpty (ext)) {
			return null;
		}
		return MediaTypes.get (ext);
	}

	protected static class ParseResults {
		String type;
		String subType;

		Map<String, String> params;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder ("('" + type + "', '" + subType
					+ "', {");
			for (String k : params.keySet ()) {
				sb.append("'" + k + "':'" + params.get(k) + "',");
			}
			sb.append("})").toString();
			
			String s = sb.toString ();
			sb.setLength (0);
			
			return s;
		}
	}

	protected static ParseResults parseMediaType (String mediaType) {
		String[] parts = mediaType.split (Lang.SEMICOLON);
		ParseResults results = new ParseResults();
		results.params = new HashMap<String, String>();

		for (int i = 1; i < parts.length; ++i) {
			String p = parts[i];
			String[] subParts = p.split (Lang.EQUALS);
			if (subParts.length == 2)
				results.params.put(subParts[0].trim(), subParts[1].trim());
		}
		String fullType = parts[0].trim();

		if (fullType.equals (Lang.STAR)) {
			fullType = "*/*";
		}
		String [] types = fullType.split (Lang.SLASH);
		results.type = types [0].trim ();
		if (types.length > 1) {
			results.subType = types [1].trim ();
		}
		return results;
	}

	protected static ParseResults parseMediaRange(String range) {
		ParseResults results = parseMediaType(range);
		String q = results.params.get ("q");
		float f = toFloat (q, 1);
		if (Lang.isNullOrEmpty (q) || f < 0 || f > 1) {
			results.params.put ("q", "1");
		}
		return results;
	}

	protected static class FitnessAndQuality implements
			Comparable<FitnessAndQuality> {
		int fitness;

		float quality;

		String mediaType; // optionally used

		public FitnessAndQuality(int fitness, float quality) {
			this.fitness = fitness;
			this.quality = quality;
		}

		public int compareTo(FitnessAndQuality o) {
			if (fitness == o.fitness) {
				if (quality == o.quality)
					return 0;
				else
					return quality < o.quality ? -1 : 1;
			} else
				return fitness < o.fitness ? -1 : 1;
		}
	}

	protected static FitnessAndQuality fitnessAndQualityParsed(String mediaType,
			Collection<ParseResults> parsedRanges) {
		int bestFitness = -1;
		float bestFitQ = 0;
		ParseResults target = parseMediaRange(mediaType);

		for (ParseResults range : parsedRanges) {
			if ((target.type.equals(range.type) || range.type.equals (Lang.STAR) || target.type.equals (Lang.STAR))
					&& (target.subType.equals (range.subType)
							|| range.subType.equals (Lang.STAR) || target.subType
								.equals (Lang.STAR))) {
				for (String k : target.params.keySet()) {
					int paramMatches = 0;
					if (!k.equals("q") && range.params.containsKey(k)
							&& target.params.get(k).equals(range.params.get(k))) {
						paramMatches++;
					}
					int fitness = (range.type.equals(target.type)) ? 100 : 0;
					fitness += (range.subType.equals(target.subType)) ? 10 : 0;
					fitness += paramMatches;
					if (fitness > bestFitness) {
						bestFitness = fitness;
						bestFitQ = toFloat (range.params.get ("q"), 0);
					}
				}
			}
		}
		return new FitnessAndQuality (bestFitness, bestFitQ);
	}

	protected static float qualityParsed (String mediaType,
			Collection<ParseResults> parsedRanges) {
		return fitnessAndQualityParsed(mediaType, parsedRanges).quality;
	}

	public static float quality(String mediaType, String ranges) {
		List<ParseResults> results = new LinkedList<ParseResults>();
		for (String r : ranges.split (Lang.COMMA)) {
			results.add (parseMediaRange (r));
		}
		return qualityParsed (mediaType, results);
	}

	public static String bestMatch (Collection<String> supported, String header) {
		if (supported == null || supported.isEmpty ()) {
			return null;
		}
		String [] array = new String [supported.size ()]; 
		
		supported.toArray (array);
		
		return bestMatch (array, header);
	}
	
	public static String bestMatch (String [] supported, String header) {
		if (supported == null || supported.length == 0 || Lang.isNullOrEmpty (header)) {
			return null;
		}
		List<ParseResults> parseResults = new LinkedList<ParseResults>();
		List<FitnessAndQuality> weightedMatches = new LinkedList<FitnessAndQuality>();
		
		for (String r : header.split (Lang.COMMA)) {
			parseResults.add (parseMediaRange (r));
		}
		
		for (String s : supported) {
			FitnessAndQuality fitnessAndQuality = fitnessAndQualityParsed (s, parseResults);
			fitnessAndQuality.mediaType = s;
			weightedMatches.add (fitnessAndQuality);
		}
		
		Collections.sort (weightedMatches);

		FitnessAndQuality lastOne = weightedMatches.get (weightedMatches.size () - 1);
		
		return (lastOne.quality != 0) ? lastOne.mediaType : null;
	}
	
	private static float toFloat (String str, float defaultValue) {
		if (str == null) {
			return defaultValue;
		}
		try {
			return Float.parseFloat (str);
		} catch (NumberFormatException nfe) {
			return defaultValue;
		}
	}	
	
	public static void main (String [] args) {
		
	}
	
}
