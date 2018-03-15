package com.bluenimble.platform.plugins.database.mongodb.impls.filters;

import com.bluenimble.platform.db.query.Condition;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.Operators.OperatorSpec;
import com.mongodb.BasicDBObject;

public class DefaultFilterAppender implements FilterAppender {

	@Override
	public BasicDBObject append (Condition condition, BasicDBObject criteria) {
		OperatorSpec spec = Operators.get (condition.operator ());
		if (spec == null) {
			return null;
		}
		
		if (spec.oposite) {
			criteria.put (Operators.Not, new BasicDBObject (spec.operator, condition.value ()));
		} else {
			criteria.put (spec.operator, condition.value ());
		}
		
		return null;
				
	}

}
