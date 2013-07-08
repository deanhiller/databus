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
                