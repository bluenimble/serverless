/**
  Represents a StorageObject instance<br/>
  <strong>Do not call constructor directly</strong>. 
  @see Storage
  @see Folder
  @class StorageObject
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var StorageObject = function (proxy) {

	this.proxy 			= proxy;

	/**	
	  The name of this storage object. In a filesystem storage, this is the name of a folder or a file
	  @type {string}
	  @readonly
	*/
	this.name 			= proxy.name ();
	/**	
	  The lastModified date of this storage object
	  @type {Date}
	  @readonly
	*/
	this.timestamp 		= proxy.timestamp ();
	/**	
	  The most close contentType for this storage object based on it's extension. It's undefined if this storage object is a folder.
	  @type {string}
	  @readonly
	*/
	this.contentType	= proxy.contentType ();
	/**	
	  Is this storage object a folder?
	  @type {boolean}
	  @readonly
	*/
	this.isFolder		= proxy.isFolder ();
	/**	
	  The length/size in bytes of this storage object. 0 if it's a folder
	  @type {integer}
	  @readonly
	*/
	this.length 		= this.isFolder ? 0 : proxy.length ();

	/**	
	  Check if this object exists
	  @param {string} - the new name
	*/
	this.exists = function () {
		return proxy.exists ();
	};
	
	/**	
	  Rename this storage object
	  @param {string} - the new name
	*/
	this.rename = function (newName) {
		proxy.rename (newName);
	};
	
	/**	
	  Delete this storage object
	  @returns {boolean} - a status indicating if the folder is deleted by the storage engine
	*/
	this.delete = function () {
		return proxy.delete ();
	};
	
	/**	
	  Delete the content of this storage object
	*/
	this.truncate = function () {
		return proxy.truncate ();
	};

	/**	
	  Update the content of this storage object. Doesn't apply to folders
	  @param {InputStream} - the new name
	  @param {boolean} - true to append to the existing content, false to overwrite 
	  
	  @returns {integer} - the number of bytes written to the storage object
	*/
	this.update = function (payload, append) {
		if (typeof append === 'undefined' || append === null) {
			append = false;
		}
		return proxy.update (payload.proxy, append);
	};

	/**	
	  Copy or Move a storage object to a specific folder
	  @param {Folder} - the destination folder
	  @param {boolean} - true: to move this storage object. false to create a copy of it
	*/
	this.copy = function (folder, move) {
		if (!folder || !folder.proxy) {
			throw 'folder should be a valid storage folder';
		}
		if (typeof move === 'undefined' || move === null) {
			move = false;
		}
		proxy.copy (folder.proxy, move);
	};

	/**	
	  Get a Json Object from this storage object. If it's a folder, returns direct children.
	  @param {Function|string} [filter] - a filter to apply when listing children
	  @param {boolean} [fetchChildren] - fetch also children

	  @returns {Object} a json object
	*/
	this.toJson = function (filter, fetchChildren) {
		if (typeof fetchChildren == 'undefined') {
			fetchChildren = true;
		}
		return proxy.toJson (this._guessFilter (filter), fetchChildren);
	};
	
	/**	
	  Get a ApiOutput from this storage object. 
	  @param {Function|string} [filter] - a filter to apply when listing children
	  @param {string} [altName] - the name of the stream source. If not provided, the name of the storage object will be used
	  @param {string} [altContentType] - the contentType of the stream source. If not provided, the contentType of the storage object will be used
	  
	  @returns {ApiOutput} an ApiOutput object
	*/
	this.toOutput = function (filter, altName, altContentType) {
		return new ApiOutput ( proxy.toOutput (this._guessFilter (filter), altName||null, altContentType||null) );
	};
	
	/**	
	  Get a stream source from this storage object. Undefined if this storage object is a folder
	  @param {string} [altName] - the name of the stream source. If not provided, the name of the storage object will be used
	  @param {string} [altContentType] - the contentType of the stream source. If not provided, the contentType of the storage object will be used
	  
	  @returns {ApiStreamSource} an ApiStreamSource object
	*/
	this.toStreamSource = function (altName, altContentType) {
		return new ApiStreamSource (proxy.toStreamSource (altName||null, altContentType||null));
	};
	
	/**	
	  Get the writer of this storage object. Undefined if it's a folder 
	  @param {ApiContext|ApiRequest} - the context in which this function is called
	  
	  @returns {OutputStream} an OutputStream object
	*/
	this.writer = function (context) {
		if (!context) {
			throw 'context not found';
		}
		return new OutputStream (proxy.writer (context.proxy));
	};
	
	/**	
	  Get the reader of this storage object. Undefined if it's a folder 
	  @param {ApiContext|ApiRequest} - the context in which this function is called
	  
	  @returns {InputStream} an InputStream object
	*/
	this.reader = function (context) {
		if (!context) {
			throw 'context not found';
		}
		return new InputStream (proxy.reader (context.proxy));
	};
	
	this._guessFilter = function (filter) {
		if (!filter) {
			return null;
		}
		var JFilter;
		var token;
		if (Lang.isString (filter)) {
			filter = filter.trim ();
			if (filter == '') {
				return null;
			}
			var lFilter = filter.toLowerCase ();
			if (lFilter == 'folders') {
				JFilter = JC_OnlyFolder_Filter;
			} else if (lFilter == 'files') {
				JFilter = JC_OnlyFiles_Filter;
			} else if (lFilter == 'empty') {
				JFilter = JC_EmptyFolders_Filter;
			} else if (lFilter == 'notempty') {
				JFilter = JC_NotEmptyFolders_Filter;
			} else if (lFilter.indexOf ('start:') == 0) {
				token = filter.substring (6);
				JFilter = JC_StartsWith_Filter;
			} else if (lFilter.indexOf ('end:') == 0) {
				token = filter.substring (4);
				JFilter = JC_EndsWith_Filter;
			} else if (lFilter.indexOf ('contain:') == 0) {
				token = filter.substring (9);
				JFilter = JC_Contains_Filter;
			} else if (filter.indexOf ('exp:') == 0) {
				token = filter.substring (4);
				JFilter = JC_Expression_Filter;
			} 
		} else {
			JFilter = Java.extend (JC_Folder_Filter, {
				accept: function (jso) {
					return filter (new StorageObject (jso));
				}
			});
		}
		return JFilter ? (token ? new JFilter (token) : new JFilter ()) : null;
	};

};