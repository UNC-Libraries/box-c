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
				$('.hier_entry_description').ellipsis({
					textSelector: '.hier_entry_primary_action', 
					parentSelector: "#facet_field_path_structure",
					ellipsisClass: "hier_ellipsis",
					segmentClass: "hier_segment"
				}).setEllipsisDefaults({
					parentWidth: $("#facet_field_path_structure").width(),
					parentLeft: $("#facet_field_path_structure").offset().left
				});
			},
			error: function(){
				$("#facet_field_path_structure").hide();
				$("#facet_field_path_list").show();
			}
		});
	}
	
	
	
});