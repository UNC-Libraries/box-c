require.config({
	urlArgs: "v=3.4-SNAPSHOT",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-access',
		'jquery-ui' : 'cdr-access'
	},
	shim: {
		'jquery-ui' : ['jquery']
	}
});

define('studentPapers', ['module', 'jquery'], function(module, $) {
	var selector = $("#mp-dept");

	selector.on('click', function() {
		redirectLink(this, null); // Required for Safari to redirect the page

		$(this).on('change', function() {
			redirectLink(this, null); // Required for Chrome and FireFox to redirect the page
		});
	});

	selector.on('keypress', function(e) {
		var key_code = e.keyCode || e.which;
		redirectLink(this, key_code);
	});

	function redirectLink(context, key_code) {
		var link = { uri: $(context) };
		var url = link.uri.val();
		var reset = $.extend(true, {}, link);


		if (key_code !== null) {
			if (url !== '' && key_code === 13) {
				reset.uri.val('');
				location.href = url;
			}
		} else {
			if (url !== '') {
				reset.uri.val('');
				location.href = url;
			}
		}
	}
});
