package com.bluenimble.platform.plugins.database.mongodb.impls.filters;

import com.bluenimble.platform.plugins.database.mongodb.impls.filters.Operators.OperatorSpec;
import com.bluenimble.platform.query.Condition;
import com.mongodb.BasicDBObject;

public class DefaultFilterAppender implements FilterAppender {

	@Override
	public BasicDBObject append (Condition condition, BasicDBObject criteria, Object value) {
		OperatorSpec spec = Operators.get (condition.operator ());
		if (spec == null) {
			return null;
		}
		
		if (spec.oposite) {
			criteria.put (Operators.Not, new BasicDBObject (spec.operator, value));
		} else {
			criteria.put (spec.operator, value);
		}
		
		return null;
				
	}

}
