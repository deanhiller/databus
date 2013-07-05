
$(function () {
/**
 * |DATABUS_CHART|
 * ***********************************************************************
 * REQUIRED FOR DATABUS URI  -- START --
 * ***********************************************************************
 * 
 * The following information MUST be present in the chart code or the
 * Databus chart loader might encounter errors.
 */
var _name = 'BLANK';
var _title = 'Dashboard Chart';
 
var _protocol = 'http';
var _database = '';
/**
 * |DATABUS_CHART|
 * ***********************************************************************
 * REQUIRED FOR DATABUS URI  -- END --
 * ***********************************************************************
 */
 	$(function () {
		var blankOptions = {
			title: {
		        text: _title
		    },
		    chart: {
		        renderTo: 'container',
			   borderColor: '#dddddd',
			   borderWidth: 1
		    },
		
		    xAxis: {
			    categories: ['','Add', 'a', 'chart', 'to', 'your', 'dashboard', ''],
			    labels:{
			                step: 1
			            }
			},
		    yAxis: {
		            endOnTick: false,
		            min: 0,
		            max: 26,
		            tickInterval: 2,
		            title: { text: 'Alphabet' },
		            labels: {
		                        formatter: function() {
		                            return this.value;
		                        }
		                    }  
		            },
		
		    series: [{
	                showInLegend: false,
	                data: [0,1,1,3,20,25,4,0]
	                }]
		};
		
		var blankChart = new Highcharts.Chart(blankOptions);
	});
});