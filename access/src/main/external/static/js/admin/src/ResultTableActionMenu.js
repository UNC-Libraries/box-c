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
			}, deleteBatchForever : {
				label : "Delete Forever",
				className : "DeleteBatchButton",
				permissions : ["purgeForever"]
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
		groups : undefined
	};
	
	function ResultTableActionMenu(options, element) {
		this.options = $.extend({}, defaultOptions, options);
		this.element = element;
		this.resultObjectList = this.options.resultObjectList;
		this.actionHandler = this.options.actionHandler;
		this.init();
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
		
		this.actionButtons = [];
		this.actionGroups = [];
		
		require(actionClasses, function(){
			var argIndex = 0, loadedClasses = arguments;
			
			$.each(self.options.groups, function(groupName, actionList){
				var groupSpan = $("<span/>").addClass("container_action_group").appendTo(self.element);
				self.actionGroups.push(groupSpan);
				for (var i in actionList) {
					var actionDefinition = self.options.actions[actionList[i]];
					actionDefinition.actionClass = loadedClasses[argIndex++];
					
					if (groupName != 'more') {
						var actionButton = $("<span>" + actionDefinition.label + "</span>")
								.addClass(actionList[i] + "_selected ajaxCallbackButton container_action")
								.appendTo(groupSpan);
						actionButton.data('actionObject', new actionDefinition.actionClass({
								resultObjectList : self.resultObjectList,
								actionHandler : self.actionHandler
							}, actionButton));
						
						actionButton.click(function(){
							$(this).data('actionObject').activate();
						});
						
						self.actionButtons.push(actionButton);
					}
				}
			});
			self.selectionUpdated();
			$(window).resize();
		});
	};
	
	ResultTableActionMenu.prototype.selectionUpdated = function() {
		for (var i in this.actionButtons) {
			var actionButton = this.actionButtons[i];
			var actionObject = actionButton.data('actionObject');
			if (actionObject.hasTargets()) {
				actionButton.removeClass("hidden");
			} else {
				actionButton.addClass("hidden");
			}
		}
		
		for (var i in this.actionGroups) {
			var actionGroup = this.actionGroups[i];
			var visibleChildren = actionGroup.children(".container_action").not(".hidden");
			if (visibleChildren.length == 0) {
				actionGroup.hide();
			} else {
				visibleChildren.removeClass("first_visible").first().addClass("first_visible");
				actionGroup.show();
			}
		}
	};
	
	return ResultTableActionMenu;
});