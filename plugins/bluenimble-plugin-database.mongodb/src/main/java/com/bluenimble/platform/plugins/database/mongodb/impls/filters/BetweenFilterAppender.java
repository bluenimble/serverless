package com.bluenimble.platform.plugins.database.mongodb.impls.filters;

import com.bluenimble.platform.db.query.Condition;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.Operators.OperatorSpec;
import com.mongodb.BasicDBObject;

public class BetweenFilterAppender implements FilterAppender {

	@Override
	public BasicDBObject append (Condition condition, BasicDBObject criteria) {
		OperatorSpec spec = Operators.get (condition.operator ());
		if (spec == null) {
			return null;
		}
		
		if (spec.oposite) {
			BasicDBObject not = new BasicDBObject ();
			not.put (Operators.Gt, condition.value ());
			not.put (Operators.Lt, condition.value ());
			criteria.put (Operators.Not, not);
		} else {
			criteria.put (Operators.Gt, condition.value ());
			criteria.put (Operators.Lt, condition.value ());
		}
		
		return null;
		
	}

}
