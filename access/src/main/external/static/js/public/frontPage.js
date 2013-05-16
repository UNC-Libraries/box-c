require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'featuredContent' : 'featuredContent'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'thumbnail' : ['jquery'],
		'featuredContent' : ['jquery']
	}
});
define('frontPage', ['module', 'jquery', 'featuredContent'], function(module, $) {
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