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
define([ 'jquery', 'jquery-ui', 'PID', 'RemoteStateChangeMonitor', 'ModalLoadingOverlay', 'ConfirmationDialog'], function(
		$, ui, PID, RemoteStateChangeMonitor) {
	$.widget("cdr.ajaxCallbackButton", {
		options : {
			pid : null,
			defaultLabel : undefined,
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
			alertHandler : "#alertHandler"
		},

		_create : function() {
			if (!this.options.defaultLabel)
				this.options.defaultLabel = this.element.text();
			if (!this.options.workLabel)
				this.options.workLabel = this.element.text();
			if (this.options.workDoneTarget == undefined)
				this.options.workDoneTarget = this;
			if (this.options.completeTarget == undefined)
				this.options.completeTarget = this;
			if (this.options.followupTarget == undefined)
				this.options.followupTarget = this;
			if (this.options.setText == undefined)
				this.options.setText = this.setText;

			this.element.addClass("ajaxCallbackButton");

			this.alertHandler = $(this.options.alertHandler);
			
			if (this.options.pid !== undefined && this.options.pid != null) {
				if (this.options.pid instanceof PID)
					this.pid = this.options.pid;
				else
					this.pid = new PID(this.options.pid);
			}
			this.setWorkURL(this.options.workPath);
			
			this.followupId = null;
		},
		
		_init : function() {
			var op = this;
			
			if (this.options.followup) {
				this.setFollowupURL(this.options.followupPath);

				this.followupMonitor = new RemoteStateChangeMonitor({
					'checkStatus' : this.options.followup,
					'checkStatusTarget' : this.options.followupTarget,
					'statusChanged' : this.completeState,
					'statusChangedTarget' : this.options.completeTarget, 
					'checkStatusAjax' : {
						url : this.followupURL,
						dataType : 'json'
					}
				});
			}
			
			if (this.options.confirm) {
				this.element.confirmationDialog({
					'promptText' : this.options.confirmMessage,
					'confirmFunction' : this.doWork,
					'confirmTarget' : this,
					'dialogOptions' : {
						width : 200
					}
				});
			}

			this.element.text(this.options.defaultLabel);
			this.element.click(function() {
				op.activate.call(op);
				return false;
			});
			
			if (this.options.disabled){
				this.disable();
			} else {
				this.enable();
			}
		},
		
		activate : function() {
			if (this.options.disabled)
				return;
			if (this.options.confirm) {
				this.element.confirmationDialog("open");
			} else {
				this.doWork();
			}
		},

		doWork : function(workMethod, workData) {
			if (this.options.disabled)
				return;
			this.performWork($.get, null);
		},

		workState : function() {
			this.disable();
			if (this.options.parentObject) {
				this.options.parentObject.setState("working");
				this.options.parentObject.setStatusText(this.options.workLabel);
			} else {
				this.element.text(this.options.workLabel);
			}
		},

		performWork : function(workMethod, workData) {
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
						}
					}
					if (op.options.parentObject)
						op.options.parentObject.setState("followup");
					op.followupMonitor.performPing();
				} else {
					if (op.options.parentObject)
						op.options.parentObject.setState("idle");
					op.options.complete.call(op.options.completeTarget, data);
					op.enable();
				}
			}).fail(function(jqxhr, textStatus, error) {
				op.alertHandler.alertHandler('error', textStatus + ", " + error);
			});
		},

		disable : function() {
			this.options.disabled = true;
			this.element.css("cursor", "default");
			this.element.addClass("disabled");
			this.element.attr('disabled', 'disabled');
		},

		enable : function() {
			this.options.disabled = false;
			this.element.css("cursor", "pointer");
			this.element.removeClass("disabled");
			this.element.removeAttr('disabled');
		},

		setWorkURL : function(url) {
			this.workURL = url;
			this.workURL = this.resolveParameters(this.workURL);
		},

		setFollowupURL : function(url) {
			this.followupURL = url;
			this.followupURL = this.resolveParameters(this.followupURL);
		},

		resolveParameters : function(url) {
			if (!url || !this.pid)
				return url;
			return url.replace("{idPath}", this.pid.getPath());
		},

		destroy : function() {
			this.element.unbind("click");
		},

		followupState : function() {
			if (this.options.followupLabel != null) {
				if (this.options.parentObject)
					this.options.parentObject.setStatusText(this.options.followupLabel);
				else 
					this.element.text(this.options.followupLabel);

			}
		},

		completeState : function(data) {
			if (this.options.parentObject) {
				this.options.parentObject.setState("idle");
			}
			this.enable();
			this.element.text(this.options.defaultLabel);
		}
	});
});