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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.xml.support.Component;
import com.bluenimble.platform.xml.support.ComponentAttribute;

public class XmlReader extends DefaultHandler implements ErrorHandler {
	
	public static final String SAX_PARSER_FACTORY = "com.qlogic.commons.saxParserFactory";

	private Component currentElement;

	private Component parentElement;

	private Component document;
	
	private String [] possiblePublicDtds;
	
	private String dtd;
	
	private StringBuilder messages;
	
	private SAXParser saxParser;
	
	public XmlReader (String filePath) throws Exception {
		this(filePath, null);
	}
	
	public XmlReader (File file) throws Exception {
		this(file.getAbsolutePath ());
	}
	
	public XmlReader (String filePath, String [] possiblePublicDtds) throws Exception {
		this(filePath, possiblePublicDtds, null);
	}
	
	public XmlReader (File file, String [] possiblePublicDtds) throws Exception {
		this(file.getAbsolutePath (), possiblePublicDtds);
	}
	
	public XmlReader (String filePath, String [] possiblePublicDtds, String dtd) throws Exception {
		this(null, new FileInputStream (filePath), possiblePublicDtds, dtd, true);
	}
	
	public XmlReader (File file, String [] possiblePublicDtds, String dtd) throws Exception {
		this(new FileInputStream (file), possiblePublicDtds, dtd);
	}
	
	public XmlReader(InputStream is) throws Exception {
		this (null, is, null, null, true);
	}

	public XmlReader(InputStream is, String [] possiblePublicDtds, String dtd) throws Exception {
		this (null, is, possiblePublicDtds, dtd, true);
	}
	
	public XmlReader(InputStream is, String [] possiblePublicDtds, String dtd, boolean validating) throws Exception {
		this (null, is, possiblePublicDtds, dtd, validating);
	}

	public XmlReader(String saxParserFactoryClass, InputStream is, String [] possiblePublicDtds, String dtd, boolean validating) throws Exception {
		try {
	        messages = new StringBuilder();
			this.possiblePublicDtds = possiblePublicDtds;
			this.dtd = dtd;
			document = new Component("");
			parentElement = document;
			SAXParserFactory factory = null;
			if (saxParserFactoryClass == null) {
				saxParserFactoryClass = System.getProperty (SAX_PARSER_FACTORY);
			}
			if (saxParserFactoryClass != null) {
				factory = (SAXParserFactory)Thread.currentThread().getContextClassLoader().loadClass (saxParserFactoryClass).newInstance ();
			} else {
				factory = SAXParserFactory.newInstance();
			}
			factory.setNamespaceAware(false);
			factory.setValidating(validating && dtd != null);
			saxParser = factory.newSAXParser ();
			XMLReader xmlReader = saxParser.getXMLReader();
			xmlReader.setEntityResolver (this);
			xmlReader.setErrorHandler (this);
			xmlReader.setContentHandler(this);
			xmlReader.setDTDHandler (this);
			xmlReader.parse(new InputSource (is));
		} catch (SAXParseException err) {
			throw new Exception("** Parsing error" + ", line "
					+ err.getLineNumber() + ", uri " + err.getSystemId() + 
					((messages != null) ? Lang.ENDLN + messages.toString () : Lang.ENDLN), err);
		} catch (SAXException e) {
			if (e.getException() != null) {
				throw new Exception(((messages != null) ? Lang.ENDLN + messages.toString () : Lang.ENDLN), e.getException());
			} else {
				throw new Exception (((messages != null) ? Lang.ENDLN + messages.toString () : Lang.ENDLN), e);
			}
		} catch (Throwable t) {
			throw new Exception (((messages != null) ? Lang.ENDLN + messages.toString () : Lang.ENDLN), t);
		}
		if (messages != null && messages.length() > 0) {
			throw new Exception(((messages != null) ? Lang.ENDLN + messages.toString () : Lang.ENDLN));
		}
	}
	
	public XmlReader(StringBuilder doc) throws Exception {
		this (doc, null, null, true);
	}
	
	public XmlReader(StringBuilder doc, String [] possiblePublicDtds, String dtd, boolean validating) throws Exception {
		try {
			messages = new StringBuilder();
			this.possiblePublicDtds = possiblePublicDtds;
			this.dtd = dtd;
			document = new Component("");
			parentElement = document;
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setValidating(validating);
			SAXParser saxParser = factory.newSAXParser ();
			StringReader reader = new StringReader(doc.toString());
			saxParser.parse(new InputSource(reader), this);
		} catch (SAXParseException err) {
			throw new Exception("** Parsing error" + ", line "
					+ err.getLineNumber() + ", uri " + err.getSystemId() + 
					((messages != null) ? Lang.ENDLN + messages.toString () : Lang.ENDLN), err);
		} catch (SAXException e) {
			if (e.getException() != null) {
				throw new Exception(((messages != null) ? Lang.ENDLN + messages.toString () : Lang.ENDLN), e.getException());
			} else {
				throw new Exception (((messages != null) ? Lang.ENDLN + messages.toString () : Lang.ENDLN), e);
			}
		} catch (Throwable t) {
			throw new Exception (((messages != null) ? Lang.ENDLN + messages.toString () : Lang.ENDLN), t);
		}
	}
	
	/**
	 * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator l) {
		// we'd record this if we needed to resolve relative URIs
		// in content or attributes, or wanted to give diagnostics.
	}

	/**
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
	}

	/**
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
	}

	/**
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String namespaceURI, String lName, // local name
			String qName, // qualified name
			Attributes attrs) throws SAXException {
		String eName = lName; // element name
		if ("".equals(eName)) {
			eName = qName; // namespaceAware = false
		}
		currentElement = new Component(eName);
		if (attrs != null) {
			for (int i = 0; i < attrs.getLength(); i++) {
				String aName = attrs.getLocalName(i); // Attr name
				if ("".equals(aName)) {
					aName = attrs.getQName(i);
				}
				currentElement.addAttribute(aName, attrs.getValue(i));
			}
		}
		parentElement.addObject(currentElement);
		parentElement = currentElement;
	}

	/**
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String namespaceURI, String sName, // simple name
			String qName) // qualified name
			throws SAXException {
		parentElement = parentElement.parents(0);
	}

	/**
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char buf[], int offset, int len) throws SAXException {
		String curr_val = new String(buf, offset, len);
		if (!curr_val.equals("")) {
			currentElement.value(currentElement.value() + curr_val);
		}
	}

	/**
	 * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
	 */
	public void ignorableWhitespace(char buf[], int offset, int len)
			throws SAXException {
		// this whitespace ignorable ... so we ignore it!
		// this callback won't be used consistently by all parsers,
		// unless they read the whole DTD. Validating parsers will
		// use it, and currently most SAX nonvalidating ones will
		// also; but nonvalidating parsers might hardly use it,
		// depending on the DTD structure.
	}

	/**
	 * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data)
		throws SAXException {
	}

	/**
	 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
	 */
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException {

		InputSource inputSource = null;

		if (systemId != null) {
			if (possiblePublicDtds == null || 
				dtd == null || 
				!Lang.existsIn (systemId, possiblePublicDtds)) {
				return new InputSource(systemId);
			}
			
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			
			if (classLoader == null) {
				classLoader = XmlReader.class.getClassLoader();
			}

			InputStream is = null;

			if (classLoader != null) {
				is = classLoader.getResourceAsStream(dtd);
			}
			
			if (is == null) {
				URL url = null;
				try {
					url = new URL (dtd);
					is = url.openStream();
				} catch (MalformedURLException mfuex) {
					throw new SAXException (mfuex);
				} catch (IOException ioex) {
					throw new SAXException (ioex);
				}
			}
			
			if (is != null) {
				inputSource = new InputSource(is);
				inputSource.setSystemId (systemId);
			}
		}
		
		return inputSource;
	}
	

	/**
	 * @param path chemin de sortie
	 * @throws IOException
	 */
	public void witeToFile (String path) throws IOException {
		document.print(path);
	}

	/**
	 * @return renvoi du composant charg�
	 */
	public Component getDocument() {
		return document.childs(0);
	}
	
    /**
     * ErrorHandler interface method
     * @param exception
     * @throws SAXException
     */
    public void warning(SAXParseException exception)
        throws SAXException {
        errorHandlerException(exception);
    }

    /**
     * ErrorHandler interface method
     * @param exception
     * @throws SAXException
     */
    public void error(SAXParseException exception)
        throws SAXException {
        errorHandlerException(exception);
    }

    /**
     * ErrorHandler interface method
     * @param exception
     * @throws SAXException
     */
    public void fatalError(SAXParseException exception)
        throws SAXException {
        errorHandlerException(exception);
    }

    /**
     * Deal with exceptions passed to the ErrorHandler interface by the parser
     */
    private void errorHandlerException(Exception e) {
        invalidate(e.getMessage());
    }

    /**
     * Set the validation status flag to false and capture the message for
     *  use later
     * @param message
     */
    private void invalidate(String message) {
        messages.append(message).append(Lang.ENDLN);
    }
    
    public void reset () {
    	saxParser.reset ();
    	currentElement = null;
    	parentElement = null;
    	document = null;
    	possiblePublicDtds = null;
    	dtd = null;
    	messages = null;
    }
    
    public static void main (String [] args) throws Exception, IOException {
    	XmlReader reader = new XmlReader (new File ("/temp/Folder.xml"));
    	Component root = reader.getDocument ();
    	
    	for (int i = 0; i < root.attrLength (); i++) {
    		ComponentAttribute attr = root.attributes (i);
    		String name = attr.name ();
    		name = name.substring (0, 1).toUpperCase () + name.substring (1);
    		System.out.println ("public static final String " + name + " = \"" + attr.name () + "\";");
    	}
    	
    	for (int i = 0; i < root.attrLength (); i++) {
    		ComponentAttribute attr = root.attributes (i);
    		String name = attr.name ();
    		name = name.substring (0, 1).toUpperCase () + name.substring (1);
    		System.out.println ("Properties.put (" + name + ", \"" + attr.value () + "\");");
    	}
    	
    }
    
}