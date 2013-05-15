require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'qtip' : 'jquery.qtip.min',
		'cdrCommon' : 'cdrCommon',
		'thumbnail' : 'thumbnail',
		'PID' : 'admin/src/PID',
		'StructureEntry' : 'src/StructureEntry',
		'StructureView' : 'src/StructureView'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'qtip' : ['jquery'],
		'thumbnail' : ['jquery'],
		'cdrCommon' : ['jquery', 'qtip']
	}
});
define('containerRecord', ['module', 'jquery', 'cdrCommon', 'StructureView'], function(module, $) {
	var $structureView = $(".structure");
	
	if ($structureView.length > 0) {
		$structureView.structureView({
			showResourceIcons : true
		});
	}
});