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
define([ 'jquery', 'jquery-ui'], function($, ui) {
	$.widget("cdr.resizableAccordionMenu", {
		options : {
			subMenu : "div",
			alsoResize : "",
			minWidth: 300,
			maxWidth: 600
		},
		
		_create : function() {
			this.subMenus = $(this.element).children("div").clone();
			
			this.subMenus.accordion({
				header: "> div > h3",
				heightStyle: "content",
				collapsible: true,
				active: false,
				activate: function(event, ui) {
					if (ui.newPanel.attr('data-href') != null && !ui.newPanel.data('contentLoaded')) {
						ui.newPanel.load(ui.newPanel.attr('data-href'));
						ui.newPanel.data('contentLoaded', true);
					}
				}
			}).accordion('activate', 0);
			
			$(this.element).html(this.subMenus);
			
			$(this.element).resizable({
				handles: 'e',
				alsoResize : this.options.alsoResize,
				minWidth: this.options.minWidth,
				maxWidth: this.options.maxWidth
			}).css('visibility', 'visible');
		}
	});
});