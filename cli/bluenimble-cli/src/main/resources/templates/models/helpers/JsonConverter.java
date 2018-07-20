package helpers;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonException;
import com.bluenimble.platform.json.JsonObject;

@Converter
public class JsonConverter implements AttributeConverter<JsonObject, String> {

	/**
	 * Convert Json object to a String
	 */
	@Override
	public String convertToDatabaseColumn (JsonObject json) {
		if (json == null) {
			return null;
		}
		return json.toString ();
	}

	/**
	 * Convert a String to a Json object
	 */
	@Override
	public JsonObject convertToEntityAttribute (String jsonString) {
		if (Lang.isNullOrEmpty (jsonString)) {
			return null;
		}
		try {
			return new JsonObject (jsonString);
		} catch (JsonException ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
	}

}