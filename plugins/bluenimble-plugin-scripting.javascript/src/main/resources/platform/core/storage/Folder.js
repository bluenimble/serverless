/**
  Represents a Folder instance<br/>
  <strong>Do not call constructor directly</strong>. 
  @see Storage
  @see StorageObject
  @class Folder
  @classdesc 
*/

/**
  @constructor
  @access private
  @augments StorageObject
*/
var Folder = function (proxy) {
	StorageObject.call (this, proxy);
};

Folder.prototype = Object.create (StorageObject.prototype);

/**	
  Add a folder under this current folder
  @param {string} - child folder name
  
  @returns {Folder} - the newly created folder
*/
Folder.prototype.addFolder = function (name, ignoreIfExists) {
	if (typeof ignoreIfExists == 'undefined') {
		ignoreIfExists = true;
	}
	return new Folder (this.proxy.add (name, ignoreIfExists));
};

/**	
  Add a new file under this folder
  @param {string} [altName] - the name of the file or an alternative name to the one provided by the ApiStreamSource
  @param {ApiStreamSource} - a stream source
  
  @returns {StorageObject} - the newly created Storage Object
*/
Folder.prototype.addObject = function (altName, streamSource, overwrite) {
	if (typeof overwrite == 'undefined') {
		overwrite = true;
	}
	return new StorageObject (
		this.proxy.add ((streamSource ? streamSource.proxy : null), altName ? altName : null, overwrite)
	);
};

/**	
  Get a storage object under this folder, by path. It could be a file or folder
  @param {string} - the storage object path
  
  @returns {StorageObject} - the Storage Object for the given path
*/
Folder.prototype.get = function (path) {
	var jso = this.proxy.get (path);
	if (!jso) {
		return null;
	}
	if (jso.isFolder ()) {
		return new Folder (jso);
	} else {
		return new StorageObject (jso);
	}
};

/**	
  Count the total number of storage objects under this folder. Includes all sub files and folders
  
  @returns {integer} - the nuber of files and folders under this folder
*/
Folder.prototype.count = function () {
	return this.proxy.count ();
};

/**	
  Check if the object given by path exists under this folder
  @param {string} - the child path

  @returns {boolean} - true/false
*/
Folder.prototype.contains = function (child) {
	return this.proxy.contains (child);
};

/**	
  List all storage objects under this folder
  @param {Function} - a visitor function. 
  @param {Function|string} [filter] - a filter to apply when listing children
  @example
  myFolder.list ( function (so) {
    return so.isFolder;
  });
  @example
  myFolder.list (
	  function (so) {
	  	// do something with the storage object
	  },
      function (so) {
	  	return so.isFolder;
	  }
  );
  @example
  myFolder.list (
	  function (so) {
	  	// do something with the storage object
	  },
      'contains:alpha'
  );
  
  @returns {integer} - the nuber of files and folders under this folder
*/
Folder.prototype.list = function (visitor, filter) {
	if (!visitor) {
		throw 'missing function argument (visitor)';
	}
	var JVisitor = Java.extend (JC_Folder_Visitor, {
		visit: function (jso) {
			visitor (new StorageObject (jso));
		}
	});
	this.proxy.list (new JVisitor (), this._guessFilter (filter));
};
	

