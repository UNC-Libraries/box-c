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
define('containerRecord', ['module', 'jquery', 'StructureView'], function(module, $) {
	var $structureView = $(".structure");
	
	if ($structureView.length > 0) {
		$structureView.structureView({
			showResourceIcons : true
		});
	}
});