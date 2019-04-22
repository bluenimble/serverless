package com.bluenimble.platform.plugins.database.mongodb.impls.filters;

import com.bluenimble.platform.plugins.database.mongodb.impls.filters.Operators.OperatorSpec;
import com.bluenimble.platform.query.Condition;
import com.mongodb.BasicDBObject;

public class TextFilterAppender implements FilterAppender {

	@Override
	public BasicDBObject append (Condition condition, BasicDBObject criteria, Object value) {
		OperatorSpec spec = Operators.get (condition.operator ());
		if (spec == null) {
			return null;
		}

		BasicDBObject filter = new BasicDBObject ();

		BasicDBObject text = new BasicDBObject ();
		text.append (Operators.Search, value);
		
		filter.append (Operators.Text, text);
		
		return filter;
	}

}
