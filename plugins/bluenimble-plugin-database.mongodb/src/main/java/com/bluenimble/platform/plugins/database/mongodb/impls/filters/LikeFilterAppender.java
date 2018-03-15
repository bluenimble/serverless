package com.bluenimble.platform.plugins.database.mongodb.impls.filters;

import java.util.regex.Pattern;

import com.bluenimble.platform.db.query.Condition;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.Operators.OperatorSpec;
import com.mongodb.BasicDBObject;

public class LikeFilterAppender implements FilterAppender {

	@Override
	public BasicDBObject append (Condition condition, BasicDBObject criteria) {
		OperatorSpec spec = Operators.get (condition.operator ());
		if (spec == null) {
			return null;
		}
		
		if (spec.oposite) {
			criteria.append (Operators.Not, Pattern.compile (Pattern.quote (String.valueOf (condition.value ())), Pattern.CASE_INSENSITIVE));
		} else {
			criteria.append (Operators.Regex, Pattern.quote (String.valueOf (condition.value ())));
			criteria.append (Operators.Options, "i");
		}
		
		return null;
		
	}

}
