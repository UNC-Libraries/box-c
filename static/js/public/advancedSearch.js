require.config({
	urlArgs: "v=4.0-SNAPSHOT",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-access',
		'jquery-ui' : 'cdr-access',
		'datepicker' : 'lib/jquery-ui-datepicker.min',
		'qtip' : 'lib/jquery.qtip.min',
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'datepicker' : ['jquery'],
		'qtip' : ['jquery']
	}
});
define('advancedSearch', ['module', 'jquery', 'datepicker', 'qtip'], function(module, $) {
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
