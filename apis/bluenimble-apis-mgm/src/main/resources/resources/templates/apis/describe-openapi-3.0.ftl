{
	"openapi": "3.0.0",
	"info": {
		"version": "${(output.release.version)!'0.0.0'}",
		"title": "${(output.name)!(output.namespace)!'No Title'}"
		<#if (output.description)??>,"description": "${(output.description)}"</#if>
		<#if (output.release.termsOfService)??>,"termsOfService": "${(output.release.termsOfService)}"</#if>
		<#if (output.release.contact)??>,"contact": ${output.release.contact}</#if>
		<#if (output.release.license)??>,"license": ${output.release.license}</#if>
	}
	<#if (output.release.servers)??>
		,"servers": [{
			<#list output.release.servers as server>
				"url": "${server}"<#if (output.release.servers)?size gt (server?index + 1)>,</#if>
			</#list>
		}],
	</#if>
	
	<#assign components = TemplateTool.newMap() >
	
	<#assign groups = TemplateTool.groupBy (output.space, output.namespace, 'endpoint', 'verb', 'oas')>
	
	<#assign SimplePlaceholders = ['header', 'path', 'query']>
	<#assign MixedPlaceholders = ['body', 'form']>
	
	,"paths": {	
		<#assign allGroupKeys = groups?keys>
		<#list allGroupKeys as key>
			"${key}": {
				<#assign group = groups[key]>
				<#assign groupKeys = group?keys>
				<#list groupKeys as gk>
					<#assign service = group[gk] >
					<#if (service.meta.publish)?? && service.meta.publish == 'false'>
						<#continue>
					</#if>
					<#assign bodyFields = TemplateTool.newList() >
					"${gk}": {
						"description": "${(service.description)!(service.name)!'no description provided'}"
						<#if (service.id)??>
							,"operationId": "${service.id}"
						</#if>
						<#if (service.name)??>
							,"summary": "${service.name}"
						</#if>
						<#if (service.description)??>
							,"description": "${service.description}"
						</#if>
						<#if (service.meta.tags)??>
							,"tags": ["${service.meta.tags?join('","')}"]
						</#if>
						
						<#assign fields = {} >
						
						,"parameters": [
							<#if (service.spec.fields)??>
								
								<#assign fields = service.spec.fields >
								<#assign allFields = fields?keys>
								
								<#assign isForm = false>
								<#assign isSingleBody = false>
								<#assign isMultiBody = false>
								
								<#assign firstIter = true>
								
								${TemplateTool.log ('Params+Headers ' + gk + ':' + key)}
								
								<#list allFields as fk>
									<#assign field = fields[fk] >
									<#if !(field.placeholder)??>
										<#continue>
									</#>
									<#if SimplePlaceholders?seq_contains(field.placeholder)>
										<#if firstIter == false>,</#if>
										{
											"name": "${fk}",
											"in": "${field.placeholder}",
											"description": "${(field.title)!fk}",
											"required": "${(field.required)!'true'}",
											<#if (field.value)??>"default": "${field.value}",</#if>
											${TemplateTool.log ('  Parameter ' + fk)}
											"schema": ${TemplateTool.oasSchema(output.space, output.namespace, field, components)}
										}
										<#assign firstIter = false>
									<#else>
										<#if (field.placeholder) == 'form'>
											<#assign isForm = true>
										<#elseif (field.placeholder) == 'body'>
											<#assign nor = TemplateTool.addToList(bodyFields, field)>
											<#if isSingleBody == true>
												<#assign isMultiBody = true>
											</#if>
											<#assign isSingleBody = true>
										</#if>
									</#if>
									
								</#list>
							</#if>
						],
						<#if isForm == true || isSingleBody == true || isMultiBody == true>
							"requestBody": {
								"content": {
									<#if isMultiBody == true>
										${TemplateTool.log ('isMultiBody ' + gk + ': ' key)}
										"multipart/form-data": {
											"schema": {
												"type": "object",
												"properties": {
													<#assign firstMixed = true>
													<#list fields?keys as fk>
														<#assign field = fields[fk] >
														<#if MixedPlaceholders?seq_contains(field.placeholder)>
															<#if firstMixed == false>,</#if>
															${TemplateTool.log ('  isMultiBody ' + fk)}
															"${fk}": ${TemplateTool.oasSchema(output.space, output.namespace, field, components)}
															<#assign firstMixed = false>
														</#if>
													</#list>
												}
											}
										}
									<#elseif isSingleBody == true>
										${TemplateTool.log ('isSingleBody ' + gk + ': ' + key)}
										<#assign contentTypes = (service.spec.fields.Accept.enum)!['application/json', 'application/yaml']>
										<#assign firstCt = true>
										<#list contentTypes as contentType>
											<#if firstCt == false>,</#if>
											"${contentType}": {
												${TemplateTool.log ('  isSingleBody > ' + contentType)}
												"schema": ${TemplateTool.oasSchema(output.space, output.namespace, bodyFields?first, components)}
											}
											<#assign firstCt = false>
										</#list>
									<#elseif isForm == true>
										${TemplateTool.log ('isForm ' + gk + ': ' + key)}
										"application/x-www-form-urlencoded": {
											<#assign allFields = fields?keys>
											<#assign firstIter = true>
											<#assign requiredFields = TemplateTool.newList() >
											"schema": {
												"type": "object",
												"properties": {
													<#list allFields as fk>
														<#assign field = fields[fk] >
														<#if (field.placeholder) == 'form'>
															<#if firstIter == false>,</#if>
															${TemplateTool.log ('  isForm ' + fk)}
															"${fk}": ${TemplateTool.oasSchema(output.space, output.namespace, field, components)}
															<#assign firstIter = false>
															<#if !(field.required)?? || (field.required) == 'true'>
																<#assign nor = TemplateTool.addToList(requiredFields, fk)>
															</#if>
														</#if>
													</#list>
												}
											}
											<#if !requiredFields?has_content>
												,"required": ["${requiredFields?join('","')}"]
											</#if>
										}
									</#if>
								}
							},
						</#if>
						"responses": {
							"200": {
								<#if gk == 'get'>
									"description": "OK",
									"content": {
										"application/json": {
											"schema": {
												"type": "object"
											}
										}
									}
								<#elseif gk == 'post'>
									"description": "Deleted",
									"content": {
										"application/json": {
											"schema": {
												"type": "object"
											}
										}
									}
								<#elseif gk == 'put'>
									"description": "Updated",
									"content": {
										"application/json": {
											"schema": {
												"type": "object"
											}
										}
									}
								<#elseif gk == 'delete'>
									"description": "Deleted",
									"content": {
										"application/json": {
											"schema": {
												"type": "object"
											}
										}
									}
								<#elseif gk == 'patch'>
									"description": "Updated",
									"content": {
										"application/json": {
											"schema": {
												"type": "object"
											}
										}
									}
								</#if>
							},
							"401": {
								"$ref": "#/components/responses/UnauthorizedError"
							},
							"403": {
								"$ref": "#/components/responses/ForbiddenError"
							},
							"404": {
								"$ref": "#/components/responses/NotFoundError"
							},
							"422": {
								"$ref": "#/components/responses/ValidationError"
							},
							"5XX": {
								"$ref": "#/components/responses/UnexpectedError"
							}
						}
						
					}<#if groupKeys?size gt (gk?index + 1)>,</#if>
				</#list>
			}<#if allGroupKeys?size gt (key?index + 1)>,</#if>
		</#list>
	},
	"components": {
		"responses": {
			"UnauthorizedError": {
				"description": "Unauthorized action error",
				"content": {
					"application/json": {
						"schema": {
							"$ref": "#/components/schemas/Error.401"
						}
					}
				}
			},
			"ForbiddenError": {
				"description": "Forbidden action error",
				"content": {
					"application/json": {
						"schema": {
							"$ref": "#/components/schemas/Error.403"
						}
					}
				}
			},
			"NotFoundError": {
				"description": "Requested object not found",
				"content": {
					"application/json": {
						"schema": {
							"$ref": "#/components/schemas/Error.404"
						}
					}
				}
			},
			"ValidationError": {
				"description": "Data validation error",
				"content": {
					"application/json": {
						"schema": {
							"$ref": "#/components/schemas/Error.422"
						}
					}
				}
			},
			"UnexpectedError": {
				"description": "unexpected error",
				"content": {
					"application/json": {
						"schema": {
							"$ref": "#/components/schemas/Error.5XX"
						}
					}
				}
			}
		},
		"schemas": {
			"Error.401": {
				"required": [
					"code",
					"message"
				],
				"properties": {
					"code": {
						"type": "integer",
						"format": "int32"
					},
					"message": {
						"type": "string"
					}
				}
			},
			"Error.403": {
				"required": [
					"code",
					"message"
				],
				"properties": {
					"code": {
						"type": "integer",
						"format": "int32"
					},
					"message": {
						"type": "string"
					}
				}
			},
			"Error.404": {
				"required": [
					"code",
					"message"
				],
				"properties": {
					"code": {
						"type": "integer",
						"format": "int32"
					},
					"message": {
						"type": "string"
					}
				}
			},
			"Error.422": {
				"required": [
					"code",
					"message"
				],
				"properties": {
					"code": {
						"type": "integer",
						"format": "int32"
					},
					"message": {
						"type": "string"
					}
				}
			},
			"Error.5XX": {
				"required": [
					"code",
					"message"
				],
				"properties": {
					"code": {
						"type": "integer",
						"format": "int32"
					},
					"message": {
						"type": "string"
					}
				}
			}
			<#if components?has_content>
				<#list components?keys as cmp>
					,"${cmp}": ${components[cmp]}
				</#list>
			</#if>
		}
	}
	
}