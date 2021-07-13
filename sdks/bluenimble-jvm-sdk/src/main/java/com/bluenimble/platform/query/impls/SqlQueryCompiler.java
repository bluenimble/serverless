/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluenimble.platform.query.impls;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.query.CompiledQuery;
import com.bluenimble.platform.query.Condition;
import com.bluenimble.platform.query.Filter;
import com.bluenimble.platform.query.GroupBy;
import com.bluenimble.platform.query.OrderBy;
import com.bluenimble.platform.query.OrderByField;
import com.bluenimble.platform.query.Query;
import com.bluenimble.platform.query.Query.Conjunction;
import com.bluenimble.platform.query.Query.Operator;
import com.bluenimble.platform.query.QueryException;
import com.bluenimble.platform.query.Select;

public class SqlQueryCompiler extends EventedQueryCompiler {

	private static final long serialVersionUID = -721087118950354168L;
	
	private static final Set<Class<?>> Primitives = new HashSet<Class<?>>();
	static {
		Primitives.add (Boolean.class);
		Primitives.add (Boolean.TYPE);
		Primitives.add (Short.class);
		Primitives.add (Short.TYPE);
		Primitives.add (Integer.class);
		Primitives.add (Integer.TYPE);
		Primitives.add (Long.class);
		Primitives.add (Long.TYPE);
		Primitives.add (Float.class);
		Primitives.add (Float.TYPE);
		Primitives.add (Double.class);
		Primitives.add (Double.TYPE);
	}
	
	private static final String					ParamPrefix		= "p";
	
	protected interface Sql {
		String OrderBy 	= "order by";
		String GroupBy 	= "group by";
		String From 	= "from";
	}
	
	private static final Map<Operator, String> 	OperatorsMap 	= new HashMap<Operator, String> ();
	static {
		OperatorsMap.put (Operator.eq, 		"=");
		OperatorsMap.put (Operator.neq, 	"<>");
		OperatorsMap.put (Operator.gt, 		">");
		OperatorsMap.put (Operator.lt, 		"<");
		OperatorsMap.put (Operator.gte, 	">=");
		OperatorsMap.put (Operator.lte, 	"<=");
		OperatorsMap.put (Operator.like, 	"like");
		OperatorsMap.put (Operator.nlike, 	"not like");
		OperatorsMap.put (Operator.btw, 	"between");
		OperatorsMap.put (Operator.nbtw, 	"not between");
		OperatorsMap.put (Operator.all, 	"in");
		OperatorsMap.put (Operator.in, 		"in");
		OperatorsMap.put (Operator.nin, 	"not in");
		OperatorsMap.put (Operator.nil, 	"is null");
		OperatorsMap.put (Operator.nnil, 	"is not null");
	}
	
	protected StringBuilder 		buff = new StringBuilder ();
	protected Map<String, Object>	bindings;
	
	private int						counter;
		
	protected Query.Construct		dml;
	private Query 					query;

	public SqlQueryCompiler (Query.Construct dml) {
		this (dml, -1);
	}
	
	public SqlQueryCompiler (Query.Construct dml, int counter) {
		this.dml 		= dml;
		this.counter 	= counter;
	}
	
	@Override
	protected void onQuery (Timing timing, Query query) throws QueryException {
		this.query 		= query;
	}

	@Override
	protected void onSelect (Timing timing, Select select)
			throws QueryException {
		if (Timing.start.equals (timing)) {
			buff.append (dml.name ());
		} else {
			buff.append (Lang.SPACE).append (Sql.From).append (Lang.SPACE);
			entity ();
		} 
	}

	@Override
	protected void onSelectField (String field, int count, int index)
			throws QueryException {
		if (!Query.Construct.select.equals (dml)) {
			return;
		}
		buff.append (Lang.SPACE); field (field);
		if (count == (index + 1)) {
			return;
		}
		buff.append (Lang.COMMA);
	}

	@Override
	protected void onConjunction (Timing timing, Conjunction conjunction, int index) throws QueryException {
		switch (timing) {
			case start:
				if (index > 0) {
					if (conjunction == null) {
						conjunction = Conjunction.and;
					}
					buff.append (Lang.SPACE).append (conjunction.name ()).append (Lang.SPACE);
				}
				buff.append (Lang.PARENTH_OPEN);
				break;
			case end:
				buff.append (Lang.PARENTH_CLOSE);
				break;
			default:
				break;	
		}
	}

	@Override
	protected void onFilter (Timing timing, Filter filter, Conjunction conjunction, boolean isWhere)
			throws QueryException {
		
		if (filter == null || filter.isEmpty ()) {
			return;
		}
		
		switch (timing) {
			case start:
				if (isWhere) {
					buff.append (Lang.SPACE).append (Query.Construct.where.name ()).append (Lang.SPACE); 
				} else {
					buff.append (Lang.SPACE);
					if (conjunction != null) {
						buff.append (conjunction.name ()).append (Lang.SPACE);
					}
					buff.append (Lang.PARENTH_OPEN); 
				}
				break;
	
			case end:
				if (!isWhere) {
					buff.append (Lang.PARENTH_CLOSE); 
				}
				break;
	
			default:
				break;
		}
	}
	
	@Override
	protected void onCondition (Condition condition, Conjunction conjunction, int index)
			throws QueryException {
		
		if (index > 0) {
			if (conjunction == null) {
				conjunction = Conjunction.and;
			}
			buff.append (Lang.SPACE).append (conjunction.name ()).append (Lang.SPACE);
		}

		field (condition.field ());
		buff.append (Lang.SPACE).append (operatorFor (condition.operator ())).append (Lang.SPACE);
		if (Operator.nil.equals (condition.operator ()) || Operator.nnil.equals (condition.operator ())) {
			return;
		}
		
		if (condition.value () == null) {
			return;
		}
		
		Object value = condition.value ();
		
		if (Operator.all.equals (condition.operator ()) || Operator.in.equals (condition.operator ()) || Operator.nin.equals (condition.operator ())) {
			if (List.class.isAssignableFrom (value.getClass ())) {
				@SuppressWarnings("unchecked")
				List<Object> values = (List<Object>)value;
				if (values.isEmpty ()) {
					buff.append (Lang.ARRAY_OPEN).append (Lang.ARRAY_CLOSE);
					return;
				}
				buff.append (Lang.ARRAY_OPEN);
				for (int i = 0; i < values.size (); i++) {
					Object o = values.get (i);
					// process
					String p = bind (o);
					if (p != null) {
						buff.append (Lang.COLON).append (p);
					} else {
						valueOf (condition, o);
					}
					if (i + 1 != values.size ()) {
						buff.append (Lang.COMMA);
					}
				}
				buff.append (Lang.ARRAY_CLOSE);
			} else if (Query.class.isAssignableFrom (value.getClass ())) {
				// process sub query
				CompiledQuery qc = new SqlQueryCompiler (Query.Construct.select, counter).compile ((Query)value);
				if (qc.bindings () != null) {
					bindings.putAll (qc.bindings ());
				}
				buff.append (Lang.PARENTH_OPEN).append (qc.query ()).append (Lang.PARENTH_CLOSE);
			}
			return;
		} 
		
		String parameter = bind (value);
		if (parameter != null) {
			buff.append (Lang.COLON).append (parameter);
		} else {
			valueOf (condition, value);
		}
		
	}

	protected String operatorFor (Operator operator) {
		return OperatorsMap.get (operator);
	}

	@Override
	protected void onOrderBy (Timing timing, OrderBy orderBy)
			throws QueryException {
		if (orderBy == null || orderBy.isEmpty ()) {
			return;
		}
		
		if (Timing.end.equals (timing)) {
			return;
		}
		buff.append (Lang.SPACE).append (Sql.OrderBy);
	}

	@Override
	protected void onOrderByField (OrderByField orderBy, int count, int index)
			throws QueryException {
		buff.append (Lang.SPACE); field (orderBy.field ());
		buff.append (Lang.SPACE).append (orderBy.direction ().name ());
		if (count == (index + 1)) {
			return;
		}
		buff.append (Lang.COMMA);
	}

	@Override
	protected void onGroupBy (Timing timing, GroupBy groupBy)
			throws QueryException {
		if (groupBy == null || groupBy.isEmpty ()) {
			return;
		}
		if (Timing.end.equals (timing)) {
			return;
		}
		buff.append (Lang.SPACE).append (Sql.GroupBy);
	}

	@Override
	protected void onGroupByField (String field, int count, int index)
			throws QueryException {
		buff.append (Lang.SPACE); field (field);
		if (count == (index + 1)) {
			return;
		}
		buff.append (Lang.COMMA);
	}
	
	@Override
	protected CompiledQuery done () throws QueryException {
		return new CompiledQuery () {
			@Override
			public String query () {
				String q = buff.toString ();
				buff.setLength (0);
				return q;
			}
			
			@Override
			public Map<String, Object> bindings () {
				if (query.bindings () == null) {
					return bindings;
				}
				return query.bindings ();
			}
		};
	}
	
	protected String bind (Object value) {
		if (query.bindings () != null) {
			return null;
		}
		
		counter++;
		String parameter = ParamPrefix + (counter);

		if (bindings == null) {
			bindings = new HashMap<String, Object> ();
		}
		
		if (value instanceof LocalDateTime) {
			LocalDateTime ldt = ((LocalDateTime)value);
			value = Date.from (ldt.atZone (ZoneOffset.UTC).toInstant ());
		}
		
		bindings.put (parameter, value);
		
		return parameter;
	}

	protected void valueOf (Condition condition, Object value) {
		if (value == null) {
			buff.append (Lang.NULL);
			return;
		}
		if (Primitives.contains (value.getClass ())) {
			buff.append (value);
			return;
		}
		String sValue = String.valueOf (value);
		if (sValue.startsWith (Lang.COLON)) {
			buff.append (sValue);
			return;
		}
		if (!condition.isRaw ()) {
			buff.append (Lang.APOS);
		}
		buff.append (Lang.replace (sValue, Lang.APOS, Lang.BACKSLASH + Lang.APOS));
		if (!condition.isRaw ()) {
			buff.append (Lang.APOS);
		}
	}
	
	protected void entity () {
		buff.append (query.entity ());
	}
	
	protected void field (String field) {
		buff.append (field);
	}

}
