/*

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */
/*
 * @author Ben Pennell
 */
define('AjaxCallbackButton', [ 'jquery', 'jquery-ui', 'RemoteStateChangeMonitor', 'ConfirmationDialog'], function(
		$, ui, RemoteStateChangeMonitor, ConfirmationDialog) {
	function AjaxCallbackButton(options) {
		this._create(options);
	};
	
	AjaxCallbackButton.prototype.defaultOptions = {
			pid : null,
			metadata : undefined,
			element : undefined,
			defaultLabel : undefined,
			workMethod : $.get,
			workLabel : undefined,
			workPath : "",
			workDone : undefined,
			workDoneTarget : undefined,
			followup : undefined,
			followupTarget : undefined,
			followupPath : "",
			followupLabel : undefined,
			followupFrequency : 1000,
			completeTarget : undefined,
			parentElement : undefined,
			animateSpeed : 80,
			confirm : false,
			confirmMessage : "Are you sure?",
			confirmAnchor : undefined,
			alertHandler : "#alertHandler"
		};

	AjaxCallbackButton.prototype._create = function(options, element) {
		this.options = $.extend({}, this.defaultOptions, options);
		
		if (element) {
			this.element = element;
			if (!this.options.defaultLabel)
				this.options.defaultLabel = this.element.text();
			if (!this.options.workLabel)
				this.options.workLabel = this.element.text();
			this.element.addClass("ajaxCallbackButton");
		}
		if (this.options.workDoneTarget == undefined)
			this.options.workDoneTarget = this;
		if (this.options.completeTarget == undefined)
			this.options.completeTarget = this;
		if (this.options.followupTarget == undefined)
			this.options.followupTarget = this;
		if (this.options.setText == undefined)
			this.options.setText = this.setText;
		if (this.options.followupError == undefined)
			this.options.followupError = this.followupError;
		if (this.options.followupErrorTarget == undefined)
			this.options.followupErrorTarget = this;
		this.alertHandler = $(this.options.alertHandler);
		
		if (this.options.pid) {
			this.pid = this.options.pid;
		}
		this.setWorkURL(this.options.workPath);
		
		this.followupId = null;
		this._init();
	};
	
	AjaxCallbackButton.prototype._init = function() {
		var op = this;
		
		if (this.options.followup) {
			this.setFollowupURL(this.options.followupPath);

			this.followupMonitor = new RemoteStateChangeMonitor({
				'checkStatus' : this.options.followup,
				'checkStatusTarget' : this.options.followupTarget,
				'checkError' : this.options.followupError,
				'checkErrorTarget' : this.options.followupErrorTarget,
				'statusChanged' : this.completeState,
				'statusChangedTarget' : this.options.completeTarget, 
				'checkStatusAjax' : {
					url : this.followupURL,
					dataType : 'json'
				}
			});
		}
		
		if (this.options.confirm) {
			
		}

		/*this.element.text(this.options.defaultLabel);
		this.element.click(function() {
			op.activate.call(op);
			return false;
		});*/
		
		if (this.options.disabled){
			this.disable();
		} else {
			this.enable();
		}
	};
	
	AjaxCallbackButton.prototype.activate = function() {
		if (this.options.disabled)
			return;
		if (this.options.confirm) {
			var dialogOptions = {
					width : 'auto',
					modal : true
				};
			if (this.options.parentObject) {
				this.options.parentObject.highlight();
				dialogOptions['close'] = function() {
					op.options.parentObject.unhighlight();
				};
			}
				
			if (this.options.confirmAnchor) {
				dialogOptions['position'] = {};
				dialogOptions['position']['of'] = this.options.confirmAnchor; 
			}
		
			var confirmationDialog = new ConfirmationDialog({
				'promptText' : this.options.confirmMessage,
				'confirmFunction' : this.doWork,
				'confirmTarget' : this,
				'dialogOptions' : dialogOptions,
				autoOpen : true
			});
		} else {
			this.doWork();
		}
	};

	AjaxCallbackButton.prototype.doWork = function(workMethod, workData) {
		if (this.options.disabled)
			return;
		this.performWork(this.options.workMethod, null);
	};

	AjaxCallbackButton.prototype.workState = function() {
		this.disable();
		if (this.options.parentObject) {
			this.options.parentObject.setState("working");
			this.options.parentObject.setStatusText(this.options.workLabel);
		} else if (this.element) {
			this.element.text(this.options.workLabel);
		}
	};

	AjaxCallbackButton.prototype.performWork = function(workMethod, workData) {
		this.workState();
		var op = this;
		workMethod(this.workURL, workData, function(data, textStatus, jqXHR) {
			if (op.options.followup) {
				if (op.options.workDone) {
					try {
						var workSuccessful = op.options.workDone.call(op.options.workDoneTarget, data);
						if (!workSuccessful)
							throw "Operation was unsuccessful";
					} catch (e) {
						op.alertHandler.alertHandler('error', e);
						if (op.options.parentObject)
							op.options.parentObject.setState("failed");
						return;
					}
				}
				if (op.options.parentObject)
					op.options.parentObject.setState("followup");
				op.followupMonitor.performPing();
			} else {
				if (op.options.parentObject)
					op.options.parentObject.setState("idle");
				if (op.options.complete)
					op.options.complete.call(op.options.completeTarget, data);
				op.enable();
			}
		}).fail(function(jqxhr, textStatus, error) {
			op.alertHandler.alertHandler('error', textStatus + ", " + error);
		});
	};
	
	AjaxCallbackButton.prototype.followupError = function(obj, errorText, error) {
		this.alertHandler.alertHandler('error', "An error occurred while checking the status of " + (this.options.metadata? this.options.metadata.title : "an object"));
		if (console && console.log)
			console.log((this.options.metadata? "Error while checking " + this.options.metadata.id + ": " : "") +errorText, error);
		if (this.options.parentObject)
			this.options.parentObject.setState("failed");
	};

	AjaxCallbackButton.prototype.disable = function() {
		this.options.disabled = true;
		if (this.element) {
			this.element.css("cursor", "default");
			this.element.addClass("disabled");
			this.element.attr('disabled', 'disabled');
		}
	};

	AjaxCallbackButton.prototype.enable = function() {
		this.options.disabled = false;
		if (this.element) {
			this.element.css("cursor", "pointer");
			this.element.removeClass("disabled");
			this.element.removeAttr('disabled');
		}
	};

	AjaxCallbackButton.prototype.setWorkURL = function(url) {
		this.workURL = url;
		this.workURL = this.resolveParameters(this.workURL);
	};

	AjaxCallbackButton.prototype.setFollowupURL = function(url) {
		this.followupURL = url;
		this.followupURL = this.resolveParameters(this.followupURL);
	};

	AjaxCallbackButton.prototype.resolveParameters = function(url) {
		if (!url || !this.pid)
			return url;
		return url.replace("{idPath}", this.pid);
	};

	AjaxCallbackButton.prototype.destroy = function() {
		if (this.element)
			this.element.unbind("click");
	};

	AjaxCallbackButton.prototype.followupState = function() {
		if (this.options.followupLabel != null) {
			if (this.options.parentObject)
				this.options.parentObject.setStatusText(this.options.followupLabel);
			else if (this.element) 
				this.element.text(this.options.followupLabel);

		}
	};

	AjaxCallbackButton.prototype.completeState = function(data) {
		if (this.options.parentObject) {
			this.options.parentObject.setState("idle");
		}
		this.enable();
		if (this.element)
			this.element.text(this.options.defaultLabel);
	};
	
	return AjaxCallbackButton;
});