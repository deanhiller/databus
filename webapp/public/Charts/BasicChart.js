--metasource-version--
1
--metasource-meta--
{
	"name": "Basic Line Chart",
	"description":"This is a basic line chart just displaying a single line with x and y axis",
	"needsHighChartsLicense" : true,
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
			        	"type" : "String",
			        	"isRequired" : true
					},
					{
						"nameInJavascript" : "timepanel",
			        	"type" : "timepanel",
			        	"isRequired" : true
					}
					]
			},
			{
				"title" : "Fill in Some Labels",
				"variables": [
					{
			        	"nameInJavascript": "timeColumn",
			        	"label": "Time Column Name",
			        	"help" : "The name of the column containing the time in milliseconds",
			        	"type" : "column",
			        	"defaultValue" : "time",
			        	"isRequired" : true
					},
					{
			        	"nameInJavascript": "valueColumn",
			        	"label": "Value Column Name",
			        	"help" : "The name of the column containing the value in milliseconds",
			        	"type" : "column",
			        	"defaultValue" : "value",
			        	"isRequired" : true
					},
				    {
			        	"nameInJavascript": "yaxisLabel",
			        	"label": "Y-axis label",
			        	"help" : "This is the label that goes on the left side of the chart",
			        	"type" : "String"
					},
					{
			        	"nameInJavascript": "units",
			        	"label": "Units",
			        	"help" : "The units showing up on y-axis and on the legend",
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
*{
 Notice this large chart has extra features like ZOOM and buttons and such why the small chart version is automatically used in
 stuff like 4 quadrant dashboards such that you can remove zoom and other extraneous features from your chart
 Also, this is a type of comment that is stripped out of the javascript going to the web page completely.
}*
$(function () {

	/**
	 * Here for the "nameInJavascript" values you have above, we assign them to variables in the javascript so we dynamically create 
	 * the wizard asking questions to the user for you and you just focus on chart creation
	 */
	var _title = '${title}';
	var _url = '${url}';
	var _timeColumn = '${timeColumn}';
	var _valueColumn = '${valueColumn}';
	var _units = '${units}';
	var _yaxisLabel = '${yaxisLabel}';

	var maximized = false,
	maximizedStream,
	height = getHeight(),
	width = $("#theChart").width(),
	chart;

	$(window).resize(function () {
		var height = getHeight();
		console.log('fit new div container size, width='+$("#theChart").width()+' height='+height+' windowH='+$(window).height());
		var width = $("#theChart").width();
		if(chart != null)
			chart.setSize(width, height, false);
	});

	Highcharts.extend(Highcharts.getOptions().lang, {
		zoomTitle: 'Zoom the chart'
	});

	Highcharts.setOptions({
		credits: { enabled: false },
		global: { useUTC: false },
		loading: { 
			labelStyle: { color: '#FFF' },
			style: { backgroundColor: '#444' }
		},
		navigator: { margin: 50 },
		plotOptions: {
			series: {
				dataGrouping: { approximation: "high", groupPixelWidth: 1 },
				marker: { /* enabled: true */ }
			}
		},
		rangeSelector: {
			inputEnabled: false,
			buttons: [{ type: 'hour', count: 1,  text: '1h' },
			          { type: 'hour', count: 6,  text: '6h' },
			          { type: 'hour', count: 12, text: '12h' },
			          { type: 'hour', count: 24, text: '24h' },
			          { type: 'hour', count: 48, text: '48h' }, 
			          { type: 'all', text: 'All'}],
			selected: 5
		},
		tooltip: {
			backgroundColor: {
				linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
				stops: [ [0, '#FFF'], [1, '#EEE'] ]
			},
			borderColor: 'gray',
			borderWidth: 1,
			yDecimals: 2
		},
		xAxis: {
			dateTimeLabelFormats: { second: '%l:%M:%S%P', minute: '%l:%M%P',
				hour: '%l%P', day: '%e %b', week: '%e %b' },
			type: 'datetime',
			title: { text: '' }
		},
		yAxis: { endOnTick: false /*, min: 0*/ }
	});

	function crisp(arr) {
		var i = arr.length;
		while (i--) {
			if (typeof arr[i] === 'number') {
				arr[i] = Math.round(arr[i]) - 0.5;
			}
		}
		return arr;
	}

	function getHeight() {
		var override = 0;
		if(override > 0)
			return override;
		var windowHeight = $(window).height();
		var height = windowHeight;
		if(height < 300)
			height = 300;
		return height;
	}
	
	function collapseData(data, columnName) {
		var collapsedData = [];
		$.each(data.data, function (index, value) {
			collapsedData.push([value[_timeColumn], (value == "NaN") ? null : value[columnName] ]);
		});
	
		if(collapsedData.length >= 2) {
			if(collapsedData[0][0] > collapsedData[1][0])
				collapsedData.reverse();
		}

		return collapsedData;
	}

	function expandData(data) {
		var expandedData = {};
		expandedData.data = [];
		expandedData.error = [];
		$.each(data, function (index, value) {
			expandedData.data.push({
				"time": value[0],
				"value": value[1] * 1000
			});
		});
		return expandedData;
	}
	
	window.collapseData = collapseData;
	window.expandData = expandData;

	function addChart() {
		console.log('url='+_url);

		$('#theChart').append($('<div class="chart" />').attr('id', 'mychart'));
		$.getJSON(_url, function (data) {

			console.log('json callback being called with data');
			var msg = "No data available in the specified time period.";
			if(data.data.length != 0) {
				msg = "Last value was "+data.data[data.data.length-1][_valueColumn]+" at "+new Date(data.data[data.data.length-1][_timeColumn]).toUTCString()
			}

			console.log('window2222 height:'+$(window).height());
			chart = new Highcharts.StockChart({
				chart: { renderTo: mychart, height: getHeight() },
				title: { text: _title },
				subtitle: { text: msg },
				xAxis: { ordinal: false },
				yAxis: [
				        {
				        	gridLineWidth: 0,
				        	labels: {
				        		formatter: function() {
				        			return this.value +' '+_units;
				        		},
				        		align: 'right'
				        	},
				        	title: { text: _yaxisLabel }
				        }
				        ],
				tooltip: { shared: true },
				legend: { align: 'center', enabled: true, floating: true,
					verticalAlign: 'top', y: 40, symbolWidth: 30 },
				*{ 
					legend: { layout: 'vertical', align: 'left', x: 120, verticalAlign: 'top',
					y: 80, floating: true, backgroundColor: '#FFFFFF'},
				}*
				series: [
				         {
				        	 name: 'SeriesName', 
				        	 type: 'spline',
				        	 yAxis: 0,
				        	 data: collapseData(data, _valueColumn),
				        	 tooltip: {
				        		 valueSuffix: ' '+_units
				        	 }
				         }
				         ]
			}); //new Chart
			
			
			
		}); //getJson
	} //addChart

	$(document).bind('keyup', 'esc', function () {
		if (maximized) {
			maximized = false;
			$('.overlay').toggleClass('overlay');
		}
	});

	addChart();
	console.log('addChart called, waiting on json data to come back');
});
--metasource-smallchart--
  *{
	 Notice this section is optional and in small spaces, the large chart version will be used.  This can be very bad if you have
	 zoom features and such.  This version of our chart gets rid of the zoom and extra stuff so it can be shown in small spaces like
	 when the dashboard has 4 charts on it.
	}*
	$(function () {

		/**
		 * Here for the "nameInJavascript" values you have above, we assign them to variables in the javascript so we dynamically create 
		 * the wizard asking questions to the user for you and you just focus on chart creation
		 */
		var _title = '${title}'
		var _url = '${url}';
		var _timeColumn = '${timeColumn}'
		var _valueColumn = '${valueColumn}'
		var _units = '${units}';
		var _yaxisLabel = '${yaxisLabel}';
		
		var maximized = false,
		    maximizedStream,
		    height = getHeight(),
		    width = $("#theChart").width(),
		    chart;

		  $(window).resize(function () {
			var height = getHeight();
		    console.log('fit new div container size, width='+$("#theChart").width()+' height='+height+' windowH='+$(window).height());
		    var width = $("#theChart").width();
		    if(chart != null)
		        chart.setSize(width, height, false);
		  });

			Highcharts.setOptions({
				credits: { enabled: false },
				global: { useUTC: false },
				loading: { 
					labelStyle: { color: '#FFF' },
					style: { backgroundColor: '#444' }
				},
				navigator: { margin: 50 },
				plotOptions: {
					series: {
						dataGrouping: { approximation: "high", groupPixelWidth: 1 },
						marker: { enabled: false }
					}
				},
				tooltip: {
					backgroundColor: {
						linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
						stops: [ [0, '#FFF'], [1, '#EEE'] ]
					},
					borderColor: 'gray',
					borderWidth: 1,
					yDecimals: 2
				},
				xAxis: {
					dateTimeLabelFormats: { second: '%l:%M:%S%P', minute: '%l:%M%P',
						hour: '%l%P', day: '%e %b', week: '%e %b' },
					type: 'datetime',
					title: { text: '' }
				},
				yAxis: { endOnTick: false /*, min: 0*/ }
			});

		  function crisp(arr) {
		    var i = arr.length;
		    while (i--) {
		      if (typeof arr[i] === 'number') {
		        arr[i] = Math.round(arr[i]) - 0.5;
		      }
		    }
		    return arr;
		  }

		  function getHeight() {
			var override = 0;
			if(override > 0)
				return override;
			var windowHeight = $(window).height();
			var height = windowHeight;
			if(height < 300)
				height = 300;
			return height;
		  }
		  
		  function collapseData(data, columnName) {
		    var collapsedData = [];
		    $.each(data.data, function (index, value) {
		      collapsedData.push([value[_timeColumn], (value == "NaN") ? null : value[columnName] ]);
		    });
		    
		    if(collapsedData.length >= 2) {
		    	if(collapsedData[0][0] > collapsedData[1][0])
		    		collapsedData.reverse();
		    }
		    
		    return collapsedData;
		  }

		  function expandData(data) {
		    var expandedData = {};
		    expandedData.data = [];
		    expandedData.error = [];
		    $.each(data, function (index, value) {
		      expandedData.data.push({
		        "time": value[0],
		          "value": value[1] * 1000
		      });
		    });
		    return expandedData;
		  }
		  window.collapseData = collapseData;
		  window.expandData = expandData;

		  function addChart() {
		    console.log('url='+_url);
		    
		    $('#theChart').append($('<div class="chart" />').attr('id', 'mychart'));
		    $.getJSON(_url, function (data) {
		        
		    	console.log('json callback being called with data');
		        var msg = "No data available in the specified time period."
		        if(data.data.length != 0) {
		            msg = "Last value was "+data.data[data.data.length-1][_valueColumn]+" at "+new Date(data.data[data.data.length-1][_timeColumn]).toUTCString()
		        }

		      console.log('window2222 height:'+$(window).height());
		      chart = new Highcharts.Chart({
		          chart: {
		              renderTo: mychart,
		              height: getHeight()
		          },
		          title: {
		              text: _title
		          },
		          subtitle: {
		                text: msg
		          },
		          xAxis: {
		              ordinal: false
		          },
		          yAxis: [
		                  {
		                	gridLineWidth: 0,
		                	labels: {
		                		formatter: function() {
		                			return this.value +' '+_units;
		                		},
		                		align: 'right'
		                	},
		                	title: {
		                		text: _yaxisLabel,
		                	}
		                  }
		          ],
		          tooltip: {
		              shared: true
		          },
		          legend: {
		              align: 'center',
		              enabled: true,
		              floating: true,
		              verticalAlign: 'top',
		              y: 40,
		              symbolWidth: 30
		          },
		          *{ legend: {
		              layout: 'vertical',
		              align: 'left',
		              x: 120,
		              verticalAlign: 'top',
		              y: 80,
		              floating: true,
		              backgroundColor: '#FFFFFF'
		          }, }*
		          series: [
		                   {   
		                	   name: 'SeriesName', 
		                	   type: 'spline',
		                	   yAxis: 0,
		                	   data: collapseData(data, _valueColumn),
		                	   tooltip: {
		                		   valueSuffix: ' '+_units
		                	   }
		                   }
		          ]
		        });

		      
		    });
		  }

		  $(document).bind('keyup', 'esc', function () {
		    if (maximized) {
		      maximized = false;
		      $('.overlay').toggleClass('overlay');
		    }
		  });

		  addChart();
		  console.log('addChart called, waiting on json data to come back');
		});