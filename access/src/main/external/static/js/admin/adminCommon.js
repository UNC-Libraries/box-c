$(function() {
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