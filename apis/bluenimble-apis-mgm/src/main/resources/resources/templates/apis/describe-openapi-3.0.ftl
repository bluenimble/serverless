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
	
	<#assign components = {} >
	<#assign bodyFields = {} >
	<#assign genericObjects = []> 
	
	<#assign groups = TemplateTool.groupBy (output.space, output.namespace, 'endpoint', 'verb')>
	
	<#assign UsualPlaceholders = ['header', 'path', 'query']>
	<#assign MixedPlaceholders = ['body', 'form']>
	
	,"paths": {	
		<#assign allGroupKeys = groups?keys>
		<#list allGroupKeys as key>
			"${key}": {
				<#assign group = groups[key]>
				<#assign groupKeys = group?keys>
				<#list groupKeys as gk>
					<#assign service = group[gk] >
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
							,"tags": ${service.meta.tags}
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
								
								<#list allFields as fk>
									<#assign field = fields[fk] >
									
									<#if SimplePlaceholders?contains(field.placeholder)>
										<#if firstIter == false>,</#if>
										{
											"name": "${fk}",
											"in": "${field.placeholder}",
											"description": "${(field.title)!fk}",
											"required": "${(field.required)!'true'}",
											<#if (field.value)??>"default": "${field.value}",</#if>
											"schema": ${TemplateTool.oasSchema(field, components)}
										}
										<#assign firstIter = false>
									<#else>
										<#if (field.placeholder) == 'form'>
											<#assign isForm = true>
										<#elseif (field.placeholder) == 'body'>
											<#assign bodyFields += field}
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
										"multipart/form-data": {
											"schema": {
												"type": "object",
												"properties": {
													<#if firstMixed == true>
													<#list allFields as fk>
														<#assign field = fields[fk] >
														<#if MixedPlaceholders?contains(field.placeholder)>
															<#if firstIter == false>,</#if>
															"${fk}": {
																"description": "${(field.title)!fk}",
																<#if (field.value)??>"default": "${field.value}",</#if>
																"schema": ${TemplateTool.oasSchema(field, components)}
															}
															<#if firstMixed == false>
															<#if >
																<#assign requiredFields += '${fk}'>
															</#if>
														</#if>
													</#list>
												}
											}
										}
									<#elseif isSingleBody == true>
										<#assign firstCt = true>
										<#list service.spec.contentTypes as contentType>
											<#if firstCt == false>,</#if>
											"${contentType}": {
												"schema": {
													<#if (bodyFields[0].type)??>
														"$ref": "#/components/schemas/${bodyFields[0].type}"
													</#else>
														"$ref": "#/components/schemas/Generic"
														<#assign components += bodyFields[0]> 
													</#if>
												}
											}
											<#assign firstCt = false>
										</#list>
									<#elseif isForm == true>
										"application/x-www-form-urlencoded": {
											<#assign allFields = fields?keys>
											<#assign firstIter = true>
											<#assign requiredFields = []>
											"schema": {
												"type": "object",
												"properties": {
													<#list allFields as fk>
														<#assign field = fields[fk] >
														<#if (field.placeholder) == 'form'>
															<#if firstIter == false>,</#if>
															"${fk}": {
																"description": "${(field.title)!fk}",
																<#if (field.value)??>"default": "${field.value}",</#if>
																"schema": ${TemplateTool.oasSchema(field, components)}
															}
															<#assign firstIter = false>
															<#if >
																<#assign requiredFields += '${fk}'>
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
								"description": "pet response",
								"content": {
									"application/json": {
										"schema": {
											"$ref": "#/components/schemas/Pet"
										}
									}
								}
							},
							"401": {
								"description": "Unauthorized action error",
								"content": {
									"application/json": {
										"schema": {
											"$ref": "#/components/schemas/Error.401"
										}
									}
								}
							},
							"403": {
								"description": "Forbidden action error",
								"content": {
									"application/json": {
										"schema": {
											"$ref": "#/components/schemas/Error.403"
										}
									}
								}
							},
							"422": {
								"description": "Data validation error",
								"content": {
									"application/json": {
										"schema": {
											"$ref": "#/components/schemas/Error.422"
										}
									}
								}
							},
							"default": {
								"description": "unexpected error",
								"content": {
									"application/json": {
										"schema": {
											"$ref": "#/components/schemas/Error.500"
										}
									}
								}
							}
						}
						
					}<#if groupKeys?size gt (gk?index + 1)>,</#if>
				</#list>
			}<#if allGroupKeys?size gt (key?index + 1)>,</#if>
		</#list>
	},
	"components": {
		"schemas": {
			"Error.500": {
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
			}
			<#if components?has_content>
				<#list components as go>
					,"Object_${go?index}": {
						"properties": ${TemplateTool.oasObjectSchema(go)}
					}
				</#list>
			</if>
		}
	}
	
}