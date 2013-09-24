--metasource-version--
1
--metasource-meta--
{
	"name": "Single Stream Updating Line Chart",
	"description":"This is a basic line chart displaying a single line with x and y axis that updates every 30 seconds.  It has a blue theme.",
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
			        	"type" : "String"
					},
					{
			        	"nameInJavascript": "intervalUrl",
			        	"label": "Data Interval Url",
			        	"help" : "The url for data with a count added to the update.",
			        	"type" : "String"
					},
					{
			        	"nameInJavascript": "updateFrequency",
			        	"label": "Data Interval",
			        	"help" : "The time frequency for updating the chart data.",
			        	"type" : "String"
					},
					{
			        	"nameInJavascript": "timeColumn",
			        	"label": "Time Column Name",
			        	"help" : "The name of the column containing the time in milliseconds",
			        	"type" : "String",
			        	"defaultValue" : "time"
					},
					{
			        	"nameInJavascript": "valueColumn",
			        	"label": "Value Column Name",
			        	"help" : "The name of the column containing the value in milliseconds",
			        	"type" : "String",
			        	"defaultValue" : "value"
					}
					]
			},
			{
				"title" : "Fill in Some Labels",
				"variables": [
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
$(function () {
	/**
	 * Controller Chart Data
	 */
	var _title = '${title}';
	var _url = '${url}';
	var _intervalUrl = '${intervalUrl}';
	var _updateFrequency = '${updateFrequency}' * 1000;
	var _timeColumn = '${timeColumn}';
	var _valueColumn = '${valueColumn}';
	var _units = '${units}';
	var _yaxisLabel = '${yaxisLabel}';

	/**
	 * Chart Creation Variables
	 */
	var chart = null;
	var maximized = false;
	var maximizedStream = null;
	var height = getHeight();
	var width = $("#theChart").width(), chart;
	var lastTime = 0;
	
	/**
	 * Adjust the Window sizes
	 */
	$(window).resize(function () {
		var height = getHeight();
		console.log('fit new div container size, width='+$("#theChart").width()+' height='+height+' windowH='+$(window).height());
		var width = $("#theChart").width();
		if(chart != null)
			chart.setSize(width, height, false);
	});
	
	/**
	 * Create chart render <div>
	 */
	$('#theChart').append($('<div class="chart" />').attr('id', 'chart_container'));
	
	/**
	 * Chart functions
	 */
	$(document).bind('keyup', 'esc', function () {
		if (maximized) {
			maximized = false;
			$('.overlay').toggleClass('overlay');
		}
	});
	
	/**
	 * Utility Functions
	 */
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
	
	/**
	 * Handle the JSON security callback requirement
	 */
	if(_url.indexOf('?') > 0) {
		_url = _url + '&callback=?';
	} else
		_url = _url + '?callback=?';
		
	if(_intervalUrl.indexOf('?') > 0) {
		_intervalUrl = _intervalUrl + '&callback=?';
	} else
		_intervalUrl = _intervalUrl + '?callback=?';
			
	/**
	 * Blue theme for this chart.  Must be declared prior to instatiating the
	 * actual chart itself.
	 */
	var theme = {
	   colors: ["#DDDF0D", "#55BF3B", "#DF5353", "#7798BF", "#aaeeee", "#ff0066", "#eeaaee",
	      "#55BF3B", "#DF5353", "#7798BF", "#aaeeee"],
	   chart: {
	      backgroundColor: {
	         linearGradient: { x1: 0, y1: 0, x2: 1, y2: 1 },
	         stops: [
	            [0, 'rgb(48, 48, 96)'],
	            [1, 'rgb(0, 0, 0)']
	         ]
	      },
	      borderColor: '#000000',
	      borderWidth: 2,
	      className: 'dark-container',
	      plotBackgroundColor: 'rgba(255, 255, 255, .1)',
	      plotBorderColor: '#CCCCCC',
	      plotBorderWidth: 1
	   },
	   title: {
	      style: {
	         color: '#C0C0C0',
	         font: 'bold 16px "Trebuchet MS", Verdana, sans-serif'
	      }
	   },
	   subtitle: {
	      style: {
	         color: '#666666',
	         font: 'bold 12px "Trebuchet MS", Verdana, sans-serif'
	      }
	   },
	   xAxis: {
	      gridLineColor: '#333333',
	      gridLineWidth: 1,
	      labels: {
	         style: {
	            color: '#A0A0A0'
	         }
	      },
	      lineColor: '#A0A0A0',
	      tickColor: '#A0A0A0',
	      title: {
	         style: {
	            color: '#CCC',
	            fontWeight: 'bold',
	            fontSize: '12px',
	            fontFamily: 'Trebuchet MS, Verdana, sans-serif'
	
	         }
	      }
	   },
	   yAxis: {
	      gridLineColor: '#333333',
	      labels: {
	         style: {
	            color: '#A0A0A0'
	         }
	      },
	      lineColor: '#A0A0A0',
	      minorTickInterval: null,
	      tickColor: '#A0A0A0',
	      tickWidth: 1,
	      title: {
	         style: {
	            color: '#CCC',
	            fontWeight: 'bold',
	            fontSize: '12px',
	            fontFamily: 'Trebuchet MS, Verdana, sans-serif'
	         }
	      }
	   },
	   tooltip: {
	      backgroundColor: 'rgba(0, 0, 0, 0.75)',
	      style: {
	         color: '#F0F0F0'
	      }
	   },
	   toolbar: {
	      itemStyle: {
	         color: 'silver'
	      }
	   },
	   plotOptions: {
	      line: {
	         dataLabels: {
	            color: '#CCC'
	         },
	         marker: {
	            lineColor: '#333'
	         }
	      },
	      spline: {
	         marker: {
	            lineColor: '#333'
	         }
	      },
	      scatter: {
	         marker: {
	            lineColor: '#333'
	         }
	      },
	      candlestick: {
	         lineColor: 'white'
	      }
	   },
	   legend: {
	      itemStyle: {
	         font: '9pt Trebuchet MS, Verdana, sans-serif',
	         color: '#A0A0A0'
	      },
	      itemHoverStyle: {
	         color: '#FFF'
	      },
	      itemHiddenStyle: {
	         color: '#444'
	      }
	   },
	   credits: {
	      style: {
	         color: '#666'
	      }
	   },
	   labels: {
	      style: {
	         color: '#CCC'
	      }
	   },
	
	   navigation: {
	      buttonOptions: {
	         symbolStroke: '#DDDDDD',
	         hoverSymbolStroke: '#FFFFFF',
	         theme: {
	            fill: {
	               linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
	               stops: [
	                  [0.4, '#606060'],
	                  [0.6, '#333333']
	               ]
	            },
	            stroke: '#000000'
	         }
	      }
	   },
	
	   // scroll charts
	   rangeSelector: {
	      buttonTheme: {
	         fill: {
	            linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
	            stops: [
	               [0.4, '#888'],
	               [0.6, '#555']
	            ]
	         },
	         stroke: '#000000',
	         style: {
	            color: '#CCC',
	            fontWeight: 'bold'
	         },
	         states: {
	            hover: {
	               fill: {
	                  linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
	                  stops: [
	                     [0.4, '#BBB'],
	                     [0.6, '#888']
	                  ]
	               },
	               stroke: '#000000',
	               style: {
	                  color: 'white'
	               }
	            },
	            select: {
	               fill: {
	                  linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
	                  stops: [
	                     [0.1, '#000'],
	                     [0.3, '#333']
	                  ]
	               },
	               stroke: '#000000',
	               style: {
	                  color: 'yellow'
	               }
	            }
	         }
	      },
	      inputStyle: {
	         backgroundColor: '#333',
	         color: 'silver'
	      },
	      labelStyle: {
	         color: 'silver'
	      }
	   },
	
	   navigator: {
	      handles: {
	         backgroundColor: '#666',
	         borderColor: '#AAA'
	      },
	      outlineColor: '#CCC',
	      maskFill: 'rgba(16, 16, 16, 0.5)',
	      series: {
	         color: '#7798BF',
	         lineColor: '#A6C7ED'
	      }
	   },
	
	   scrollbar: {
	      barBackgroundColor: {
	            linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
	            stops: [
	               [0.4, '#888'],
	               [0.6, '#555']
	            ]
	         },
	      barBorderColor: '#CCC',
	      buttonArrowColor: '#CCC',
	      buttonBackgroundColor: {
	            linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
	            stops: [
	               [0.4, '#888'],
	               [0.6, '#555']
	            ]
	         },
	      buttonBorderColor: '#CCC',
	      rifleColor: '#FFF',
	      trackBackgroundColor: {
	         linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
	         stops: [
	            [0, '#000'],
	            [1, '#333']
	         ]
	      },
	      trackBorderColor: '#666'
	   },
	
	   // special colors for some of the
	   legendBackgroundColor: 'rgba(0, 0, 0, 0.5)',
	   legendBackgroundColorSolid: 'rgb(35, 35, 70)',
	   dataLabelsColor: '#444',
	   textColor: '#C0C0C0',
	   maskColor: 'rgba(255,255,255,0.3)'
	};
	
	/**
	 * The actual chart creation
	 */
	$.getJSON(_url, function (startData) {
		var startingData = [];
						   
		$.each(startData.data, function (key, val) {
		    var time = val[_timeColumn];
		    var value = val[_valueColumn];
		    
		    var formattedTime = new Date(time);
		    		
		    startingData.push([formattedTime.getTime(), (value == "NaN") ? null : value]);
		    lastTime = time;
		});
		
		startingData.reverse();
		
		var msg = "No data available in the specified time period.";
		if(startingData.length != 0) { 
			msg = "Last value was " + startingData[startingData.length-1][1] + 
			 " at " + new Date(startingData[startingData.length-1][0]).toUTCString();
		}	
	
		Highcharts.setOptions({
			global : {
				useUTC : false
			}
		});
		
		var options = {
			chart : {
				renderTo: 'chart_container',
				events : {
					load : function() {
						var series = this.series[0];
												
						setInterval(function() {								
							$.getJSON(_intervalUrl, function (chartData) {
								
								var lastTime = startingData[startingData.length-1][0];
								var count = 0;
								$.each(chartData.data, function (key, val) {									
								    var time = val[_timeColumn];
								    var value = val[_valueColumn];
								    
								    var formattedTime = new Date(time);
								    
								    if(time != lastTime) {							    
								    	series.addPoint([formattedTime.getTime(), value], true, true);
								    	count++;
								    	lastTime = time;
								    }
								});
								
								var theChart = $('#chart_container').highcharts();
								
								if(count != 0) { 
									theChart.setTitle(_title, "Last value was " + startingData[startingData.length-1][1] + 
										 " at " + new Date(startingData[startingData.length-1][0]).toUTCString());
								} else {
									theChart.setTitle(_title, "Value has not changed.");
								}
							});
						}, _updateFrequency);
					}
				}
			},
			
			rangeSelector: {
				buttons: [{
					count: 1,
					type: 'minute',
					text: '1m'
				}, {
					count: 5,
					type: 'minute',
					text: '5m'
				}, {
					count: 10,
					type: 'minute',
					text: '10m'
				}, {
					count: 30,
					type: 'minute',
					text: '30m'
				}, {
					count: 60,
					type: 'minute',
					text: '1h'
				}, {
					count: 120,
					type: 'minute',
					text: '2h'
				}, {
					count: 720,
					type: 'minute',
					text: '12h'
				}, {
					count: 1440,
					type: 'minute',
					text: '24h'
				}, {
					count: 2880,
					type: 'minute',
					text: '48h'
				}, {
					count: 10080,
					type: 'minute',
					text: '1w'
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
			
			subtitle: {
				text: msg
			},
			
			xAxis: { 
				ordinal: false 
			},
			
			yAxis: [{
					gridLineWidth: 0,
				     labels: {
				     	formatter: function() {
				     		return this.value +' '+_units;
				     	},
				     	align: 'right'
				     },
				     title: { text: _yaxisLabel }
			}],
				
			tooltip: { 
				shared: true 
			},
			
			legend: { 
				align: 'center', enabled: true, floating: true,
				verticalAlign: 'top', y: 40, symbolWidth: 30 
			},
			
			exporting: {
				enabled: false
			},
			
			series : [{
				name : 'Time Series',
				type : 'area',
				threshold : null,
				fillColor : {
					linearGradient : {
						x1: 0, 
						y1: 0, 
						x2: 0, 
						y2: 1
					},
					stops : [[0, theme.colors[0]], [1, 'rgba(0,0,0,0)']]
				},
				data : startingData,
				tooltip: {
		        		valueSuffix: ' '+_units
		        	}
			}]
		};
		
		window.chart = new Highcharts.StockChart(Highcharts.merge(options, theme));
	});
});