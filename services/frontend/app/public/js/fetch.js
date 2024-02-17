(function(exports) {
    function fetch(url, async, callback) {
        xhttp = new XMLHttpRequest();
        xhttp.onreadystatechange = function() {
            if (this.readyState == 4 && this.status == 200) {
                if (callback) {
                    callback(this.responseText);
                }
            }
        };
        xhttp.open("GET", url, async);
        if (async == true) {
            xhttp.timeout = 30000;
        }
        xhttp.send();
    }

    exports.fetch = fetch;
})(window);
