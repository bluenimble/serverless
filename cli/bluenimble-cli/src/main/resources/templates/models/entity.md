# [[Model]] entity
[[Model]]
	[[#if ModelSpec.refs]]import java.util.*[[/if]]
	@Entity(name="[[Model]]")
	
	id long @Id @Column(name = "ID") @GeneratedValue(strategy = GenerationType.IDENTITY)
	timestamp date
	
	# direct fields
	[[#each ModelSpec.fields]]
	[[#hasnt ModelSpec.refs @key]][[@key]] [[#eq type 'Object']]json @Convert(converter = JsonConverter.class)[[else]][[type]][[/eq]][[/hasnt]]
	[[/each]]
	
	[[#if ModelSpec.refs]]# relationships
	[[#each ModelSpec.refs]]
	[[#eq multiple 'true']]
	[[@key]] List<[[entity]]> @OneToMany @JoinColumn(name="[[Model]]ID", referencedColumnName="ID")
	[[else]]
	[[@key]] [[entity]] @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="ID")
	[[/eq]]
	[[/each]][[/if]]