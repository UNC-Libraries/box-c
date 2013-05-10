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

define('searchResults', ['module', 'jquery', 'StructureView', 'cdrCommon'], function(module, $) {
	$("#sort_select").change(function(){
		$("#result_sort_form").submit();
	});
	
	$("#facet_field_path_structure").removeClass("hidden");
	$("#facet_field_path_list").hide();
	
	var structureUrl = $("#facet_field_path_structure > a").attr("href");
	
	if (structureUrl !== undefined){
		$.ajax({
			url: structureUrl,
			success: function(data){
				var $structure = $(data);
				$structure.addClass("facet popout").structureView({
					showResourceIcons : false
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