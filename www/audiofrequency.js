cordova.define("cordova/plugin/audiofrequency", function(require, exports, module) {
    var cordova = require('cordova'),
		exec = require('cordova/exec');

    var AudioFrequency = function() {
		this.channels = {
			audiofrequency: cordova.addWindowEventHandler("audiofrequency")
		};
		for (var key in this.channels) {
			this.channels[key].onHasSubscribersChange = AudioFrequency.onHasSubscribersChange;
		}
	};

	function handlers () {
		return audiofrequency.channels.audiofrequency.numHandlers;
	};

	AudioFrequency.onHasSubscribersChange = function () {
		alert(1);
		if (this.numHandlers === 1 && handlers() === 1) {
			exec(audiofrequency._frequency, audiofrequency._error, "AudioFrequency", "start", []);
		} else if (handlers() === 0) {
			exec(null, null, "AudioFrequency", "stop", []);
		}
	};

	AudioFrequency.prototype._frequency = function (frequencyData) {
		alert(2);
		cordova.fireWindowEvent("audiofrequency", frequencyData);
	};

	AudioFrequency.prototype._error = function (e) {
		console.log("Error initializing AudioFrequency: " + e);
	};

	var audiofrequency = new AudioFrequency();

	module.exports = audiofrequency;
});