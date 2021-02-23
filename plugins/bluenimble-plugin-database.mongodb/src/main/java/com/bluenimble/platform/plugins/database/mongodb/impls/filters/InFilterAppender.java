package com.bluenimble.platform.plugins.database.mongodb.impls.filters;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.plugins.database.mongodb.impls.MongoDatabaseImpl;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.Operators.OperatorSpec;
import com.bluenimble.platform.query.Condition;
import com.mongodb.BasicDBObject;

public class InFilterAppender implements FilterAppender {

	@Override
	public BasicDBObject append (Condition condition, BasicDBObject criteria, Object value) {
		OperatorSpec spec = Operators.get (condition.operator ());
		if (spec == null) {
			return null;
		}
		
		if (!(value instanceof JsonArray)) {
			return null;
		}
		
		String field = condition.field ();
		
		boolean idIsUserField = false;
		// ignore id replacement
		if (field.startsWith (Lang.XMARK)) {
			field = field.substring (1);
			idIsUserField = true;
		}

		if ((field.equals (Database.Fields.Id) || field.endsWith (MongoDatabaseImpl.IdPostfix)) && !idIsUserField) {
			// 
			List<Object> values = new ArrayList<Object> ();
			JsonArray array = (JsonArray)value;
			for (int i = 0; i < array.count (); i++) {
				Object v = array.get (i);
				if (!ObjectId.isValid (String.valueOf (v))) {
					continue;
				}
				values.add (new ObjectId (String.valueOf (v)));
			}
			value = values;
		}
		
		if (spec.oposite) {
			criteria.put (Operators.Not, new BasicDBObject (spec.operator, value));
		} else {
			criteria.put (spec.operator, value);
		}
		
		return null;
				
	}

}
