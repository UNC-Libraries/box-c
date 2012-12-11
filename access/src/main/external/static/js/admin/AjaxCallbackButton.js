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
(function($) {
	$.widget( "cdr.ajaxCallbackButton", {
		options: {
			pid: null,
			defaultLabel: undefined,
			workLabel: undefined,
			workPath: "",
			workDone: undefined,
			workDoneTarget: undefined,
			followup: undefined,
			followupTarget: undefined,
			followupPath: "",
			followupLabel: undefined,
			followupFrequency: 1000,
			complete: this.defaultComplete,
			completeTarget: undefined,
			parentElement: undefined,
			animateSpeed: 80,
			confirm: false,
			confirmMessage: undefined
		},
		
		_create: function() {
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
			
			this.followupId = null;
			if (this.options.pid instanceof PID)
				this.pid = this.options.pid;
			else this.pid = new PID(this.options.pid);
			this.setWorkURL(this.options.workPath);
			this.setFollowupURL(this.options.followupPath);
			
			var op = this;
			
			if (this.options.confirm) {
				this.confirmDialog = $("<div class='confirm_dialogue'></div>");
				if (this.options.confirmMessage === undefined) {
					this.confirmDialog.append("<p>Are you sure?</p>");
				} else {
					this.confirmDialog.append("<p>" + this.options.confirmMessage + "</p>");
				}
				$("body").append(this.confirmDialog);
				this.confirmDialog.dialog({
					dialogClass: "no_titlebar",
					position: {my: "right top", at: "right bottom", of: op.element},
		            resizable: false,
		            minHeight: 60,
		            width: 180,
		            modal: false,
		            autoOpen: false,
		            buttons: {
		                "Yes": function() {
		                    op.doWork();
		                    $(this).dialog("close");
		                },
		                "Cancel": function() {
		                    $(this).dialog("close");
		                }
		            }
		        });
			}
			
			this.element.text(this.options.defaultLabel);
			this.element.click(function(){
				if (op.options.disabled)
					return false;
				if (op.options.confirm) {
					op.confirmDialog.dialog("open");
				} else {
					op.doWork();
				}
				
				return false;
			});
		},
		
		doWork: function() {
			this.element.text(this.options.workLabel);
			this.disable();
			if (this.options.parentObject) 
				this.options.parentObject.setState("working");
			var op = this;
			$.getJSON(this.workURL, function(data) {
				if (op.options.followup) {
					if (op.options.workDone) {
						var workSuccessful = op.options.workDone.call(op.options.workDoneTarget, data);
						if (!workSuccessful)
							return;
					}
					if (op.options.parentObject) 
						op.options.parentObject.setState("followup");
					op.followupPing();
				} else {
					if (op.options.parentObject) 
						op.options.parentObject.setState("idle");
					op.options.complete.call(op.options.completeTarget, data);
					op.enable();
				}
			});
		},
		
		disable: function() {
			this.options.disabled = true;
			this.element.css("cursor", "default");
		},
		
		enable: function() {
			this.options.disabled = false;
			this.element.css("cursor", "pointer");
		},
		
		setWorkURL: function(url) {
			this.workURL = url;
			this.workURL = this.resolveParameters(this.workURL);
		},
		
		setFollowupURL: function(url) {
			this.followupURL = url;
			this.followupURL = this.resolveParameters(this.followupURL);
		},
		
		resolveParameters: function(url) {
			return url.replace("{idPath}", this.pid.getPath());
		},
		
		destroy: function() {
			this.element.unbind("click");
		},
		
		followupPing: function() {
			var op = this;
			if (this.options.followupLabel != null) {
				this.element.text(this.options.followupLabel);
			}
			$.getJSON(this.followupURL, function(data) {
				var isDone = op.options.followup.call(op.options.followupTarget, data);
				if (isDone) {
					if (op.followupId != null) {
						clearInterval(op.followupId);
						op.followupId = null;
					}
					if (op.options.parentObject) {
						op.options.parentObject.setState("idle");
					}
					op.enable();
					op.options.complete.call(op.options.completeTarget, data);
				} else if (op.followupId == null) {
					op.followupId = setInterval($.proxy(op.followupPing, op), op.options.followupFrequency);
				}
			});
		},
		
		defaultComplete: function(data) {
			this.element.text(op.options.defaultLabel);
		}
	});
})(jQuery);