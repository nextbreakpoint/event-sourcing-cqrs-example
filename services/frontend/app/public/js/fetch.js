(function(exports) {
    function fetch(url, async, onCompleted, onFailure) {
        xhttp = new XMLHttpRequest();
        xhttp.onreadystatechange = function() {
            if (this.readyState == 4 && this.status == 200) {
                if (onCompleted) {
                    onCompleted(this.responseText);
                }
            }
        };
        xhttp.onabort = function(e) {
            if (onFailure) {
                onFailure(e);
            }
        };
        xhttp.onerror = function(e) {
            if (onFailure) {
                onFailure(e);
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
