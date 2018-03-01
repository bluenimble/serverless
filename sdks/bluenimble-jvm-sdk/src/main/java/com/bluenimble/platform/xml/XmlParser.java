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
package com.bluenimble.platform.xml;

import java.io.Reader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.bluenimble.platform.Lang;

public abstract class XmlParser extends DefaultHandler {
	
	private static final SAXParserFactory DEFAULT_FACTORY = SAXParserFactory.newInstance ();
	
	protected enum Kind {
		Document,
		Element
	}
	
	protected transient SAXParserFactory factory;
	protected transient SAXParser saxParser;

	protected String rootTag; 
	protected String currentTag; 

	public XmlParser (String saxParserClassFactory) throws XmlParserException {
		super ();
		try {
			if (saxParserClassFactory != null) {
				factory = (SAXParserFactory)Thread.currentThread ().getContextClassLoader ().loadClass (saxParserClassFactory).newInstance ();
			} else {
				factory = DEFAULT_FACTORY;
			}
			factory.setNamespaceAware (true);
			saxParser = factory.newSAXParser ();
		} catch (Throwable e) {
			throw new XmlParserException (e);
		} 
	}
	
	public XmlParser () throws XmlParserException {
		this (null);
	}
	
	public void parse (Reader xml) throws XmlParserException {
		try {
			saxParser.parse (new InputSource (xml), this);
		} catch (Exception th) {
			if (th.getCause () != null && th.getCause () instanceof XmlParserException) {
				throw (XmlParserException)th.getCause ();
			}
			throw new XmlParserException (th);
		}
	}

	@Override
	public void startDocument () throws SAXException {
		try {
			onStart 	(Kind.Document, null);
		} catch (Exception e) {
			throw new SAXException (e.getMessage (), e);
		}
	}

	@Override
	public void endDocument () throws SAXException {
		try {
			onEnd 	(Kind.Document, null);
		} catch (Exception e) {
			throw new SAXException (e.getMessage (), e);
		}
	}
	
	public void reset () {
		saxParser.reset ();
		currentTag = null;
	}

	@Override
	public void startElement (String uri, String localName, String name,
			Attributes attrs) throws SAXException {
		String eName = localName; // element name
		if (Lang.BLANK.equals (eName)) {
			eName = name; // namespaceAware = false
		}
		
		if (currentTag == null) {
			rootTag = eName;
		}

		currentTag = eName;
		
		try {
			onStart	(Kind.Element, eName);
			if (attrs != null) {
				for (int i = 0; i < attrs.getLength(); i++) {
					String aName = attrs.getLocalName (i);
					if (Lang.BLANK.equals (aName)) {
						aName = attrs.getQName (i);
					}
					onAttribute (currentTag, aName, attrs.getValue(i));
				}
			}
		} catch (Exception e) {
			throw new SAXException (e.getMessage (), e);
		}
		
	}

	@Override
	public void characters (char[] ch, int start, int length)
			throws SAXException {
		
		if (currentTag == null) {
			return;
		}
		String value = null;
		try {
			value = new String (ch, start, length);
		} catch (Throwable th) {
			// Ignore
		}
		try {
			onData 	(value);
		} catch (Exception e) {
			throw new SAXException (e.getMessage (), e);
		}
	}

	@Override
	public void endElement (String uri, String localName, String name)
		throws SAXException {
		String eName = localName; 
		if (Lang.BLANK.equals (eName)) {
			eName = name; 
		}
		try {
			onEnd 	(Kind.Element, eName);
		} catch (Exception e) {
			throw new SAXException (e.getMessage (), e);
		}
	}
	
	protected abstract void onStart 	(Kind kind, String name) throws XmlParserException;
	protected abstract void onEnd 		(Kind kind, String name) throws XmlParserException;
	protected abstract void onData 		(String data) throws XmlParserException;
	protected abstract void onAttribute	(String eName, String name, String value) throws XmlParserException;

}
