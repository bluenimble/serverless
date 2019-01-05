package com.bluenimble.platform.plugins.database.mongodb.impls.filters;

import java.util.regex.Pattern;

import com.bluenimble.platform.db.query.Condition;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.Operators.OperatorSpec;
import com.mongodb.BasicDBObject;

public class RegexFilterAppender implements FilterAppender {

	@Override
	public BasicDBObject append (Condition condition, BasicDBObject criteria, Object value) {
		OperatorSpec spec = Operators.get (condition.operator ());
		if (spec == null) {
			return null;
		}
		
		String operator = Operators.Regex;
		
		if (spec.oposite) {
			operator = Operators.Not;
		} 
		
		criteria.append (operator, Pattern.compile (String.valueOf (value)));
		
		return null;
		
	}

}
