define('ResultTableActionMenu', [ 'jquery', 'jquery-ui', 'ResultObjectList', 'ActionButton'], 
		function($, ui, ResultObjectList, ActionButton) {

	var defaultOptions = {
		resultObjectList : null,
		actions : {
			deleteBatch : {
				label : "Delete",
				action : "DeleteBatch"
			}, restoreBatch : {
				label : "Restore",
				action : "RestoreBatch"
			}, destroyBatch : {
				label : "Destroy",
				action : "DestroyBatch"
			}, publish : {
				label : "Publish",
				action : "PublishBatch"
			}, unpublish : {
				label : "Unpublish",
				action : "UnpublishBatch"
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
		for (var index in this.options.groups) {
			var group = this.options.groups[index];
			for (var aIndex in group.actions) {
				actionClasses.push(group.actions[aIndex].action + "Action");
			}
		}
		
		this.actionButtons = [];
		this.actionGroups = [];
		
		require(actionClasses, function(){
			var argIndex = 0, loadedClasses = arguments;
			
			for (var index in self.options.groups) {
				var group = self.options.groups[index];
				if (group.hideInTableMenu) {
					continue;
				}
			
				var groupSpan = $("<span/>").addClass("container_action_group").hide().appendTo(self.element);
				self.actionGroups.push(groupSpan);
			
				for (var aIndex in group.actions) {
					var actionDefinition = group.actions[aIndex];
					var actionClass = loadedClasses[argIndex++];
				
					var actionButton = $("<span class='hidden'>" + actionDefinition.label + "</span>")
							.addClass(actionDefinition.action + "_selected ajaxCallbackButton container_action")
							.appendTo(groupSpan);
				
					actionButton.data('actionObject', new ActionButton({
						actionClass : actionClass,
						context : {
							action : actionClass,
							target : self.resultObjectList,
							anchor : actionButton
						},
						actionHandler : self.actionHandler,
					}, actionButton));

					actionButton.click(function(){
						$(this).data('actionObject').activate();
					});
					self.actionButtons.push(actionButton);
				}
			}
		
			$(window).resize();
		});
	};
	
	ResultTableActionMenu.prototype.selectionUpdated = function() {
		for (var i in this.actionButtons) {
			var actionButton = this.actionButtons[i];
			var actionObject = actionButton.data('actionObject');
			if (actionObject.action.hasTargets()) {
				actionButton.removeClass("hidden");
			} else {
				actionButton.addClass("hidden");
			}
		}
		
		for (var i in this.actionGroups) {
			var actionGroup = this.actionGroups[i];
			var visibleChildren = actionGroup.children(".container_action").not(".hidden");
			if (visibleChildren.length == 0) {
				actionGroup.css('display', 'none');
			} else {
				visibleChildren.removeClass("first_visible").first().addClass("first_visible");
				actionGroup.css('display', 'inline-block')
			}
		}
	};
	
	return ResultTableActionMenu;
});