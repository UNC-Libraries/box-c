define('IsSorted', function() {
    function IsSorted() {
        this.sorted = JSON.parse(localStorage.getItem('unc-cdr-admin-sort'));
    }

    IsSorted.prototype.stale = function() {
        var QUARTER_HOUR =  15 * 60 * 1000;
        var sorted = this.sorted;

        if (sorted === null) {
            return true;
        } else if ((Date.now() - sorted.sortTime) < QUARTER_HOUR) {
            return false;
        } else {
            localStorage.removeItem('unc-cdr-admin-sort');
            return true;
        }
    };

    IsSorted.prototype.setSorted = function(sortSettings) {
        localStorage.setItem('unc-cdr-admin-sort', JSON.stringify(sortSettings));
    };

    IsSorted.prototype.getSorted = function() {
        return this.sorted;
    };

    return new IsSorted;
});