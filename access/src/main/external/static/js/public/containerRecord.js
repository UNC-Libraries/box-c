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
define('containerRecord', ['module', 'jquery', 'StructureView'], function(module, $) {
	var $structureView = $(".structure");
	
	if ($structureView.length > 0) {
		$.ajax({
			url: "/structure/" + $structureView.attr('data-pid') + "/json",
			dataType : 'json',
			success: function(data){
				$structureView.structureView({
					showResourceIcons : true,
					showParentLink : false,
					rootNode : data.root,
					queryPath : 'list'
				});
			},
			error: function(e){
				console.log("Failed to load", e);
			}
		});
	}
});