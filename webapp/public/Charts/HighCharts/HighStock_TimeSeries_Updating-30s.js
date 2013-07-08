$(function() {
/**
 * |DATABUS_CHART|
 * ***********************************************************************
 * REQUIRED FOR DATABUS URI  -- START --
 * ***********************************************************************
 * 
 * The following information MUST be present in the chart code or the
 * Databus chart loader might encounter errors.
 */
var _name = 'HighStock - Time Series (Updating)';
var _title = 'HighStock - Time Series (Updating)';
 
var _protocol = 'http';
var _database = '/api/firstvaluesV1/__COUNT__/rawdataV1/fakeTimeSeries?reverse=true';
/**
 * |DATABUS_CHART|
 * ***********************************************************************
 * REQUIRED FOR DATABUS URI  -- END --
 * ***********************************************************************
 */
 	
 	var databaseCall = _database.replace('__COUNT__', '50');
 	
 	var _url = _protocol + '://' + window.location.host + databaseCall;
 	var frequency = 30000;
 	
 	_title = _title + " [Frequency = " + frequency/1000 + "s]"
 	
 	var lastTime = 0;
 	
 	var theDiv = "renderTo: 'container'";
 	var containerDiv = '#' + theDiv.replace(/renderTo: '(.*)'/, "$1");
 	
	$.getJSON(_url, function (startData) {
		var startingData = [];
						   
		$.each(startData.data, function (key, val) {
		    var time = val.time;
		    var value = val.value;
		    
		    var formattedTime = new Date(time);
		
		    startingData.push([formattedTime.getTime(), value]);
		    lastTime = time;
		});
		
		startingData.reverse();
	
	
		Highcharts.setOptions({
			global : {
				useUTC : false
			}
		});
		
		// Create the chart
		var options = {
			chart : {
				renderTo: 'container',
				events : {
					load : function() {
						var series = this.series[0];
						
						var databaseIntervalCall = _database.replace('__COUNT__', '1');
						var intervalURL = _protocol + '://' + window.location.host + databaseIntervalCall;
						
						
						setInterval(function() {
							$.getJSON(intervalURL, function (chartData) {
												   
								$.each(chartData.data, function (key, val) {
								    var time = val.time;
								    var value = val.value;
								    
								    var formattedTime = new Date(time);
								    
								    if(time != lastTime) {
								    	series.addPoint([formattedTime.getTime(), value], true, true);
								    	lastTime = time;
								    }
								});
							});
						}, frequency);
					}
				}
			},
			
			rangeSelector: {
				buttons: [{
					count: 1,
					type: 'minute',
					text: '1M'
				}, {
					count: 5,
					type: 'minute',
					text: '5M'
				}, {
					count: 10,
					type: 'minute',
					text: '10M'
				}, {
					count: 30,
					type: 'minute',
					text: '30M'
				}, {
					type: 'all',
					text: 'All'
				}],
				inputEnabled: false,
				selected: 2
			},
			
			title : {
				text : _title
			},
			
			exporting: {
				enabled: false
			},
			
			series : [{
				name : 'Time Series',
				data : startingData
			}]
		};
		
		var chart = new Highcharts.StockChart(options);
	});

});