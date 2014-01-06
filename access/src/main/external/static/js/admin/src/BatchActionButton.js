define('BatchActionButton', ['jquery', 'ActionButton', 'ResultObjectList' ], function($, ActionButton, ResultObjectList) {

	function BatchActionButton(options, element) {
		this._create(options, element);
	};
	
	BatchActionButton.prototype.constructor = BatchActionButton;
	BatchActionButton.prototype = Object.create( ActionButton.prototype );
	
	var defaultOptions = {
			confirmAnchor : undefined,
			confirmMessage : "Are you sure?",
			animateSpeed: 'fast',
			
			resultObjectList : undefined
		};
		
	BatchActionButton.prototype.getTargetIds = function() {
		var targetIds = [];
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (this.isValidTarget(resultObject))
				targetIds.push(resultObject.getPid());
		}
		return targetIds;
	};

	BatchActionButton.prototype.hasTargets = function() {
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (this.isValidTarget(resultObject))
				return true;
		}
		return false;
	};
	
	BatchActionButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected();
	};
	
	return BatchActionButton;
});
