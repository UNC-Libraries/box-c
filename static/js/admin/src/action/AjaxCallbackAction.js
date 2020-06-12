define('AjaxCallbackAction', [ 'jquery', 'jquery-ui', 'RemoteStateChangeMonitor', 'ConfirmationDialog'], function(
		$, ui, RemoteStateChangeMonitor, ConfirmationDialog) {
	
	/**
	Flow:
	execute > doWork > workState > workDone > followup* > completeState
	OR
	execute > doWork > workState > workDone > complete > completeState
	*/
	
	function AjaxCallbackAction(options) {
		this._create(options);
	};
	
	AjaxCallbackAction.prototype.actionName = "Action";
	
	AjaxCallbackAction.prototype.defaultOptions = {
			workMethod : "get",
			workLabel : undefined,
			workPath : "",
			followup : true,
			followupPath : "",
			followupFrequency : 1000,
			confirm : false,
			alertHandler : "#alertHandler"
		};

	AjaxCallbackAction.prototype._create = function(options, target) {
		this.options = $.extend({}, this.defaultOptions, options);
		
		this.alertHandler = $(this.options.alertHandler);
		
		this.setWorkURL(this.options.workPath);
		
		this.followupId = null;
		this._init();
	};
	
	AjaxCallbackAction.prototype._init = function() {
		var op = this;
		
		if (this.options.followup) {
			this.setFollowupURL(this.options.followupPath);

			this.followupMonitor = new RemoteStateChangeMonitor({
				checkStatus : this.followup,
				checkStatusTarget : this,
				checkError : this.options.followupError,
				checkErrorTarget : this,
				statusChanged : this.completeState,
				statusChangedTarget : this,
				pingFrequency : this.options.followupFrequency,
				checkStatusAjax : {
					url : this.followupURL,
					dataType : 'json'
				}
			});
		}
	};
	
	AjaxCallbackAction.prototype.execute = function() {
		if (this.options.confirm) {
			var confirmOptions = $.extend({
				promptText : "Are you sure?",
				confirmFunction : this.doWork,
				confirmTarget : this,
				autoOpen : true,
				dialogOptions : {
					width : 'auto',
					modal : true
				}
			}, this.options.confirm);
				
			if (confirmOptions.confirmAnchor) {
				confirmOptions.dialogOptions['position'] = {};
				confirmOptions.dialogOptions['position']['of'] = confirmOptions.confirmAnchor; 
			}
		
			var confirmationDialog = new ConfirmationDialog(confirmOptions);
		} else {
			this.doWork();
		}
	};

	AjaxCallbackAction.prototype.doWork = function() {
		this.performWork(this.options.workMethod, this.options.workData);
	};

	AjaxCallbackAction.prototype.workState = function() {
		if (this.context.target) {
			this.context.target.setState("working");
			this.context.target.setStatusText(this.options.workLabel);
		}
	};

	AjaxCallbackAction.prototype.performWork = function(workMethod, workData) {
		this.workState();
		var op = this;
		
		$.ajax({
			type: workMethod,
			url: this.workURL,
			dataType: "json",
			data: workData
		}).done(function(data) {
			if (op.options.followup) {
				try {
					var workSuccessful = op.workDone(data);
					
					if (!workSuccessful)
						throw "Operation was unsuccessful";
				} catch (e) {
					op.alertHandler.alertHandler('error', e.message);
					if (typeof console == "object")
						console.error(e.message, e.error);
					if (op.context.target)
						op.context.target.setState("failed");
					return;
				}
				if (op.context.target)
					op.context.target.setState("followup");
				op.followupMonitor.performPing();
			} else {
				if (op.context.target)
					op.context.target.setState("idle");
				op.complete(data);
			}

			// Trigger unchecking all checked objects when action completes
			$(".select_all").trigger('click', function(){
				var checkbox = $(this).children("input").prop("checked", false);
				var toggleFn = checkbox.prop("checked") ? "select" : "unselect";
				var resultObjects = op.resultObjectList.resultObjects;
				for (var index in resultObjects) {
					resultObjects[index][toggleFn]();
				}
				op.selectionUpdated();
			}).children("input").prop("checked", false);

		}).fail(function(data, error) {
			op.alertHandler.alertHandler('error', data.responseText + ", " + error);
			console.error(data.responseText, error);
		});
	};
	
	AjaxCallbackAction.prototype.workDone = function(data) {
		this.completeTimestamp = data.timestamp;
		return true;
	};

	AjaxCallbackAction.prototype.setWorkURL = function(url) {
		this.workURL = url;
		this.workURL = this.resolveParameters(this.workURL);
	};

	AjaxCallbackAction.prototype.setFollowupURL = function(url) {
		this.followupURL = url;
		this.followupURL = this.resolveParameters(this.followupURL);
	};

	AjaxCallbackAction.prototype.resolveParameters = function(url) {
		if (!url || !this.context.target.pid)
			return url;
		return url.replace("{idPath}", this.context.target.pid);
	};
	
	AjaxCallbackAction.prototype.followup = function(data) {
		return true;
	};

	AjaxCallbackAction.prototype.followupState = function() {
		if (this.options.followupLabel != null) {
			if (this.context.target)
				this.context.target.setStatusText(this.options.followupLabel);
		}
	};
	
	AjaxCallbackAction.prototype.followupError = function(obj, errorText, error) {
		this.alertHandler.alertHandler('error', "An error occurred while checking the status of " + (this.options.metadata? this.options.metadata.title : "an object"));
		if (console && console.log)
			console.log((this.options.metadata? "Error while checking " + this.options.metadata.id + ": " : "") +errorText, error);
		if (this.context.target)
			this.context.target.setState("failed");
	};

	AjaxCallbackAction.prototype.complete = function(data) {
	};

	AjaxCallbackAction.prototype.completeState = function(data) {
		if (this.context.target) {
			this.context.target.setState("idle");
		}
	};
	
	return AjaxCallbackAction;
});