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
		
		var theme = {
			   colors: ["#DDDF0D", "#55BF3B", "#DF5353", "#7798BF", "#aaeeee", "#ff0066", "#eeaaee",
			      "#55BF3B", "#DF5353", "#7798BF", "#aaeeee"],
			   chart: {
			      backgroundColor: {
			         linearGradient: [0, 0, 250, 500],
			         stops: [
			            [0, 'rgb(48, 96, 48)'],
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
				data : startingData
			}]
		};
		
		var chart = new Highcharts.StockChart(Highcharts.merge(options, theme));
	});

});