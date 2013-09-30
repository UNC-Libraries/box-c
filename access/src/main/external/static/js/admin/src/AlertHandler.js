define('AlertHandler', ['jquery', 'jquery-ui', 'qtip'], function($) {
	$.widget("cdr.alertHandler", {
		_create: function() {
			// Utilise delegate so we don't have to rebind for every qTip!
			$(document).delegate('.qtip.jgrowl', 'mouseover mouseout', this.timer);
		},
		
		error: function(message) {
			this.showAlert(message, "error");
		},
		
		message: function(message) {
			this.showAlert(message, "info");
		},
		
		success: function(message) {
			this.showAlert(message, "success");
		},
		
		showAlert: function(messages, type) {
			if (!messages)
				return;
			if (messages instanceof Array) {
				for (var index in messages)
					this._renderAlert(messages[index], type);
			} else {
				this._renderAlert(messages, type);
			}
		},
		
		_renderAlert: function(message, type) {
			var target = $('.qtip.jgrowl:visible:last');
			var self = this;
			
			$(document.body).qtip({
					content: {
						text: message,
						title: {
							text: "",
							button: true
						}
					},
					position: {
						my: 'top right',
						at: (target.length ? 'bottom' : 'top') + ' right',
						target: target.length ? target : $(window),
						adjust: { 'y': 10, 'x' : target.length ? 0 : -5 },
						effect: function(api, newPos) {
							$(this).animate(newPos, {
									duration: 200,
									queue: false
							});
							api.cache.finalPos = newPos; 
						}
					},
					show: {
						event: false,
						// Don't show it on a regular event
						ready: true,
						// Show it when ready (rendered)
						effect: function() {
							$(this).stop(0, 1).fadeIn(400);
						},
						// Matches the hide effect
						delay: 0,
						persistent: false
					},
					hide: {
						event: false,
						// Don't hide it on a regular event
						effect: function(api) {
							// Do a regular fadeOut, but add some spice!
							$(this).stop(0, 1).fadeOut(400).queue(function() {
							// Destroy this tooltip after fading out
							api.destroy();
							// Update positions
							self.updateGrowls();
						});
					}
				},
				style: {
					classes: 'jgrowl qtip-admin qtip-rounded alert-' + type,
					tip: false
				},
				events: {
					render: function(event, api) {
						// Trigger the timer (below) on render
						self.timer.call(api.elements.tooltip, event);
					}
				}
			}).removeData('qtip');
		},
		
		updateGrowls : function() {
			// Loop over each jGrowl qTip
			var each = $('.qtip.jgrowl'),
				width = each.outerWidth(),
				height = each.outerHeight(),
				gap = each.eq(0).qtip('option', 'position.adjust.y'),
				pos;
	 
			each.each(function(i) {
				var api = $(this).data('qtip');
	 
				// Set target to window for first or calculate manually for subsequent growls
				api.options.position.target = !i ? $(window) : [
					pos.left + width, pos.top + (height * i) + Math.abs(gap * (i-1))
				];
				api.set('position.at', 'top right');
				
				// If this is the first element, store its finak animation position
				// so we can calculate the position of subsequent growls above
				if(!i) { pos = api.cache.finalPos; }
			});
		},
		
		// Setup our timer function
		timer : function(event) {
			var api = $(this).data('qtip'),
				lifespan = 5000;
			
			// If persistent is set to true, don't do anything.
			if (api.get('show.persistent') === true) { return; }
	 
			// Otherwise, start/clear the timer depending on event type
			clearTimeout(api.timer);
			if (event.type !== 'mouseover') {
				api.timer = setTimeout(api.hide, lifespan);
			}
		}
	});
});