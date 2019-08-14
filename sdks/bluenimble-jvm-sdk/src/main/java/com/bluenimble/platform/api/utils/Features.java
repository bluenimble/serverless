package com.bluenimble.platform.api.utils;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.json.JsonObject;

public class Features {
	
	public static <T> T get (Api api, Class<T> type, String feature, ApiContext context) {
		if (Lang.isNullOrEmpty (feature)) {
			feature = ApiSpace.Features.Default;
		}
		
		String featureType = null;
		Feature aFeature = type.getAnnotation (Feature.class);
		if (aFeature != null) {
			featureType = aFeature.name ();
		}
		
		if (Lang.isNullOrEmpty (featureType)) {
			return api.space ().feature (type, feature, context);
		}
		
		String featureName = feature;
		int lastIndexOfSharp = feature.lastIndexOf (Lang.SHARP);
		if (lastIndexOfSharp > -1) {
			featureName = feature.substring (0, lastIndexOfSharp);
		}
		JsonObject defautls = Json.getObject (api.getFeatures (), featureType);
		if (!Json.isNullOrEmpty (defautls)) {
			featureName = Json.getString (defautls, featureName, featureName);
		}
		if (lastIndexOfSharp > -1) {
			featureName += Lang.SHARP + feature.substring (lastIndexOfSharp + 1);
		}
		
		return api.space ().feature (type, featureName, context);
	}
	
}
