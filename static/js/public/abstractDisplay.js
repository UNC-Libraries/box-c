(function() {
    var clicked = document.getElementById('show-abstract');
    var partial_abstract = document.getElementById('truncated-abstract');
    var full_abstract = document.getElementById('full-abstract');

    if (partial_abstract !== null) {
        clicked.addEventListener('click', function (e) {
            e.preventDefault();
            partial_abstract.classList.toggle('hidden');
            full_abstract.classList.toggle('hidden');
            this.innerText = partial_abstract.classList.contains('hidden') ? 'Read less' : 'Read more';
        });
    }
})();