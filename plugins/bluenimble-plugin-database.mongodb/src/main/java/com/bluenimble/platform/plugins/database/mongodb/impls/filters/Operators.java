package com.bluenimble.platform.plugins.database.mongodb.impls.filters;

import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.db.query.Query.Operator;

public class Operators {
	
	static final String Unknown = "$_";
	static final String Not 	= "$not";
	static final String Lt 		= "$lt";
	static final String Gt 		= "$gt";
	static final String Regex 	= "$regex";
	static final String Options = "$options";
	static final String Text 	= "$text";
	static final String Search 	= "$search";
	
	static class OperatorSpec {
		String operator;
		boolean oposite;
		
		OperatorSpec (String operator, boolean oposite) {
			this.operator 	= operator;
			this.oposite 	= oposite;
		}
		
		OperatorSpec (String operator) {
			this.operator 	= operator;
		}
		
	}
	
	private static final Map<Operator, OperatorSpec> OperatorsMap = new HashMap<Operator, OperatorSpec> ();
	static {
		OperatorsMap.put (Operator.eq, new OperatorSpec ("$eq"));
		OperatorsMap.put (Operator.neq, new OperatorSpec ("$ne"));
		OperatorsMap.put (Operator.gt, new OperatorSpec (Gt));
		OperatorsMap.put (Operator.gte, new OperatorSpec ("$gte"));
		OperatorsMap.put (Operator.lt, new OperatorSpec (Lt));
		OperatorsMap.put (Operator.lte, new OperatorSpec ("$lte"));
		OperatorsMap.put (Operator.like, new OperatorSpec ("$regex"));
		OperatorsMap.put (Operator.nlike, new OperatorSpec ("$regex", true));
		OperatorsMap.put (Operator.in, new OperatorSpec ("$in"));
		OperatorsMap.put (Operator.nin, new OperatorSpec ("$nin"));
		OperatorsMap.put (Operator.btw, new OperatorSpec (Unknown));
		OperatorsMap.put (Operator.nbtw, new OperatorSpec (Unknown, true));
		OperatorsMap.put (Operator.nil, new OperatorSpec ("$exists", true));
		OperatorsMap.put (Operator.nnil, new OperatorSpec ("$exists", false));
		OperatorsMap.put (Operator.regex, new OperatorSpec (Unknown));
		OperatorsMap.put (Operator.ftq, new OperatorSpec (Unknown));
		OperatorsMap.put (Operator.near, new OperatorSpec (Unknown));
		OperatorsMap.put (Operator.within, new OperatorSpec (Unknown));
	}
	
	static OperatorSpec get (Operator operator) {
		return OperatorsMap.get (operator);
	}
	

}
