$(function() {
	$.preload(['expand.png', 'collapse.png', 'hier_vertical_line.png', 'hier_container_with_siblings.png', 'hier_container.png', 
	           'hier_file.png', 'hier_folder.png', 'hier_collection.png'], {
		base: "/static/images/"
	});
	
	var actionTooltipSettings = {
			content: {
				text: false
			},
			show: {
				delay: 500
			},
			position: {
				target: 'mouse',
				corner: {
					target: 'bottomMiddle',
					tooltip: 'topLeft'
				},
				adjust: {
					screen: true
				}
			},
			style: {
				classes: {
					content: "tooltip_browse_results"
				},
				border: {
					width: 0
				},
				tip: {
					corner: 'topRight',
					color: '#f2f2f2',
					size: {
						x: 7,
						y: 5
					}
				}
			}
		}; 
	
		
	$(".hier_entry_primary_action").qtip(actionTooltipSettings);
	
	actionTooltipSettings['show']['delay'] = 100;
	$(".hier_entry_secondary_action").qtip(actionTooltipSettings);
	
	$(".hier_action.collapse").live('click', function(){
		var toggleImage = $("#" + this.id + " img");
		toggleImage.attr("src", "/static/images/expand.png");
		
		var pid = this.id.substring(this.id.lastIndexOf("_") + 1);
		$("#hier_container_children_" + pid).hide();
		$(this).removeClass("collapse");
		$(this).addClass("expand");
		return false;
	});
	
	$(".hier_action.expand").live('click', function(){
		var initiatingLink = $(this);
		var toggleImage = $("#" + this.id + " img");
		toggleImage.attr("src", "/static/images/collapse.png");
		
		$(this).removeClass("expand");
		$(this).addClass("collapse");
		
		var pid = this.id.substring(this.id.lastIndexOf("_") + 1);
		var structureUrl = $(this).attr("href");
		var poundIndex = structureUrl.indexOf("#");
		structureUrl = structureUrl.substring(poundIndex + 1);
		
		var indentDepth = $("#" + this.id + ":parent").parent().find(".indent_unit").size() + 1;
		
		if ($(this).hasClass("hier_container_not_loaded")){
			var loadingImage = $("<img src=\"/static/images/ajax_loader.gif\"/>");
			initiatingLink.after(loadingImage);
			$.ajax({
				url: structureUrl,
				success: function(data){
					if (data) {
						var childrenContainer = $("#hier_container_children_" + pid);
						$("#hier_container_children_" + pid + " .hier_entry .indent_unit:nth-child(" + indentDepth + ")").each(function(){
							if (!$(this).hasClass("hier_with_siblings")){
								$(this).addClass("hier_with_siblings");
							}
						});
						
						$("#hier_container_children_" + pid + " .hier_entry .hier_container_not_loaded").each(function(){
							var expandUrl = $(this).attr("href");
							beginIndentCode = expandUrl.indexOf("&indentCode=") + 12;
							if (beginIndentCode != -1){
								endIndentCode = expandUrl.indexOf("&", beginIndentCode);
								if (endIndentCode == -1){
									endIndentCode = expandUrl.length-1;
								}
								indentCode = expandUrl.substring(beginIndentCode, endIndentCode);
								indentCode = indentCode.substring(0, indentDepth-1) + "1" + indentCode.substring(indentDepth+1);
								
								$(this).attr("href", expandUrl.substring(0, beginIndentCode) + indentCode + expandUrl.substring(endIndentCode));
							}
							
						});
						childrenContainer.html(childrenContainer.html() + data);
						childrenContainer.show();
					}
					initiatingLink.removeClass("hier_container_not_loaded");
					loadingImage.remove();
				},
				error: function(xhr, ajaxOptions, thrownError){
					loadingImage.remove();
				}
			});
			
		} else {
			$("#hier_container_children_" + pid).show();
		}
		return false;
	});
});