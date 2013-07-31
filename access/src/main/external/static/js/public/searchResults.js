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

define('searchResults', ['module', 'jquery', 'StructureView'], function(module, $) {
	$("#sort_select").change(function(){
		$("#result_sort_form").submit();
	});
	
	$("#facet_field_path_structure").removeClass("hidden");
	$("#facet_field_path_list").hide();
	
	var structureUrl = $("#facet_field_path_structure > a").attr("href");
	
	var filterParams = module.config().filterParams;
	if (structureUrl !== undefined){
		$.ajax({
			url: structureUrl,
			dataType : 'json',
			success: function(data){
				var $structure = $('<div/>').html(data);
				$structure.addClass("facet popout").structureView({
					showResourceIcons : false,
					showParentLink : true,
					rootNode : data.root,
					queryPath : 'list',
					filterParams : filterParams
				});
				$("#facet_field_path_structure").html($structure);
			},
			error: function(){
				$("#facet_field_path_structure").hide();
				$("#facet_field_path_list").show();
			}
		});
	}
});