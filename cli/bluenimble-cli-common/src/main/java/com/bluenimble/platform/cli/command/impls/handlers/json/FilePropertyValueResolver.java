package com.bluenimble.platform.cli.command.impls.handlers.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.impls.handlers.PropertyValueResolver;

public class FilePropertyValueResolver implements PropertyValueResolver {

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
			return IOUtils.toString (is);
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (is);
		}
	}

}
