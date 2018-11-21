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
package com.bluenimble.platform.server.interceptor.impls;

import java.util.Iterator;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiStatus;
import com.bluenimble.platform.api.impls.AbstractApiRequest;
import com.bluenimble.platform.api.impls.ApiImpl;
import com.bluenimble.platform.api.impls.ContainerApiRequest;
import com.bluenimble.platform.api.impls.ContainerApiResponse;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.media.ApiMediaProcessor;
import com.bluenimble.platform.api.security.ApiAuthenticationException;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.security.ApiConsumerResolver;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.api.validation.ApiServiceValidatorException;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.interceptor.ApiInterceptor;
import com.bluenimble.platform.server.security.impls.DefaultApiConsumer;
import com.bluenimble.platform.server.tracking.ServerRequestTrack;
import com.bluenimble.platform.server.utils.ApiUtils;
import com.bluenimble.platform.server.utils.ConfigKeys;
import com.bluenimble.platform.server.utils.Messages;

public class DefaultApiInterceptor implements ApiInterceptor {
	
	private static final long serialVersionUID = 904004854168178129L;
	
	private ApiServer server;
	
	@Override
	public void init (ApiServer server) {
		this.server = server;
	}
	
	@Override
	public void intercept (Api api, ApiRequest request, ApiResponse response) {
		
		logDebug (api, "<" + request.getId () + "> Process Request \n" + request.toString ());
		
		ServerRequestTrack track = 
				server.getRequestTracker (Json.getString (api.getTracking (), Api.Spec.Tracking.Tracker)).create (api, request);
		
		request.track (track);
		
		response.set (ApiHeaders.NodeID, 		Json.getString (request.getNode (), ApiRequest.Fields.Node.Id));
		response.set (ApiHeaders.NodeType, 		Json.getString (request.getNode (), ApiRequest.Fields.Node.Type));
		response.set (ApiHeaders.NodeVersion, 	Json.getString (request.getNode (), ApiRequest.Fields.Node.Version));
		
		ApiMediaProcessor mediaProcessor = null;

		ApiConsumer consumer = null;
		ApiService service = null;
		try {
			
			// api life cycle - onRequest
			api.getSpi ().onRequest (api, request, response);
			
			// resolve service
			service = ((ApiImpl)api).lockup (request);
	
			ApiResponse.Status 	notFoundStatus 	= null;
			String 				notFoundMessage = null;
			
			if (service == null) {
				notFoundStatus 	= ApiResponse.NOT_FOUND;
				notFoundMessage = api.message (request.getLang (), Messages.ServiceNotFound, request.getVerb ().name () + Lang.SPACE + request.getPath ());
			} else if (service.status () != ApiStatus.Running) {
				notFoundStatus 	= ApiResponse.SERVICE_UNAVAILABLE;
				notFoundMessage = api.message (request.getLang (), Messages.ServiceNotAvailable, service.getName ());
			}
			
			if (notFoundStatus != null) {
				if (response instanceof ContainerApiResponse) {
					((ContainerApiResponse)response).setException (
						new ApiServiceExecutionException (notFoundMessage).status (notFoundStatus)
					);
				} else {
					response.error (
						notFoundStatus, 
						notFoundMessage
					);
					writeError (mediaProcessor, api, null, null, request, response); 
				}
				track.finish (
					(JsonObject)	
					new JsonObject ().set (
						ApiResponse.Error.Code, notFoundStatus.getCode ()
					).set (
						ApiResponse.Error.Message, notFoundMessage
					)
				);
				return;
			}
			
			((AbstractApiRequest)request).setService (service);
			
			// Lookup media processor
			mediaProcessor = api.lockupMediaProcessor (request, service);
			
			track.update (service);
	
			logInfo (api, "<" + request.getId () + "> Using service " + service.getVerb () + Lang.SPACE + Json.getString (service.toJson (), ApiService.Spec.Endpoint) + Lang.SPACE + Lang.PARENTH_OPEN + service.getName () + Lang.PARENTH_CLOSE);
			
			// api life cycle - onService
			api.getSpi ().onService (api, service, request, response);
			
			logInfo (api, "<" + request.getId () + "> Interceptor will use media.processor [" + mediaProcessor.getClass ().getSimpleName () + "]");
			
			JsonObject apiSecMethods = Json.getObject (api.getSecurity (), Api.Spec.Security.Schemes);
			if (apiSecMethods == null) {
				apiSecMethods = JsonObject.Blank;
			}
			
			JsonArray serviceSecMethods = Json.getArray (service.getSecurity (), ApiService.Spec.Security.Schemes);
			
			ApiConsumerResolver resolver = null;
			try {
				Iterator<String> rKeys = apiSecMethods.keys ();
				if (rKeys != null) {
					while (rKeys.hasNext ()) {
						String resolverName = rKeys.next ();
						if (serviceSecMethods != null && !serviceSecMethods.contains (resolverName)) {
							continue;
						}
						ApiConsumerResolver r = server.getConsumerResolver (resolverName);
						if (r == null) {
							continue;
						}
						consumer = r.resolve (api, service, request);
						if (consumer != null) {
							resolver = r;
							break;
						}
					}				
				}
				if (consumer == null) {
					consumer = new DefaultApiConsumer (ApiConsumer.Type.Unknown);
				}
				
				api.getSpi ().findConsumer (api, service, request, consumer);
				
				if (resolver != null) {
					resolver.authorize (api, service, request, consumer);
				}
				
			} catch (ApiAuthenticationException e) {
				if (response instanceof ContainerApiResponse) {
					((ContainerApiResponse)response).setException (
						new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.UNAUTHORIZED)
					);
				} else {
					response.error (ApiResponse.UNAUTHORIZED, e.getMessage ());
					writeError (mediaProcessor, api, consumer, service, request, response); 
				}
				track.finish (
					(JsonObject)	
					new JsonObject ().set (
						ApiResponse.Error.Code, ApiResponse.UNAUTHORIZED.getCode ()
					).set (
						ApiResponse.Error.Message, e.getMessage ()
					)
				);
				return;
			}

			try {
				server.getServiceValidator ().validate (api, Json.getObject (service.toJson (), ApiService.Spec.Spec), consumer, request);
			} catch (ApiServiceValidatorException e) {
				if (response instanceof ContainerApiResponse) {
					((ContainerApiResponse)response).setException (
						new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.UNPROCESSABLE_ENTITY)
					);
				} else {
					writeValidationError (api, consumer, service, request, response, mediaProcessor, e);
				}
				Object error = null;
				if (e.getFeedback () != null) {
					error = e.getFeedback ();
				} else {
					error = e.getMessage ();
				}
				track.finish (
					(JsonObject)	
					new JsonObject ().set (
						ApiResponse.Error.Code, ApiResponse.UNPROCESSABLE_ENTITY.getCode ()
					).set (
						ApiResponse.Error.Message, error
					)
				);
				return;
			}
			
			Exception executeError = null;
			ApiOutput output = null;
			
			JsonObject mock = Json.getObject (service.toJson (), ApiService.Spec.Mock);
			if (mock != null && Json.getBoolean (mock, ConfigKeys.Enabled, false)) {
				output = new JsonApiOutput (Json.getObject (mock, ApiService.Spec.Output));
				logInfo (api, "<" + request.getId () + "> Service using mock output");
			} else {
				try {
					// api life cycle - onExecute
					api.getSpi ().onExecute (api, consumer, service, request, response);
					
					output = service.getSpi ().execute (api, consumer, request, response);
					
					// api life cycle - afterExecute
					api.getSpi ().afterExecute (api, consumer, service, request, response);
				} catch (Exception ex) {
					executeError = ex;
				}
			}
			
			// call finish with error status
			request.finish (executeError != null);
			
			// throw error
			if (executeError != null) {
				throw executeError;
			}
			
			if (request instanceof ContainerApiRequest) {
				request.set (ApiRequest.Output, output);
			} else {
				response.set (ApiHeaders.ExecutionTime, (System.currentTimeMillis () - request.getTimestamp ().getTime ()));
				
				if (response.isCommitted ()) {
					logInfo (api, "<" + request.getId () + "> Response already committed. No media processing required");
					long time = System.currentTimeMillis () - request.getTimestamp ().getTime ();
					track.finish (
						(JsonObject)	
						new JsonObject ().set (
							ApiResponse.Error.Code, ApiResponse.OK.getCode ()
						).set (
							ApiResponse.Error.Message, time
						)
					);
					logInfo (api, " <" + request.getId () + "> ExecTime-Cancel: Service " + service.getVerb () + ":" + Json.getString (service.toJson (), ApiService.Spec.Endpoint) + " - Time " + time + " millis");
					return;
				}
			
				mediaProcessor.process (api, service, consumer, output, request, response);
			}
			
			int iStatus = ApiResponse.OK.getCode ();
			ApiResponse.Status status = response.getStatus ();
			if (status != null) {
				iStatus = status.getCode ();
			}
			
			long time = System.currentTimeMillis () - request.getTimestamp ().getTime ();
			
			track.finish (
				(JsonObject)	
				new JsonObject ().set (
					ApiResponse.Error.Code, iStatus
				).set (
					ApiResponse.Error.Message, time
				)
			);
			
			logInfo (api, "<" + request.getId () + "> ExecTime-Success: Service " + service.getVerb () + ":" + Json.getString (service.toJson (), ApiService.Spec.Endpoint) + " - Time " + time + " millis");
			
		} catch (Throwable th) {
			ApiResponse.Status status = null;
			
			if (th instanceof ApiServiceExecutionException) {
				status = ((ApiServiceExecutionException)th).status ();
			} 
			if (status == null) {
				status = ApiResponse.INTERNAL_SERVER_ERROR;
			}
			
			if (response instanceof ContainerApiResponse) {
				if (th instanceof ApiServiceExecutionException) {
					((ContainerApiResponse)response).setException ((ApiServiceExecutionException)th);
				} else {
					((ContainerApiResponse)response).setException (new ApiServiceExecutionException (th.getMessage (), th));
				}
				
				track.finish (
					(JsonObject)Lang.toError (th).set (ApiResponse.Error.Code, status.getCode ())
				);
			} else {
				boolean isValidationError = false;
				if (th instanceof ApiServiceExecutionException) {
					Throwable rootCause = ((ApiServiceExecutionException)th).getRootCause ();
					if (rootCause instanceof ApiServiceValidatorException) {
						ApiServiceValidatorException vex = (ApiServiceValidatorException)rootCause;
						isValidationError = true;
						writeValidationError (api, consumer, service, request, response, mediaProcessor, vex);
						
						Object error = null;
						if (vex.getFeedback () != null) {
							error = vex.getFeedback ();
						} else {
							error = vex.getMessage ();
						}
						track.finish (
							(JsonObject)	
							new JsonObject ().set (
								ApiResponse.Error.Code, ApiResponse.UNPROCESSABLE_ENTITY.getCode ()
							).set (
								ApiResponse.Error.Message, error
							)
						);
					} 
				} 
				if (!isValidationError) {
					
					JsonObject oError = Lang.toError (th);
					
					// logError (api, "<" + request.getId () + "> - Execute Service / Media Processing - caused an error\n" + oError.toString (), null);
										
					response.error (status, new Object [] { oError.get (ApiResponse.Error.Message), oError.get (ApiResponse.Error.Trace)});
					writeError (mediaProcessor, api, consumer, service, request, response);
					
					track.finish (
						(JsonObject)oError.set (ApiResponse.Error.Code, status.getCode ())
					);
				}
			}
			
		} finally {
			request.destroy ();
		}
		
	}
	
	private void writeValidationError (Api api, ApiConsumer consumer, ApiService service, ApiRequest request, ApiResponse response, 
			ApiMediaProcessor mediaProcessor, ApiServiceValidatorException e) {
		Object err = null;
		if (e.getFeedback () != null) {
			err = e.getFeedback ();
		} else {
			err = e.getMessage ();
		}
		response.error (e.status (), err);
		writeError (mediaProcessor, api, consumer, service, request, response); 
	}

	private void logDebug (Api api, Object o) {
		api.tracer ().log (Level.Debug, o);
	}
	
	private void logInfo (Api api, Object o) {
		api.tracer ().log (Level.Info, o);
	}
	
	private void logError (Api api, Object o, Throwable th) {
		api.tracer ().log (Level.Error, o, th);
	}
	
	private void writeError (ApiMediaProcessor mediaProcessor, Api api, ApiConsumer consumer, ApiService service, ApiRequest request, ApiResponse response) {

		if (service != null) {
			api.getSpi ().onError (api, service, consumer, request, response, response.getError ());
		}
		
		response.set (ApiHeaders.ExecutionTime, (System.currentTimeMillis () - request.getTimestamp ().getTime ()));

		if (mediaProcessor == null) {
			response.set (ApiHeaders.ContentType, ApiContentTypes.Json);
			try {
				response.write (response.getError ());
				response.close ();
			} catch (Exception iox) {
				logError (api, Lang.BLANK, iox);
			}
			ApiUtils.logError (api, response, server.tracer ());
			long time = System.currentTimeMillis () - request.getTimestamp ().getTime ();
			logInfo (api, "<" + request.getId () + "> ExecTime-Error: " + (service != null ? service.getVerb () + ":" + Json.getString (service.toJson (), ApiService.Spec.Endpoint) : "Unknown Service") + " - ExecutionTime " + time + " millis");
			return;
		}
			
		try {
			logError (api, "<" + request.getId () + "> Send Error Response\n" + response.getError ().toString (2, false), null);
			mediaProcessor.process (api, service, consumer, null, request, response);
		} catch (Exception mex) {
			logError (api, "\tMediaProcessor - Writing Error - caused an error\n", mex);
			response.error (ApiResponse.INTERNAL_SERVER_ERROR, Lang.toMessage (mex));
			try {
				response.write (response.getError ());
				response.close ();
			} catch (Exception iox) {
				logError (api, Lang.BLANK, iox);
			}
		} finally {
			long time = System.currentTimeMillis () - request.getTimestamp ().getTime ();
			logInfo (api, "<" + request.getId () + "> ExecTime-Error: " + (service != null ? service.getVerb () + ":" + Json.getString (service.toJson (), ApiService.Spec.Endpoint) : "Unknown Service") + " - ExecutionTime " + time + " millis");
		}
		
	}

}
