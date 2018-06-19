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
        				"method": "${service.verb}",
						"service": "[keys.endpoints.space]/${output.namespace}${endpoint}",
						"headers": {
			                <#if (service.spec.fields)??>
			                	<#list (service.spec.fields)?keys as key>
									<#assign spec = service.spec.fields[key]>
									<#if (spec.scope)?? && spec.scope == "h">
										<#if headers gt 0 >,</#if>
										"${key}": "${(spec.value)!Guesser.guess(key, spec)}"
										<#assign headers++ >
									</#if>
									<#if !(service.security.enabled)?? || ((service.security.enabled)?? && service.security.enabled == "true")>
										<#if headers gt 0 >,</#if>
										"Authorization": "Token [vars.Token]"
										<#assign headers++ >
									</#if>
								</#list>
			                </#if>
						},
						"params": {
			                <#if (service.spec.fields)??>
			                	<#list (service.spec.fields)?keys as key>
									<#assign spec = service.spec.fields[key]>
									<#if (!(spec.scope)?? || spec.scope == "p") && key != "payload">
										<#if params gt 0 >,</#if>
										"${key}": <#if !(spec.type)?? || (spec.type?upper_case != "OBJECT" && spec.type?upper_case != "MAP")>"</#if>${(spec.value)!Guesser.guess(key, spec)}<#if !(spec.type)?? || (spec.type?upper_case != "OBJECT" && spec.type?upper_case != "MAP")>"</#if>
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
										"${key}": ${(spec.value)!Guesser.guess(key, spec)}
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