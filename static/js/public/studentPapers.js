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
		$(this).on('change', function() {
			if ($(this).val() !== '') {
				location.href = $(this).val();
			}
		});
	});

	selector.on('keypress', function(e) {
		var code = e.keyCode || e.which;

		if ($(this).val() !== '' && code === 13) {
			location.href = $(this).val();
		}
	});
});
