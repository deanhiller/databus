
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
var _name = 'HighStock - Time Series Single Stream';
var _title = 'HighStock - Time Series Single Stream';
 
var _protocol = 'http';
var _database = '/api/firstvaluesV1/50/rawdataV1/fakeTimeSeries?reverse=true';
/**
 * |DATABUS_CHART|
 * ***********************************************************************
 * REQUIRED FOR DATABUS URI  -- END --
 * ***********************************************************************
 */
 	var _url = _protocol + '://' + window.location.host + _database;
 	 	
	$.getJSON(_url, function (startData) {
		var streamData = [];
						   
		$.each(startData.data, function (key, val) {
		    var time = val.time;
		    var value = val.value;
		    
		    var formattedTime = new Date(time);
		
		    streamData.push([formattedTime.getTime(), value]);
		});
		
		// Create the chart
		var options = {			
			chart : {
				renderTo: 'container'
			},

			rangeSelector : {
				selected : 1
			},

			title : {
				text : _title
			},
			
			series : [{
				name : 'Time Series',
				data : streamData,
				tooltip: {
					valueDecimals: 2
				}
			}]
		};
			
		var chart = new Highcharts.StockChart(options);
	});
});