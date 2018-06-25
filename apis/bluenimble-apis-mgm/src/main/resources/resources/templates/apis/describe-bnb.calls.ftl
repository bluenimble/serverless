{
	"calls": [
        <#if (output.services)??>
			<#list output.services as service>
				
				<#assign endpoint = "" >
				
				<#assign aEndpoint = service.endpoint?split("/") >
				<#list aEndpoint as part>
					<#if part?starts_with(":")>
						<#assign endpoint += ("[params." + part?replace(":", "") + "]/")>
					<#else>
						<#assign endpoint += (part + "/")>
					</#if>
				</#list>
                
                <#if endpoint?ends_with("/")>
                    <#assign endpoint = endpoint?remove_ending("/") >
                </#if>
				
		        <#assign params = 0 >
		        <#assign headers = 0 >
		        <#assign streams = 0 >
				{
        			"id": "Call-${service?index}",
        			"request": {
        				"method": "${(service.verb)!'GET'}",
						"service": "[keys.endpoints.space]/${output.namespace}${endpoint}",
						"headers": {
			                <#if (service.spec.fields)??>
			                	<#list (service.spec.fields)?keys as key>
									<#assign spec = service.spec.fields[key]>
									<#if (spec.scope)?? && spec.scope == "h">
										<#if headers gt 0 >,</#if>
										"${key}": "${(spec.value)!TemplateTool.guess(output.space, output.namespace, key, spec)}"
										<#assign headers++ >
									</#if>
									<#if !(service.security.enabled)?? || ((service.security.enabled)?? && service.security.enabled == "true")>
										<#if headers gt 0 >,</#if>
										"Authorization": "Bearer [vars.Token]"
										<#assign headers++ >
									</#if>
								</#list>
			                </#if>
						},
						"params": {
			                <#if (service.spec.fields)??>
			                	<#list (service.spec.fields)?keys as key>
									<#assign spec = service.spec.fields[key]>
									<#assign isObjectType = TemplateTool.isObjectType(output.space, output.namespace, spec)>
									<#if (!(spec.scope)?? || spec.scope == "p") && key != "payload">
										<#if params gt 0 >,</#if>
										"${key}": <#if isObjectType == false>"</#if>${(spec.value)!TemplateTool.guess(output.space, output.namespace, key, spec)}<#if isObjectType == false>"</#if>
										<#assign params++ >
									</#if>
								</#list>
			                </#if>
						},
						"body": {
			                <#if (service.spec.fields)??>
			                	<#list (service.spec.fields)?keys as key>
									<#assign spec = service.spec.fields[key]>
									<#if ((spec.scope)?? && spec.scope == "s") || key == "payload">
										<#if streams gt 0 >,</#if>
										"${key}": ${(spec.value)!TemplateTool.guess(output.space, output.namespace, key, spec)}
										<#assign streams++ >
									</#if>
								</#list>
			                </#if>
						}
        			}
        		}<#if (output.services)?size gt (service?index + 1)>,</#if>
			</#list>
		</#if>
	]
}