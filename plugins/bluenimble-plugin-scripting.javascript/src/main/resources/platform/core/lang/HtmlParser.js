var JC_Jsoup	 	= Java.type ('org.jsoup.Jsoup');

/**
  HtmlParser Class
  @namespace HtmlParser
*/
var HtmlParser = {
	/**	
	  Parses an html payload 
	  @param {object} - proxy
	  
	  @returns {Document} an html document instance 
	*/
	parse: function (html) {
		return JC_Jsoup.parse (html);
	},
	/**	
	  Open an url, parse it and create an html document
	  @param {object} - proxy
	  
	  @returns {Document} an html document instance 
	*/
	open: function (url) {
		return JC_Jsoup.connect (url).get ();
	}
};