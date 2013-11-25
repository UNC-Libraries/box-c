define('ResultTableActionMenu', [ 'jquery', 'jquery-ui', 'ResultObjectList'], 
		function($, ui, ResultObjectList) {

	var defaultOptions = {
		resultObjectList : null,
		actions : {
			deleteBatch : {
				label : "Delete",
				className : "MoveBatchToTrashButton",
				permissions : ["moveToTrash"]
			}, restoreBatch : {
				label : "Restore",
				className : "RemoveBatchFromTrashButton",
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
			1 : ['restoreBatch', 'deleteBatch'],
			2 : ['publish', 'unpublish']/*,
			'more' : ['reindex']*/
		}
	};
	
	function ResultTableActionMenu(options, parentElement) {
		this.options = $.extend({}, defaultOptions, options);
		this.resultObjectList = this.options.resultObjectList;
		this.init(parentElement);
	};
	
	ResultTableActionMenu.prototype.init = function(parentElement) {
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
			parentElement.append(self.element);
			
			$.each(self.options.groups, function(groupName, actionList){
				var groupSpan = $("<span/>").addClass("container_action_group").appendTo(self.element);
				for (var i in actionList) {
					var actionDefinition = self.options.actions[actionList[i]];
					actionDefinition.actionClass = loadedClasses[argIndex++];
					
					if (groupName != 'more') {
						var actionButton = $("<span>" + actionDefinition.label + "</span>")
								.addClass(actionList[i] + "_selected ajaxCallbackButton container_action")
								.appendTo(groupSpan);
						actionButton.data('actionObject', new actionDefinition.actionClass({resultObjectList : self.resultObjectList}, actionButton));
						
						actionButton.click(function(){
							$(this).data('actionObject').activate();
						});
					}
				}
			});
		});
	};
	
	return ResultTableActionMenu;
});