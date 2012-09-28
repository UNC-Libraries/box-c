$(function() {
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
				$("#facet_field_path_structure").html(data);
			},
			error: function(){
				$("#facet_field_path_structure").hide();
				$("#facet_field_path_list").show();
			}
		});
	}
	
	
	
});