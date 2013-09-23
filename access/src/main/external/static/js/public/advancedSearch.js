$(function() {
	$(".advsearch_date").datepicker({showOn: false});
	$(".advsearch_date").datepicker("option", "dateFormat", "yy-mm-dd");
	$(".date_field_tooltip").qtip({
		content: {
			text: false
		},
		position: {
			corner: {
				target: 'bottomMiddle',
				tooltip: 'topLeft'
			},
			adjust: {
				screen: true
			}
		},
		style: {
			classes: {
				content: "tooltip_adv_search"
			},
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
	});
});