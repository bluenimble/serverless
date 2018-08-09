package com.bluenimble.platform.messenger.impls.socketio.utils;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class MessageUtils {

	public static JsonObject toJson (JSONObject object) throws JSONException {
		if (object == null || JSONObject.NULL.equals (object)) {
			return null;
		}
		JsonObject json = new JsonObject ();

		@SuppressWarnings("unchecked")
		Iterator<String> keysItr = object.keys ();
		while (keysItr.hasNext()) {
			String key = keysItr.next();
			Object value = object.get(key);

			if (value instanceof JSONArray) {
				value = toArray ((JSONArray) value);
			} else if (value instanceof JSONObject) {
				value = toJson ((JSONObject) value);
			}
			
			json.set (key, value);
		}
		return json;
	}

	public static JsonArray toArray (JSONArray array) throws JSONException {
		JsonArray list = new JsonArray ();
		for (int i = 0; i < array.length(); i++) {
			Object value = array.get(i);
			if (value instanceof JSONArray) {
				value = toArray ((JSONArray) value);
			} else if (value instanceof JSONObject) {
				value = toJson ((JSONObject) value);
			}
			
			list.add (value);
		}
		return list;
	}

}
