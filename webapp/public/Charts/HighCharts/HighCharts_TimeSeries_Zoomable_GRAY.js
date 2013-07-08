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
		
		var theme = {
			   colors: ["#DDDF0D", "#7798BF", "#55BF3B", "#DF5353", "#aaeeee", "#ff0066", "#eeaaee",
			      "#55BF3B", "#DF5353", "#7798BF", "#aaeeee"],
			   chart: {
			      backgroundColor: {
			         linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
			         stops: [
			            [0, 'rgb(96, 96, 96)'],
			            [1, 'rgb(16, 16, 16)']
			         ]
			      },
			      borderWidth: 0,
			      borderRadius: 15,
			      plotBackgroundColor: null,
			      plotShadow: false,
			      plotBorderWidth: 0
			   },
			   title: {
			      style: {
			         color: '#FFF',
			         font: '16px Lucida Grande, Lucida Sans Unicode, Verdana, Arial, Helvetica, sans-serif'
			      }
			   },
			   subtitle: {
			      style: {
			         color: '#DDD',
			         font: '12px Lucida Grande, Lucida Sans Unicode, Verdana, Arial, Helvetica, sans-serif'
			      }
			   },
			   xAxis: {
			      gridLineWidth: 0,
			      lineColor: '#999',
			      tickColor: '#999',
			      labels: {
			         style: {
			            color: '#999',
			            fontWeight: 'bold'
			         }
			      },
			      title: {
			         style: {
			            color: '#AAA',
			            font: 'bold 12px Lucida Grande, Lucida Sans Unicode, Verdana, Arial, Helvetica, sans-serif'
			         }
			      }
			   },
			   yAxis: {
			      alternateGridColor: null,
			      minorTickInterval: null,
			      gridLineColor: 'rgba(255, 255, 255, .1)',
			      minorGridLineColor: 'rgba(255,255,255,0.07)',
			      lineWidth: 0,
			      tickWidth: 0,
			      labels: {
			         style: {
			            color: '#999',
			            fontWeight: 'bold'
			         }
			      },
			      title: {
			         style: {
			            color: '#AAA',
			            font: 'bold 12px Lucida Grande, Lucida Sans Unicode, Verdana, Arial, Helvetica, sans-serif'
			         }
			      }
			   },
			   legend: {
			      itemStyle: {
			         color: '#CCC'
			      },
			      itemHoverStyle: {
			         color: '#FFF'
			      },
			      itemHiddenStyle: {
			         color: '#333'
			      }
			   },
			   labels: {
			      style: {
			         color: '#CCC'
			      }
			   },
			   tooltip: {
			      backgroundColor: {
			         linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
			         stops: [
			            [0, 'rgba(96, 96, 96, .8)'],
			            [1, 'rgba(16, 16, 16, .8)']
			         ]
			      },
			      borderWidth: 0,
			      style: {
			         color: '#FFF'
			      }
			   },
			
			
			   plotOptions: {
			      series: {
			         shadow: true
			      },
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
			
			   toolbar: {
			      itemStyle: {
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
			
			   // special colors for some of the demo examples
			   legendBackgroundColor: 'rgba(48, 48, 48, 0.8)',
			   legendBackgroundColorSolid: 'rgb(70, 70, 70)',
			   dataLabelsColor: '#444',
			   textColor: '#E0E0E0',
			   maskColor: 'rgba(255,255,255,0.3)'
			};
			
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
                            [0, theme.colors[0]],
                            [1, Highcharts.Color(theme.colors[0]).setOpacity(0).get('rgba')]
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
        
        var chart = new Highcharts.Chart(Highcharts.merge(options, theme));
    });
});
                