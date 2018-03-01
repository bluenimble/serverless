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
package com.bluenimble.platform.messenger.impls.smtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.TemplateSource;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.messaging.Messenger;
import com.bluenimble.platform.messaging.MessengerException;
import com.bluenimble.platform.messaging.Recipient;
import com.bluenimble.platform.messaging.Sender;
import com.bluenimble.platform.messaging.impls.JsonSender;

public class SmtpMessenger implements Messenger {

	private static final long serialVersionUID = 4303282790607692198L;
	
	protected static final Map<String, CachedTemplate> templates = new ConcurrentHashMap<String, CachedTemplate> ();
	
	private static final Sender NoSender 	= new JsonSender (null);
	
	private static final Handlebars engine 		= new Handlebars ();
	
	private static final String StartDelimitter 	= "[[";
	private static final String EndDelimitter 		= "]]";
	
	private String 	user;
	private Session session;
	
	public SmtpMessenger (String user, Session session) {
		this.session = session;
		this.user = user;
		
		engine.startDelimiter (StartDelimitter);
		engine.endDelimiter (EndDelimitter);
	}
	
	@Override
	public void send (Sender sender, Recipient [] recipients, String subject, String content, ApiStreamSource... attachments) throws MessengerException {
	
		ClassLoader pcl = Thread.currentThread ().getContextClassLoader ();
		
		Thread.currentThread ().setContextClassLoader (getClass().getClassLoader ());
		
		// creates a new e-mail message
		try {
			
			if (sender == null) {
				sender = NoSender;
			}
			
			Message message = new MimeMessage (session);

			String senderEmail = sender.id ();
			if (Lang.isNullOrEmpty (senderEmail)) {
				senderEmail = user;
			}
			
			String senderName = sender.name ();
			if (Lang.isNullOrEmpty (senderName)) {
				message.setFrom (new InternetAddress (senderEmail));
			} else {
				message.setFrom (new InternetAddress (senderEmail, senderName));
			}
			
			InternetAddress [] toAddresses = new InternetAddress [recipients.length];
			for (int i = 0; i < recipients.length; i++) {
				toAddresses [i] = new InternetAddress (recipients [i].id ());
			}
			
			message.setRecipients (Message.RecipientType.TO, toAddresses);
			message.setSubject (subject);
			message.setSentDate (new Date ());

			// creates message part
			MimeBodyPart messageText = new MimeBodyPart ();
			messageText.setContent (content, ApiContentTypes.Html);

			// creates multi-part
			Multipart multipart = new MimeMultipart ();
			multipart.addBodyPart (messageText);

			// adds attachments
			if (attachments != null && attachments.length > 0) {
				for (final ApiStreamSource attachment : attachments) {
					MimeBodyPart mbd = new MimeBodyPart ();
			        DataSource ds = new DataSource () {
						@Override
						public String getContentType () {
							return attachment.contentType ();
						}

						@Override
						public InputStream getInputStream () throws IOException {
							return attachment.stream ();
						}

						@Override
						public String getName () {
							return attachment.name ();
						}

						@Override
						public OutputStream getOutputStream () throws IOException {
							throw new UnsupportedOperationException ("getOutputStream not supported");
						}
			        	
			        }; 
			        mbd.setDataHandler (new DataHandler (ds));

					multipart.addBodyPart (mbd);
				}
			}

			// sets the multi-part as e-mail's content
			message.setContent (multipart);

			// sends the e-mail
			Transport.send (message);
		} catch (Exception ex) {
			throw new MessengerException (ex.getMessage (), ex);
		} finally {
			Thread.currentThread ().setContextClassLoader (pcl);
		}
		
	}

	@Override
	public void send (Sender sender, Recipient [] recipients, String subject, final ApiResource content, JsonObject data,
			ApiStreamSource... attachments) throws MessengerException {
		
		final String uuid = content.owner () + Lang.SLASH + content.name ();
		
		CachedTemplate cTemplate = templates.get (uuid);
		
		if (cTemplate == null || cTemplate.timestamp < content.timestamp ().getTime ()) {
			cTemplate = new CachedTemplate ();
			cTemplate.timestamp = content.timestamp ().getTime ();
			
			TemplateSource source = new TemplateSource () {
				@Override
				public long lastModified () {
					return content.timestamp ().getTime ();
				}
				@Override
				public String filename () {
					return uuid;
				}
				@Override
				public String content () throws IOException {
					
					InputStream is = null;
					
					try {
						is = content.toInput ();
						return IOUtils.toString (is);
					} finally {
						IOUtils.closeQuietly (is);
					}
				}
			};
			
			try {
				cTemplate.template = engine.compile (source, StartDelimitter, EndDelimitter);
			} catch (IOException e) {
				throw new MessengerException (e.getMessage (), e);
			}
			
			templates.put (uuid, cTemplate);
			
		}
		
		StringWriter sw = new StringWriter ();
		
		try {
			cTemplate.template.apply (data, sw);
		} catch (Exception e) {
			throw new MessengerException (e.getMessage (), e);
		}
		
		send (sender, recipients, subject, sw.toString (), attachments);
		
	}
	
	
	class CachedTemplate {
		Template 	template;
		long		timestamp;
	}
	
}
