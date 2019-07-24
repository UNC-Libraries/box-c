(function() {
    var browse_type = document.getElementById('browse-display-type');
    browse_type.addEventListener('click', setBrowseType, false);
    browse_type.addEventListener('touchstart', setBrowseType, false);

    function setBrowseType(e) {
        e.preventDefault();
        var selected_browse = e.target.id;
        localStorage.setItem('dcr-browse-display', selected_browse);
    }
})();