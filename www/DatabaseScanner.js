var exec = require('cordova/exec');

var DatabaseScanner = {
    listDatabases: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'DatabaseScanner', 'listDatabases', []);
    }
};

module.exports = DatabaseScanner;