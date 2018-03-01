
function toJavaSender (sender) {
	return new JC_Messenger_Sender (JC_Converters.convert (sender));
}
function toJavaRecipients (recipients) {
	if (recipients == null || !recipients.length || recipients.length <= 0) {
		return null;
	}
	var jRecipients = [];
	for (var i = 0; i < recipients.length; i++) {
		jRecipients.push (new JC_Messenger_Recipient (JC_Converters.convert (recipients [i])));
	}
	return jRecipients;
}
function toJavaAttachments (attachments) {
	if (!attachments || !attachments.length || attachments.length <= 0) {
		return null;
	}
	var jAttachments = [];
	for (var i = 0; i < attachments.length; i++) {
		jAttachments.push (attachments [i].proxy);
	}
	return jAttachments;
}

/**
  Represents a Messenger instance<br/>
  <strong>Do not call constructor directly</strong>. 
  @see Api#messenger
  @class Messenger
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var Messenger = function (proxy) {

	/**	
	  Send a message to a list of recipients
	  @param {JsonObject} - the sender
	  @param {JsonArray} - the list of recipients
	  @param {string} - the subject of the message
	  @param {string} - the content of the message
	  @param {ApiStreamSource[]} [attachments] - an array of attachments 
	*/
	this.send = function (sender, recipients, subject, content, attachments) {
		proxy.send (
			toJavaSender (sender), 
			toJavaRecipients (recipients), 
			subject, 
			content, 
			toJavaAttachments (attachments)
		);
	};
	
	/**	
	  Send a message to a list of recipients using a template as content
	  @param {JsonObject} - the sender
	  @param {JsonArray} - the list of recipients
	  @param {string} - the subject of the message
	  @param {ApiResource} - the template to use as message body
	  @param {JsonObject} - the data binding for the template
	  @param {ApiStreamSource[]} [attachments] - an array of attachments 
	*/
	this.sendWithTpl = function (sender, recipients, subject, template, data, attachments) {
		proxy.send (
			toJavaSender (sender), 
			toJavaRecipients (recipients), 
			subject, 
			template.proxy,
			JC_Converters.convert (data),
			toJavaAttachments (attachments)
		);
	};

};
