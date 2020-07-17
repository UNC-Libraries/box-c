define('ChangeDepositPipelineStateAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {
	function ChangeDepositPipelineStateAction(options) {
		this._create(options);
	};
	
	ChangeDepositPipelineStateAction.prototype.constructor = ChangeDepositPipelineStateAction;
	ChangeDepositPipelineStateAction.prototype = Object.create( AjaxCallbackAction.prototype );
	
	ChangeDepositPipelineStateAction.prototype.actionName = "Delete";
		
	ChangeDepositPipelineStateAction.prototype._create = function(context) {
		this.context = context;
		
		this.options = {
			workMethod: "post",
			workPath: "/services/api/edit/depositPipeline/" + context.pipelineAction,
			confirm: {
				promptText: context.pipelineAction + " the deposit pipeline?",
				confirmAnchor: context.confirmAnchor
			}
		};
		
		AjaxCallbackAction.prototype._create.call(this, this.options);
	};
	
	ChangeDepositPipelineStateAction.prototype.completeState = function() {
		this.alertHandler.alertHandler("success", "Changing state of the deposit pipeline: " + this.context.pipelineAction);
	};

	return ChangeDepositPipelineStateAction;
});