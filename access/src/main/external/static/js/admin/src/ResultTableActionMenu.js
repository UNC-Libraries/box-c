define('ResultTableActionMenu', [ 'jquery', 'jquery-ui', 'ResultObjectList'], 
		function($, ui, ResultObjectList) {

	var defaultOptions = {
		resultObjectList : null,
		actions : {
			deleteBatch : {
				label : "Delete",
				className : "DeleteBatchButton",
				permissions : ["moveToTrash"]
			}, publish : {
				label : "Publish",
				className : "PublishBatchButton",
				permissions : ["publish"]
			}, unpublish : {
				label : "Unpublish",
				className : "UnpublishBatchButton",
				permissions : ["publish"]
			}, reindex : {
				label : "Reindex",
				className : "UnpublishBatchButton",
				permissions : ["purgeForever"]
			}
		},
		groups : {
			1 : ['deleteBatch'],
			2 : ['publish', 'unpublish']/*,
			'more' : ['reindex']*/
		}
	};
	
	function ResultTableActionMenu(options) {
		this.options = $.extend({}, defaultOptions, options);
		this.resultObjectList = this.options.resultObjectList;
		this.init(this.options.metadata);
	};
	
	ResultTableActionMenu.prototype.init = function() {
		var self = this;
		// Load action classes
		var actionClasses = [];
		$.each(this.options.groups, function(groupName, actionList){
			for (var i in actionList) {
				actionClasses.push(self.options.actions[actionList[i]].className);
			}
		});
		
		require(actionClasses, function(){
			var argIndex = 0, loadedClasses = arguments;
			self.element = $("<div/>").addClass("result_table_action_menu");
			
			$.each(self.options.groups, function(groupName, actionList){
				var groupSpan = $("<span/>").addClass("container_action_group").appendTo(self.element);
				for (var i in actionList) {
					var actionDefinition = self.options.actions[actionList[i]];
					actionDefinition.actionClass = loadedClasses[argIndex++];
					
					if (groupName != 'more') {
						var actionButton = $("<span>" + actionList[i] + "</span>")
								.addClass(actionList[i] + "_selected ajaxCallbackButton container_action")
								.appendTo(groupSpan);
						actionDefinition.actionObject =  new actionDefinition.actionClass({resultObjectList : self.resultObjectList}, actionButton);
					}
				}
			});
		});
	};
	
	return ResultTableActionMenu;
});