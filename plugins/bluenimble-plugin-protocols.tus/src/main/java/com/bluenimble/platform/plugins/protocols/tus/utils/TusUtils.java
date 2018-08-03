package com.bluenimble.platform.plugins.protocols.tus.utils;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;

public class TusUtils {
	
	private static final String 		GetProxy 	= "getProxy";
	
	private static final Class<?> [] 	NoTypes 	= new Class<?> [] {};
	private static final Object [] 		NoArgs 			= new Object [] {};

	public static HttpServletRequest toHttpRequest (ApiRequest request) throws Exception {
		Method getProxy = request.getClass ().getMethod (GetProxy, NoTypes);
		return (HttpServletRequest)getProxy.invoke (request, NoArgs);		
	}
	
	public static HttpServletResponse toHttpResponse (ApiResponse response) throws Exception {
		Method getProxy = response.getClass ().getMethod (GetProxy, NoTypes);
		return (HttpServletResponse)getProxy.invoke (response, NoArgs);		
	}

}
