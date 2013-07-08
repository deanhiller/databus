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
var _name = 'HighCharts - Time Series Date Zoomable';
var _title = 'HighCharts - Time Series Date Zoomable';
 
var _protocol = 'http';
var _database = '/api/firstvaluesV1/56/rawdataV1/fakeTimeSeries?reverse=true';
/**
 * |DATABUS_CHART|
 * ***********************************************************************
 * REQUIRED FOR DATABUS URI  -- END --
 * ***********************************************************************
 */
 	var _url = _protocol + '://' + window.location.host + _database;
 	
 	_title = _title + " [Past Year]";
 	_name = _name + "[Past Year by Week]";
 	
 	var startDate = new Date();
 	startDate.setDate(startDate.getDate() - 365);
 	
	$.getJSON(_url, function (startData) {
		var streamData = [];
						   
		$.each(startData.data, function (key, val) {
		    var value = val.value;
		
		    streamData.push(value);
		});
		
		streamData.reverse();
		
		// Create the chart
		var options = {	
            chart: {
                zoomType: 'x',
                spacingRight: 20,
			 renderTo: 'container'
            },
            title: {
                text: _title
            },
            subtitle: {
                text: document.ontouchstart === undefined ?
                    'Click and drag in the plot area to zoom in' :
                    'Drag your finger over the plot to zoom in'
            },
            xAxis: {
                type: 'datetime',
                maxZoom: 14 * 24 * 3600000, // fourteen days
                title: {
                    text: null
                }
            },
            yAxis: {
                title: {
                    text: 'Value'
                }
            },
            tooltip: {
                shared: true
            },
            legend: {
                enabled: false
            },
            plotOptions: {
                area: {
                    fillColor: {
                        linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1},
                        stops: [
                            [0, Highcharts.getOptions().colors[0]],
                            [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
                        ]
                    },
                    lineWidth: 1,
                    marker: {
                        enabled: false
                    },
                    shadow: false,
                    states: {
                        hover: {
                            lineWidth: 1
                        }
                    },
                    threshold: null
                }
            },    
            series: [{
                type: 'area',
                name: _name,
                //pointInterval: 24 * 3600 * 1000 // one day
                pointInterval: 24 * 3600 * 1000 * 7,
                pointStart: Date.UTC(startDate.getFullYear(), startDate.getMonth() - 1, startDate.getDate()),
                data: streamData
            }]
        };
        
        var chart = new Highcharts.Chart(options);
    });
});
                