define('thumbnails', ['jquery', 'preload'], function($) {
	var placeholderUrls = {};
	
	$(".smallthumb, .largethumb").each(function(){
		var classList = $(this).attr('class').split(/\s+/);
		$.each( classList, function(index, item){
			if (item.indexOf('ph') == 0) {
				var placeholderComponents = item.split(/\_/);
				if (placeholderComponents.length == 3 && !(item in placeholderUrls)){
					placeholderUrls["." + item] = "/static/images/placeholder/" + placeholderComponents[1] + "/" + placeholderComponents[2] + ".png";
				}
			}
		});
	});
	
	for (placeholderKey in placeholderUrls){
		$(placeholderKey).preload({
			placeholder: placeholderUrls[placeholderKey],
			hidePlaceholderOnLoad: true,
			notFound: placeholderUrls[placeholderKey],
			threshold: 1,
			onComplete: function(data){
				if (data.found)
					$("#" + data.original.id).fadeIn(500);
			}
		});
	}
});