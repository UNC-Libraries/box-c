require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'thumbnail' : 'thumbnail',
		'StructureEntry' : 'src/StructureEntry',
		'StructureView' : 'src/StructureView'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'thumbnail' : ['jquery']
	}
});

define('structureBrowse', ['module', 'jquery', 'StructureView'], function(module, $) {
	$(".structure").structureView({
		showResourceIcons : true
	});
});