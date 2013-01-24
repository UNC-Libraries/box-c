define('MetadataObject', [ 'jquery', 'PID' ], function($, PID) {
	function MetadataObject(metadata) {
		this.init(metadata);
	};
	
	$.extend(MetadataObject.prototype, {
		data: null,
		
		init: function(metadata) {
			this.data = metadata;
			if (this.data === undefined || this.data == null) {
				this.data = {};
			}
			this.setPID(this.data.id);
		},
		
		setPID: function(pid) {
			this.pid = new PID(pid);
		},
		
		isPublished: function() {
			if (!$.isArray(this.data.status)){
				return true;
			}
			return $.inArray("Unpublished", this.data.status) == -1;
		},
		
		publish: function () {
			if (!$.isArray(this.data.status)){
				this.data.status = [];
			} else {
				this.data.status.splice($.inArray("Unpublished", this.data.status), 1);
			}
			this.data.status.push("Published");
		},
		
		unpublish: function () {
			if (!$.isArray(this.data.status)){
				this.data.status = [];
			} else {
				this.data.status.splice($.inArray("Published", this.data.status), 1);
			}
			this.data.status.push("Unpublished");
		}
	});
	
	return MetadataObject;
});