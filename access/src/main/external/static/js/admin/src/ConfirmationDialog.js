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
define([ 'jquery', 'jquery-ui', 'PID', 'RemoteStateChangeMonitor', 'ModalLoadingOverlay'], function(
		$, ui, PID, RemoteStateChangeMonitor) {
	$.widget("cdr.confirmationDialog", {
		options : {
			'promptText' : 'Are you sure?',
			'confirmFunction' : undefined,
			'confirmTarget' : undefined,
			'confirmText' : 'Yes',
			'cancelText' : 'Cancel',
			'dialogOptions' : {
				modal : false,
				minHeight : 60,
				autoOpen : false,
				resizable : false,
				dialogClass : "no_titlebar confirm_dialog"
			},
			'solo' : true
		},
		
		_create : function() {
			var self = this;
			
			this.confirmDialog = $("<div class='confirm_dialogue'></div>");
			if (this.options.promptText === undefined) {
				this.confirmDialog.append("<p>Are you sure?</p>");
			} else {
				this.confirmDialog.append("<p>" + this.options.promptText + "</p>");
			}
			$("body").append(this.confirmDialog);
			
			var buttonsObject = {};
			
			buttonsObject[self.options.cancelText] = function() {
				$(this).dialog("close");
			};
			
			buttonsObject[self.options.confirmText] = function() {
				if (self.options.confirmFunction) {
					self.options.confirmFunction.call(self.options.confirmTarget);
				}
				$(this).dialog("close");
			};
			
			var dialogOptions = $.extend({}, this.options.dialogOptions, {
				position : {
					my : "right top",
					at : "right bottom",
					of : self.element
				},
				open : function() {
					if (self.options.solo) {
						$.each($('div.ui-dialog-content'), function (i, e) {
							if ($(this).dialog("isOpen") && this !== self.confirmDialog[0]) 
								$(this).dialog("close");
						});
					}
				},
				buttons : buttonsObject
			});
			this.confirmDialog.dialog(dialogOptions);
		},
		
		open : function () {
			this.confirmDialog.dialog('open');
		},
		
		close : function () {
			this.confirmDialog.dialog('close');
		}
	});
});