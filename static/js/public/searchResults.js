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
		'StructureView' : 'cdr-access',
		'thumbnails' : 'cdr-access'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'underscore': {
			exports: '_'
		}
	}
});

define('searchResults', ['module', 'jquery', 'StructureView', 'thumbnails'], function(module, $) {
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
