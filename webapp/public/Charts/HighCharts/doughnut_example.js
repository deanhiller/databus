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
var _name = 'PowerConsumption';
var _title = 'RSF Building Power Consumption';

var _protocol = 'http';
var _database = '/api/firstvaluesV1/50/aggregation/RSF_PowerConsumption_Example?reverse=true';
/**
 * |DATABUS_CHART|
 * ***********************************************************************
 * REQUIRED FOR DATABUS URI  -- END --
 * ***********************************************************************
 */

	var days = 2;
    	var endTime = (new Date())
        .getTime();
    	var startTime = endTime - days * 24 * 3600000;

    	var url = _protocol + '://' + window.location.host + _database;
    	
    	//"RSF_LIGHTING_1MINl1m": 74243.11065477342,
	//"RSF_BUILDING_1MINl1m": -451521.2354785786,
	//"value": -784268.0896022104,
	//"RSF_DATACENTER_1MINl1m": 5290.259885313927,
	//"RSF_ELEVATOR_1MINl1m": 63140.06062611822,
	//"RSF_HEATING_1MINl1m": 42058.61024692249,
	//"RSF_MISC_1MINl1m": -753068.6534745345,
	//"RSF_MECHANICAL_1MINl1m": 68952.85076945949,
	//"RSF_COOLING_1MINl1m": 166636.9071683151
	
	$.getJSON(url, function (chartData) {
	   var RSF_LIGHTING_1MINl1m_Total = 0;
        var RSF_BUILDING_1MINl1m_Total = 0;
        var RSF_DATACENTER_1MINl1m_Total = 0;
        var RSF_ELEVATOR_1MINl1m_Total = 0;
        var RSF_HEATING_1MINl1m_Total = 0;
        var RSF_MISC_1MINl1m_Total = 0;
        var RSF_MECHANICAL_1MINl1m_Total = 0;
        var RSF_COOLING_1MINl1m_Total = 0;

        $.each(chartData.data, function (key, val) {
            var time = val.time;
            
            var RSF_LIGHTING_1MINl1m = Math.abs(val.RSF_LIGHTING_1MINl1m);
            var RSF_BUILDING_1MINl1m = Math.abs(val.RSF_BUILDING_1MINl1m);
            var RSF_DATACENTER_1MINl1m = Math.abs(val.RSF_DATACENTER_1MINl1m);
            var RSF_ELEVATOR_1MINl1m = Math.abs(val.RSF_ELEVATOR_1MINl1m);
            var RSF_HEATING_1MINl1m = Math.abs(val.RSF_HEATING_1MINl1m);
            var RSF_MISC_1MINl1m = Math.abs(val.RSF_MISC_1MINl1m);
            var RSF_MECHANICAL_1MINl1m = Math.abs(val.RSF_MECHANICAL_1MINl1m);
            var RSF_COOLING_1MINl1m = Math.abs(val.RSF_COOLING_1MINl1m);
            
            if(RSF_LIGHTING_1MINl1m != undefined) {
            	RSF_LIGHTING_1MINl1m_Total += RSF_LIGHTING_1MINl1m;
            }
            	
            if(RSF_BUILDING_1MINl1m != undefined) {
            	RSF_BUILDING_1MINl1m_Total += RSF_BUILDING_1MINl1m;
            }
            	
            if(RSF_DATACENTER_1MINl1m != undefined) {
            	RSF_DATACENTER_1MINl1m_Total += RSF_DATACENTER_1MINl1m;
            }
            	
        	  if(RSF_ELEVATOR_1MINl1m != undefined) {
            	RSF_ELEVATOR_1MINl1m_Total += RSF_ELEVATOR_1MINl1m;
            }
            	
            if(RSF_HEATING_1MINl1m != undefined) {
            	RSF_HEATING_1MINl1m_Total += RSF_HEATING_1MINl1m;
            }
            	
            if(RSF_MISC_1MINl1m != undefined) {
            	RSF_MISC_1MINl1m_Total += RSF_MISC_1MINl1m;
            }
            	
            if(RSF_MECHANICAL_1MINl1m != undefined) {
            	RSF_MECHANICAL_1MINl1m_Total += RSF_MECHANICAL_1MINl1m;
            }
            	
            if(RSF_COOLING_1MINl1m != undefined) {
            	RSF_COOLING_1MINl1m_Total += RSF_COOLING_1MINl1m;
            }
            	
        });
        
        var HVACtotal = RSF_HEATING_1MINl1m_Total + RSF_COOLING_1MINl1m_Total;
        var Mechanicaltotal = RSF_MECHANICAL_1MINl1m_Total + RSF_ELEVATOR_1MINl1m_Total + RSF_MISC_1MINl1m_Total + RSF_LIGHTING_1MINl1m_Total;
        var Computingtotal = RSF_BUILDING_1MINl1m_Total + RSF_DATACENTER_1MINl1m_Total;

        var thisTotal = HVACtotal + Mechanicaltotal + Computingtotal;
        
        var HVAC = [precision(((RSF_HEATING_1MINl1m_Total/HVACtotal)*100), 2), precision(((RSF_COOLING_1MINl1m_Total/HVACtotal)*100), 2)];
        var HVACpercentage = precision((HVACtotal/thisTotal) * 100, 2);
        
        var Mechanical = [precision(((RSF_MECHANICAL_1MINl1m_Total/Mechanicaltotal)*100), 2), precision(((RSF_ELEVATOR_1MINl1m_Total/Mechanicaltotal)*100), 2), precision(((RSF_MISC_1MINl1m_Total/Mechanicaltotal)*100), 2), precision(((RSF_LIGHTING_1MINl1m_Total/Mechanicaltotal)*100), 2)];
        var Mechanicalpercentage = precision((Mechanicaltotal/thisTotal) * 100, 2);
        
        var Computing = [precision(((RSF_BUILDING_1MINl1m_Total/Computingtotal)*100), 2), precision(((RSF_DATACENTER_1MINl1m_Total/Computingtotal)*100), 2)];
        var Computingpercentage = precision((Computingtotal/thisTotal) * 100, 2);
    
        var colors = Highcharts.getOptions().colors,
            categories = ['HVAC', 'Mechanical', 'Computing', 'Lighting'],
            name = 'Departments',
            data = [{
                    y: HVACpercentage,
                    color: colors[0],
                    drilldown: {
                        name: 'HVAC',
                        categories: ['HEATING', 'COOLING'],
                        data: HVAC,
                        color: colors[0]
                    }
                }, {
                    y: Mechanicalpercentage,
                    color: colors[1],
                    drilldown: {
                        name: 'Mechanical',
                        categories: ['MECHANICAL', 'ELEVATOR', 'MISC', 'LIGHTING'],
                        data: Mechanical,
                        color: colors[1]
                    }
                }, {
                    y: Computingpercentage,
                    color: colors[2],
                    drilldown: {
                        name: 'Computing',
                        categories: ['BUILDING', 'DATACENTER'],
                        data: Computing,
                        color: colors[2]
                    }
                }];
    
    
        // Build the data arrays
        var departmentData = [];
        var groupsData = [];
        for (var i = 0; i < data.length; i++) {
    
            // add browser data
            departmentData.push({
                name: categories[i],
                y: data[i].y,
                color: data[i].color
            });
    
            // add version data
            for (var j = 0; j < data[i].drilldown.data.length; j++) {
                var brightness = 0.2 - (j / data[i].drilldown.data.length) / 5 ;
                groupsData.push({
                    name: data[i].drilldown.categories[j],
                    y: data[i].drilldown.data[j],
                    color: Highcharts.Color(data[i].color).brighten(brightness).get()
                });
            }
        }
        
	   var theme_de = {
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
		         font: 'bold 10px "Trebuchet MS", Verdana, sans-serif'
		      }
		   },
		   xAxis: {
		      gridLineColor: '#333333',
		      gridLineWidth: 1,
		      labels: {
		         style: {
		         	  fontSize: '8px',
		            color: '#A0A0A0'
		         }
		      },
		      lineColor: '#A0A0A0',
		      tickColor: '#A0A0A0',
		      title: {
		         style: {
		            color: '#CCC',
		            fontWeight: 'bold',
		            fontSize: '10px',
		            fontFamily: 'Trebuchet MS, Verdana, sans-serif'
		
		         }
		      }
		   },
		   yAxis: {
		      gridLineColor: '#333333',
		      labels: {
		         style: {
		         	  fontSize: '8px',
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
		            fontSize: '10px',
		            fontFamily: 'Trebuchet MS, Verdana, sans-serif'
		         }
		      }
		   },
		   tooltip: {
		      backgroundColor: 'rgba(0, 0, 0, 0.75)',
		      style: {
		         	  fontSize: '8px',
		         color: '#F0F0F0'
		      }
		   },
		   toolbar: {
		      itemStyle: {
		         	  fontSize: '8px',
		         color: 'silver'
		      }
		   },
		   plotOptions: {
		      line: {
		         dataLabels: {
		         	  fontSize: '8px',
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
		         font: '7pt Trebuchet MS, Verdana, sans-serif',
		         color: '#A0A0A0'
		      },
		      itemHoverStyle: {
		         	  fontSize: '8px',
		         color: '#FFF'
		      },
		      itemHiddenStyle: {
		         	  fontSize: '8px',
		         color: '#444'
		      }
		   },
		   credits: {
		      style: {
		         fontSize: '8px',
		         color: '#666'
		      }
		   },
		   labels: {
		      style: {
		         fontSize: '8px',
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
		         	  fontSize: '8px',
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
		         	  fontSize: '8px',
		         backgroundColor: '#333',
		         color: 'silver'
		      },
		      labelStyle: {
		         	  fontSize: '8px',
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
		         	  fontSize: '8px',
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
		         	  fontSize: '8px',
		   maskColor: 'rgba(255,255,255,0.3)'
		};
    
        // Create the chart
        var options = {
            chart: {
                type: 'pie',
                renderTo: 'container'
            },
            title: {
                text: _title
            },
            yAxis: {
                title: {
                    text: 'Total percent Power Consumption'
                }
            },
            plotOptions: {
                pie: {
                    shadow: false,
                    center: ['50%', '50%']
                }
            },
            tooltip: {
        	    valueSuffix: '%'
            },
            series: [{
                name: 'Departments',
                data: departmentData,
                size: '60%',
                dataLabels: {
                    formatter: function() {
                        return this.y > 5 ? this.point.name : null;
                    },
                    color: 'white',
                    distance: -30
                }
            }, {
                name: 'Groups',
                data: groupsData,
                size: '80%',
                innerSize: '60%',
                dataLabels: {
                    formatter: function() {
                        // display only if larger than 1
                        return this.y > 1 ? '<b style="font-size:8px;">'+ this.point.name +':</b><i style="font-size:8px;">'+ this.y +'%</i>'  : null;
                    }
                }
            }]
        };
        
        var theActualChart = new Highcharts.Chart(Highcharts.merge(options, theme_de));
    })
})

function precision(value, precision) {
    var power = Math.pow(10, precision || 0);
    return (Math.round(value * power) / power);
}
    