define('ChangeLocationAction', [ 'jquery', 'URLUtilities'], function($, URLUtilities) {
	function ChangeLocationAction(context) {
		this.context = context;
	};
	
	ChangeLocationAction.prototype.execute = function() {
		var url;
		if (this.context.application == "access") {
			url = URLUtilities.getAccessUrl();
		} else {
			url = URLUtilities.getAdminUrl();
		}

		url += this.context.url;
		
		if (this.context.newWindow) {
			window.open(url,'_blank');
		} else {
			document.location.href = url;
		}
	};

	return ChangeLocationAction;
});