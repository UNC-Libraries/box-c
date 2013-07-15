define('ResultObjectList', ['jquery', 'MetadataObject', 'ResultObject' ], function($, MetadataObject, ResultObject) {
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
			var self = this;
			console.time("Initialize entries");
			
			console.time("Get entries");
			var $entries = $(".res_entry", this.element);

			console.timeEnd("Get entries");
			var metadataObjects = self.options.metadataObjects;
			for (var i = 0; i < $entries.length; i++) {
				var id = $entries[i].id;
				id = 'uuid:' + id.substring(id.indexOf('_') + 1);
				self.resultObjects[id] = new ResultObject($entries.eq(i), {id : id, metadata : metadataObjects[id], 
					resultObjectList : self});
			}
			console.timeEnd("Initialize entries");
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
				url : this.options.refreshEntryUrl + resultObject.getPid(),
				dataType : 'json',
				success : function(data, textStatus, jqXHR) {
					var newContent = $(data.content);
					resultObject.getElement().replaceWith(newContent);
					self.resultObjects[id] = new ResultObject(newContent, {id : id, metadata : data.data.metadata, resultObjectList : self});
				}
			});
		}
		
	});
	
	return ResultObjectList;
});