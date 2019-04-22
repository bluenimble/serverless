package com.bluenimble.platform.plugins.database.mongodb.impls.filters;

import com.bluenimble.platform.query.Condition;
import com.mongodb.BasicDBObject;

public interface FilterAppender {
	
	BasicDBObject append (Condition condition, BasicDBObject critera, Object value);
	
}
