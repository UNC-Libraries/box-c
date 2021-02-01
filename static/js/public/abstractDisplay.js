(function() {
    var partial_abstract = document.getElementById('truncated-abstract');

    if (partial_abstract !== null) {
        var show_abstract = document.getElementById('show-abstract');
        var hide_abstract = document.getElementById('hide-abstract');
        var full_abstract = document.getElementById('full-abstract');

        show_abstract.addEventListener('click', function (e) {
            e.preventDefault();
            partial_abstract.classList.add('hidden');
            full_abstract.classList.remove('hidden');
        });

        hide_abstract.addEventListener('click', function (e) {
            e.preventDefault();
            full_abstract.classList.add('hidden');
            partial_abstract.classList.remove('hidden');
        });
    }
})();