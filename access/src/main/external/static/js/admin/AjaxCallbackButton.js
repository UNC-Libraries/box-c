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
			followup: undefined,
			followupPath: "",
			followupLabel: undefined,
			followupFrequency: 1000,
			complete: this.defaultComplete,
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
			if (this.options.parentElement)
				this.options.parentElement.addClass("working", "fast");
			var op = this;
			$.getJSON(this.workURL, function(data) {
				if (op.options.followup) {
					if (op.options.workDone) {
						var workSuccessful = op.options.workDone.call(op, data);
						if (!workSuccessful)
							return;
					}
					if (op.options.parentElement) 
						op.options.parentElement.switchClass("working", "followup", op.options.animateSpeed);
					op.followupPing();
				} else {
					if (op.options.parentElement)
						op.options.parentElement.removeClass("working", op.options.animateSpeed);
					op.options.complete.call(op, data);
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
				var isDone = op.options.followup.call(op, data);
				if (isDone) {
					if (op.followupId != null) {
						clearInterval(op.followupId);
						op.followupId = null;
					}
					if (op.options.parentElement) {
						op.options.parentElement.removeClass("followup", op.options.animateSpeed);
					}
					op.enable();
					op.options.complete.call(op, data);
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