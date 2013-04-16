define('ResultObjectList', ['jquery', 'MetadataObject', 'ResultObject' ], function($, MetadataObject) {
	function ResultObjectList(options) {
		this.init(options);
	};
	
	$.extend(ResultObjectList.prototype, {
		options: {
			resultIdPrefix : "entry_",
			metadataObjects : undefined,
			refreshEntryUrl : "entry/"
		},
		resultObjects: {},
		
		init: function(options) {
			this.options = $.extend({}, this.options, options);
			for (var i = 0; i < this.options.metadataObjects.length; i++) {
				var metadata = this.options.metadataObjects[i];
				var parentEl = $(".entry[data-pid='" + metadata.id + "']");
				//var parentEl = $("#" + this.options.resultIdPrefix + metadata.id.replace(":", "\\:"));
				this.resultObjects[metadata.id] = parentEl.resultObject({"metadata" : metadata, "resultObjectList" : this});
			}
		},
		
		getResultObject: function(id) {
			return this.resultObjects[id];
		},
		
		removeResultObject: function(id) {
			if (id in this.resultObjects) {
				delete this.resultObjects[id];
			}
		},
		
		refreshObject: function(id) {
			var self = this;
			var resultObject = this.getResultObject(id);
			$.ajax({
				url : this.options.refreshEntryUrl + resultObject.resultObject('getPid').getPath(),
				dataType : 'json',
				success : function(data, textStatus, jqXHR) {
					var newContent = $(data.content);
					resultObject.replaceWith(newContent);
					self.resultObjects[id] = newContent.resultObject({"metadata" : data.data.metadata, "resultObjectList" : self});
				}
			});
		}
		
	});
	
	return ResultObjectList;
});