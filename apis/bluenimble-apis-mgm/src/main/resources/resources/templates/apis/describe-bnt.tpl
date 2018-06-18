{
	"tests": [
        {% if (output.services is not null) %}
		{% for service in output.services %}

        {% set params = 0 %}
        {% set headers = 0 %}
        {% set streams = 0 %}
        
        {
        "id": "t{{loop.index}}",
        "request": {	
			"method": "{{ service.verb }}",
			"service": "[keys.domain]/{{output.namespace}}{{ service.endpoint }}",
			"headers": {
                {% if (service.spec.fields is not null ) %}
				{% for key, value in service.spec.fields %}
					{% if (value.scope == "h") %}
						{% if (headers > 0) %},{% endif %}
						"{{ key }}": "{% if (value.value is not null) %}{{value.value}}{% else %}{{valueGuesser.guess(key, value)}}{% endif %}"
						{% set headers = (headers + 1) %}
					{% endif %}
				{% endfor %}
                {% endif %}
			},
			"params": {
                {% if (service.spec.fields is not null ) %}
				{% for key, value in service.spec.fields %}
					{% if ( ( (value.scope is null) or (value.scope == "p") ) and (key != 'payload') ) %}
						{% if (params > 0) %},{% endif %}
						"{{ key }}": {% if value.type != 'Object' %}"{% endif %}{% if (value.value is not null) %}{{value.value}}{% else %}{{valueGuesser.guess(key, value)}}{% endif %}{% if value.type != 'Object' %}"{% endif %}
						{% set params = (params + 1) %}
					{% endif %}
				{% endfor %}
                {% endif %}
			},
			"body": {
                {% if (service.spec.fields is not null ) %}
		    	{% for key, value in service.spec.fields %}
					{% if ( (value.scope == "s") or (key == "payload") ) %}
						{% if (streams > 0) %},{% endif %}
						"{{ key }}": {% if (value.value is not null) %}{{value.value}}{% else %}{{valueGuesser.guess(key, value)}}{% endif %}
						{% set streams = (streams + 1) %}
					{% endif %}
				{% endfor %}
                {% endif %}
			}
		}	
		}{% if (loop.length > loop.index) %},{% endif %}
		{% endfor %}
        {% endif %}
	]
}