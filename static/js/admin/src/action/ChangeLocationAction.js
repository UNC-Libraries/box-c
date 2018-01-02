define('ChangeLocationAction', [ 'jquery'], function($) {
	function ChangeLocationAction(context) {
		this.context = context;
	};
	
	ChangeLocationAction.prototype.execute = function() {
		var url;
		if (this.context.application !== undefined) {
			url = this.context.accessBaseUrl;
		} else {
			url = this.context.adminBaseUrl;
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