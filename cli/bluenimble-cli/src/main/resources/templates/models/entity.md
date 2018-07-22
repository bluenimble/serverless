# [[Model]] entity
[[Model]]
	[[#if ModelSpec.refs]]import java.util.*[[/if]]
	@Entity(name="[[Model]]")
	
	id long @Id @Column(name = "ID") @GeneratedValue(strategy = GenerationType.IDENTITY)
	timestamp date
	
	# direct fields
	[[#each ModelSpec.fields]]
	[[#hasnt ModelSpec.refs @key]][[@key]] [[#isObject type]]json @Convert(converter = helpers.JsonConverter.class)[[else]][[type]][[/isObject]][[/hasnt]]
	[[/each]]
	
	[[#if ModelSpec.refs]]# relationships
	[[#each ModelSpec.refs]]
	[[#eq multiple 'true']]
	[[@key]] List<[[entity]]> @OneToMany @JoinColumn(name="[[uppercase Model]]_ID", referencedColumnName="ID")
	[[else]]
	[[@key]] [[entity]] @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="[[uppercase @key]]_ID")
	[[/eq]]
	[[/each]][[/if]]