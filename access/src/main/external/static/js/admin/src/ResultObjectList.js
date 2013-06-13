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
			var self = this;
			console.time("Initialize entries");
			
			console.time("Get entries");
			var $entries = $(".res_entry", this.element);
			var test = $entries.eq(0);
			test = $entries.eq(1);
			test = $entries.eq(200);
			test = $entries.eq(500);
			console.timeEnd("Get entries");
			//console.profile();
			for (var i = 0; i < $entries.length; i++) {
				var $this = $entries.eq(i);
				var id = $this.attr('id');
				id = 'uuid' + id.substring(id.indexOf(':') + 1);
				var metadata = self.options.metadataObjects[id];
				self.resultObjects[id] = $this.resultObject({'id' : id, "metadata" : metadata, "resultObjectList" : self});
			}
			//console.profileEnd();
			/*$(".res_entry").each(function(){
				var $this = $(this);
				var id = $this.attr('id');
				id = 'uuid' + id.substring(id.indexOf(':') + 1);
				var metadata = self.options.metadataObjects[id];
				self.resultObjects[id] = $this.resultObject({'id' : id, "metadata" : metadata, "resultObjectList" : self});
			});*/
			/*for (var i = 0; i < this.options.metadataObjects.length; i++) {
				var metadata = this.options.metadataObjects[i];
				var parentEl = $("#res_" + metadata.id.substring(metadata.id.indexOf(':') + 1));
				this.resultObjects[metadata.id] = parentEl.resultObject({"metadata" : metadata, "resultObjectList" : this});
			}*/
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
				url : this.options.refreshEntryUrl + resultObject.resultObject('getPid'),
				dataType : 'json',
				success : function(data, textStatus, jqXHR) {
					var newContent = $(data.content);
					resultObject.replaceWith(newContent);
					self.resultObjects[id] = newContent.resultObject({'id' : id, "metadata" : data.data.metadata, "resultObjectList" : self});
				}
			});
		}
		
	});
	
	return ResultObjectList;
});