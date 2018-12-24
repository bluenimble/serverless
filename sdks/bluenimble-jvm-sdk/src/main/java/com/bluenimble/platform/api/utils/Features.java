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
		if (!Lang.isNullOrEmpty (featureType)) {
			JsonObject defautls = Json.getObject (api.getFeatures (), featureType);
			if (!Json.isNullOrEmpty (defautls)) {
				feature = Json.getString (defautls, feature, feature);
			}
		}
		
		return api.space ().feature (type, feature, context);
	}
	
}
