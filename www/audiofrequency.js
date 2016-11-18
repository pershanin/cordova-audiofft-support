cordova.define("cordova/plugin/audiofrequency", function(require, exports, module) {
    var exec = require('cordova/exec');

    var AudioFrequency = function() {};
	
	AudioFrequency.prototype.start = function() {
        exec(audiofrequency._frequency, audiofrequency._error, 'AudioFrequency', 'start', []);
    }
	
	AudioFrequency.prototype.stop = function() {
        exec(null, null, "AudioFrequency", "stop", []);
    }

	AudioFrequency.prototype._frequency = function (frequencyData) {
		cordova.fireWindowEvent("audiofrequency", frequencyData);
	};

	AudioFrequency.prototype._error = function (e) {
		console.log("Error initializing AudioFrequency: " + e);
	};

	var audiofrequency = new AudioFrequency();

	module.exports = audiofrequency;
});