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
package com.bluenimble.platform.remote.impls.serializers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.remote.SerializationException;
import com.bluenimble.platform.remote.Serializer;

public class XmlSerializer implements Serializer {
	
	private static final long serialVersionUID = 5466668574952401976L;

	private static final DocumentBuilderFactory Factory = DocumentBuilderFactory.newInstance();

	@Override
	public Object serialize (InputStream input) throws SerializationException {
		if (input == null) {
			return null;
		}
		try {
			DocumentBuilder dBuilder = Factory.newDocumentBuilder();
			Reader reader = new BufferedReader (new InputStreamReader(input));
			InvalidXmlCharacterFilter filter = new InvalidXmlCharacterFilter (reader);
			return dBuilder.parse (new InputSource (filter));
		} catch (Exception ex) {
			throw new SerializationException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (input);
		}
	}
	
	public static void main (String [] args) throws Exception {
		DocumentBuilder dBuilder = Factory.newDocumentBuilder ();
		Reader reader = new BufferedReader (new InputStreamReader(new FileInputStream (new File ("/Users/lilya/cnn_topstories.xml"))));
		InvalidXmlCharacterFilter filter = new InvalidXmlCharacterFilter (reader);
		Document document = dBuilder.parse (new InputSource (filter));
		Node node = document.getElementsByTagName ("channel").item (0);
		NodeList list = node.getChildNodes ();
		JsonObject result = new JsonObject ();
		JsonArray  array  = new JsonArray ();
		result.set ("items", array);
		for (int i = 0; i < list.getLength (); i++) {
			Node n = list.item (i);
			if (n.getNodeName () == null || n.getNodeName () != "item") {
				continue;
			}
			JsonObject item = new JsonObject ();
			JsonObject provider = new JsonObject ();
			item.set ("provider", provider);
			provider.set ("id", "CNN");
			provider.set ("name", "CNN");
			NodeList attrs = n.getChildNodes ();
			for (int j = 0; j < attrs.getLength (); j++) {
				Node a = attrs.item (j);
				if (a.getNodeName () == null) {
					continue;
				}
				if (a.getNodeName ().equals ("media:group")) {
					item.set ("url", a.getFirstChild ().getAttributes ().getNamedItem ("url").getNodeValue ().trim ());
				}
				if (a.getNodeName ().equals ("link")) {
					item.set ("id", Base64.encodeBase64URLSafeString (a.getTextContent ().trim ().getBytes ()));
					provider.set ("link", a.getTextContent ().trim ());
				}
				if (a.getNodeName ().equals ("title")) {
					provider.set ("text", a.getTextContent ().trim ());
				}
				if (a.getNodeName ().equals ("description")) {
					provider.set ("detail", a.getTextContent ().trim ());
				}
				if (a.getNodeName ().equals ("pubDate")) {
					provider.set (
						"timestamp", 
						Lang.toUTC (Lang.toDate (a.getTextContent ().trim (), "EEE, dd MMM yyyy HH:mm:ss zzz"))
					);
				}
			}
			array.add (item);
		}
		System.out.println (result);
	}
	
}
