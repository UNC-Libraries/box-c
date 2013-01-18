define('ResultObjectList', ['jquery', 'MetadataObject', 'ResultObject' ], function($, MetadataObject) {
	function ResultObjectList(options) {
		this.init(options);
	};
	
	$.extend(ResultObjectList.prototype, {
		options: {
			resultIdPrefix : "entry_",
			metadataObjects : undefined
		},
		resultObjects: {},
		
		init: function(options) {
			this.options = $.extend({}, this.options, options);
			for (var i = 0; i < this.options.metadataObjects.length; i++) {
				var metadata = this.options.metadataObjects[i];
				var parentEl = $("#" + this.options.resultIdPrefix + metadata.id.replace(":", "\\:"));
				this.resultObjects[metadata.id] = parentEl.resultObject({"metadata" : metadata});
			}
		},
		
		getResultObject: function(id) {
			return this.resultObjects[id];
		}
	});
	
	return ResultObjectList;
});