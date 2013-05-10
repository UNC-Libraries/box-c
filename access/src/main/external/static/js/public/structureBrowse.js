require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'qtip' : 'jquery.qtip.min',
		'thumbnail' : 'thumbnail',
		'PID' : 'admin/src/PID',
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