require.config({
	urlArgs: "v=4.0-SNAPSHOT",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-access',
		'jquery-ui' : 'cdr-access',
		'text' : 'lib/text',
		'tpl' : 'lib/tpl',
		'underscore' : 'lib/underscore',
		'StructureEntry' : 'cdr-access',
		'StructureView' : 'cdr-access'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'underscore': {
			exports: '_'
		}
	}
});

define('structureBrowse', ['module', 'jquery', 'StructureView'], function(module, $) {
	$(".structure").structureView({
		showResourceIcons : true,
		queryPath : "search",
		secondaryActions : true,
		rootNode : module.config().results.root,
		retrieveFiles : true,
		filterParams : module.config().filterParams
	});
});
