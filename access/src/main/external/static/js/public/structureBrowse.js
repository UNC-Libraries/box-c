require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'thumbnail' : 'thumbnail',
		'text' : 'text',
		'underscore' : 'underscore',
		'StructureEntry' : 'src/StructureEntry',
		'StructureView' : 'src/StructureView'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'thumbnail' : ['jquery'],
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
		retrieveFiles : true
	});
});