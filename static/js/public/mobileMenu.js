require.config({
    urlArgs: "v=3.4-SNAPSHOT",
    baseUrl: '/static/js/',
    paths: {
        'jquery' : 'cdr-access'
    }
});
define('mobileMenu', ['module', 'jquery'], function(module, $) {
    $('#navbar-burger').on('click touchstart', function(e) {
        var self = $(this);
        var mobile_menu = $('#' + self.attr('data-target'));

        self.toggleClass('open');
        mobile_menu.toggleClass('is-active');

        if (self.hasClass('open')) {
            self.attr('aria-expanded', true);
        } else {
            self.attr('aria-expanded', false)
        }
    });
});