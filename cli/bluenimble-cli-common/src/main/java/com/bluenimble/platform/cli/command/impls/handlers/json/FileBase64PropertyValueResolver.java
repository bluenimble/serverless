package com.bluenimble.platform.cli.command.impls.handlers.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.impls.handlers.PropertyValueResolver;
import com.bluenimble.platform.encoding.Base64;

public class FileBase64PropertyValueResolver implements PropertyValueResolver {

	@Override
	public Object lookup (Tool tool, String value) throws CommandExecutionException {
		
		String path = value.trim ();
		
		if (path.startsWith (Lang.TILDE + File.separator)) {
			path = System.getProperty ("user.home") + path.substring (1);
		}
		
		File file = new File (path);
		if (!file.exists ()) {
			throw new CommandExecutionException ("file " + value + " not found");
		}
		
		InputStream is = null;
		try {
			is = new FileInputStream (file);
			byte [] bytes = IOUtils.toByteArray (is);
			return new String (Base64.encodeBase64 (bytes), Encodings.UTF8);
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (is);
		}
	}

}
