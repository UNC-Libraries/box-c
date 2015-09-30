define('ResultObjectList', ['jquery', 'ResultObject' ], function($, ResultObject) {
	function ResultObjectList(options) {
		this.init(options);
	};
	
	$.extend(ResultObjectList.prototype, {
		defaultOptions: {
			resultIdPrefix : "entry_",
			metadataObjects : undefined,
			refreshEntryUrl : "entry/",
			parent : null,
			splitLoadLimit : 70,
			resultEntryTemplate : 'tpl!../templates/admin/resultEntry'
		},
		
		init: function(options) {
			this.options = $.extend({}, this.defaultOptions, options);
			var self = this;
			this.resultObjects = {};
			//console.time("Initialize entries");
			require([this.options.resultEntryTemplate], function(resultEntryTemplate){
				//console.profile();
				var metadataObjects = self.options.metadataObjects;
				for (var i = 0; i < metadataObjects.length && i < self.options.splitLoadLimit; i++) {
					var metadata = metadataObjects[i];
					self.resultObjects[metadata.id] = new ResultObject({metadata : metadata, resultObjectList : self, template : resultEntryTemplate});
					if (self.options.parent)
						self.options.parent.append(self.resultObjects[metadata.id].element);
				}
				if (metadataObjects.length > self.options.splitLoadLimit) {
					setTimeout(function(){
						//console.time("Second batch");
						for (var i = self.options.splitLoadLimit; i < metadataObjects.length; i++) {
							var metadata = metadataObjects[i];
							self.resultObjects[metadata.id] = new ResultObject({metadata : metadata, resultObjectList : self, template : resultEntryTemplate});
							if (self.options.parent)
								self.options.parent.append(self.resultObjects[metadata.id].element);
							
							$(document).trigger("cdrResultsRendered");
						}
						//console.timeEnd("Second batch");
					}, 100);
				} else {
					$(document).trigger("cdrResultsRendered");
				}
				//console.timeEnd("Initialize entries");
			});
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
			var resultObject = this.getResultObject(id);
			$.ajax({
				url : this.options.refreshEntryUrl + resultObject.getPid(),
				dataType : 'json',
				success : function(data, textStatus, jqXHR) {
					resultObject.init(data);
				},
				error : function(A, B, C) {
					console.log(B);
				}
			});
		},
		
		getSelected: function() {
			var selected = [];
			for (var index in this.resultObjects) {
				if (this.resultObjects[index].selected)
					selected.push(this.resultObjects[index]);
			}
			return selected;
		}
	});
	
	return ResultObjectList;
});