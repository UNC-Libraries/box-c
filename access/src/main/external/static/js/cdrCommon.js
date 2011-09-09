$(function() {
	$('.clear_on_first_focus').each(function() {
		$(this).data("initial_text", $(this).val());
		$(this).data("been_cleared", false);
	}).focus(function() {
		if ($(this).val() == $(this).data("initial_text")) {
			$(this).val("");
			$(this).data("been_cleared", true);
		}
	}).blur(function() {
		if ($(this).val() == "") {
			$(this).val($(this).data("initial_text"));
			$(this).data("been_cleared", false);
		}
	});

	$(".clear_on_submit_without_focus").submit(function() {
		var childClearField = $(this).find('.clear_on_first_focus');
		if (childClearField.data("been_cleared") == false) {
			childClearField.val("");
		}
	});

	
	var tooltipSettings = {
			content: {
				text: false
			},
			show: {
				delay: 300
			},
			position: {
				target: 'mouse',
				corner: {
					target: 'bottomMiddle',
					tooltip: 'topLeft'
				},
				adjust: {
					screen: true
				}
			},
			style: {
				border: {
					width: 0
				},
				tip: {
					corner: 'topRight',
					color: '#f2f2f2',
					size: {
						x: 7,
						y: 5
					}
				}
			}
		}; 
	
	$(".has_tooltip").qtip(tooltipSettings);
});

//google analytics
var _gaq = _gaq || [];
_gaq.push([ '_setAccount', 'UA-17932020-4' ]);
_gaq.push([ '_trackPageview' ]);

(function() {
	var ga = document.createElement('script');
	ga.type = 'text/javascript';
	ga.async = true;
	ga.src = ('https:' == document.location.protocol ? 'https://ssl'
			: 'http://www')
			+ '.google-analytics.com/ga.js';
	var s = document.getElementsByTagName('script')[0];
	s.parentNode.insertBefore(ga, s);
})();