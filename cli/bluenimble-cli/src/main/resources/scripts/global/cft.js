
var File 			= native ('java.io.File');

var SpecUtils 		= native ('com.bluenimble.platform.icli.mgm.utils.SpecUtils');

// check if valid command args
if (typeof Command === 'undefined') {
	throw 'missing command arguments. eg. cft [Template Variable]';
}

var tokens = Lang.split (Command, ' ', true);

if (tokens.length < 1) {
	throw 'missing command arguments. eg. cft MyTemplateVar';
}

var templateName = tokens [0];

var template = Vars [templateName];

if (!template) {
	throw 'template ' + templateName + ' not found';
}

// check namespace
if (!template.namespace) {
	throw 'api namespace not found in template';
}

// check model
if (!template.model) {
	Tool.warning ('No model found');
	return;
}

Tool.command ('create api ' + template.namespace);

var apiSrcPath = Config.apis [template.namespace];
if (!apiSrcPath) {
	apiSrcPath = template.namespace;
}

var apiFolder   = new File (Config.workspace + '/' + apiSrcPath);

var apiSpec;
try {
	apiSpec = SpecUtils.read (apiFolder);
} catch (ex) {
	Tool.error ('File ' + apiSpecFile.getAbsolutePath () + ' -> ' + ex.message);
	return;
}

// set from template
apiSpec.name 		= template.name;
apiSpec.description = template.description;
apiSpec.vendor 		= template.vendor;
apiSpec.version 	= template.version;

// save spec
SpecUtils.write (apiFolder, apiSpec);

for (var entity in template.model) {
	// for each entity 
	Tool.command ('create service * ' + entity);
	
	var services = Vars.get ('services.created');
	
	// add spec from template
	// if a field is a reference, generate endpoint
}

