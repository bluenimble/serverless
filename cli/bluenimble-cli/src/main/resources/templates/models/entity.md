# [[Model]] entity
[[Model]]
	[[#if ModelSpec.refs]]import java.util.*[[/if]]
	@Entity(name="[[Model]]")
	
	[[#eq ModelSpec.addDefaults 'true']]# defaults
	id long @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	timestamp date[[/eq]]
	
	# direct fields
	[[#each ModelSpec.fields]]
	[[@key]] [[#eq type 'json']]json @Convert(converter = JsonConverter.class)[[else]][[type]][[/eq]]
	[[/each]]
	
	[[#if ModelSpec.refs]]# relationships
	[[#each ModelSpec.refs]]
	[[@key]] [[type]]
	[[/each]][[/if]]