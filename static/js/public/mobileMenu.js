(function() {
    var mobile_burger = document.getElementById('navbar-burger');

    mobile_burger.addEventListener('click', toggleMenu, false);
    mobile_burger.addEventListener('touchstart', toggleMenu, false);

    function toggleMenu() {
        var target_attribute = mobile_burger.getAttribute('data-target');
        var mobile_menu = document.getElementById(target_attribute);

        mobile_burger.classList.toggle('open');
        mobile_menu.classList.toggle('is-active');

        if (mobile_burger.classList.contains('open')) {
            mobile_burger.setAttribute('aria-expanded', 'true');
        } else {
            mobile_burger.setAttribute('aria-expanded', 'false');
        }
    }
})();