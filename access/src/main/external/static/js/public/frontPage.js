require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'qtip' : 'jquery.qtip.min',
		'cdrCommon' : 'cdrCommon',
		'featuredContent' : 'featuredContent'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'qtip' : ['jquery'],
		'thumbnail' : ['jquery'],
		'cdrCommon' : ['jquery', 'qtip'],
		'featuredContent' : ['jquery']
	}
});
define('frontPage', ['module', 'jquery', 'cdrCommon', 'featuredContent'], function(module, $) {
	$("#slideshow").easySlider({
		prevId: 'prev_button',
		prevText: "<div></div><img src='/static/images/left_slideshow_arrow.png'/>",
		nextId: 'next_button',
		nextText: "<div></div><img src='/static/images/right_slideshow_arrow.png'/>",
		controlsShow: true,
		fade: true,
		speed: 500
	});
});