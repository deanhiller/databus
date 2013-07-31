--metasource-version--
1
--metasource-meta--
{ 
	"name": "Basic Line Chart",
	"description":"This is a basic line chart just displaying a single line with x and y axis",
	"pages" : [
			{
				"title" : "Fill in Chart Info",
				"variables": [
			        {
			        	"nameInJavascript": "title",
			        	"label": "Title",
			        	"help" : "This is the title",
			        	"type" : "String"
					},
					{
			        	"nameInJavascript": "url",
			        	"label": "Data Url",
			        	"help" : "The url of your input data",
			        	"type" : "String"
					}
					]
			}
	]
}
--metasource-header--
<script src="/public/includes/jquery.hotkeys.js"></script>
<script src="/public/includes/highstock.js"></script>
<script src="/public/includes/exporting.js"></script>
--metasource-largechart--
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
var _title = ${variables.title}
var _url = ${variables.url}

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
		
		streamData.reverse();
		
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
