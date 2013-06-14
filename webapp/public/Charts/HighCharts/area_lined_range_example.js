// Raw Data Example that has been already cleaned and splined (i.e. logged)
//https://databus.nrel.gov/api/rawdataV1/wind103W_GloCM22log_m/1371051120000/1371052260000      - JUST RAW DATA
//			url		 /api/rawdataV1/     <table>         / start time  /  end time

// Typical Client Data Request Example
//https://databus.nrel.gov/api/splinesV2/limitderivative/60000/0/rangecleanV1/-20000000/20000000/rawdataV1/D73937763phaseRealPower/1371051120000/1371052260000
//             url        /api/ module  /  module method/ rate/?/   module   / low val / highval/rawdataV1/       <table>         / start time  /  end time
//												   ? == epoch offset

// Pure Raw Data Example (nothing done to data)
//https://databus.nrel.gov/api/rawdataV1/D73937763phaseRealPower/1371051120000/1371052260000


// EXAMPLE of dynamic data pull : views/gui/MyStuff/tableData.html

// GOOD ONE TO TEST WITH: https://databus.nrel.gov/api/rawdataV1/wind103W_GloCM22log_m/1370908800000/1370995199999
// GOOD ONE IN CSV: https://databus.nrel.gov/api/csv/rawdataV1/D73937763phaseRealPower/1371051120000/1371052260000

$(function () {
    var _solrProtocol = 'http';
    var days = 2;
    var endTime = (new Date())
        .getTime();
    var startTime = endTime - days * 24 * 3600000;

    var url = 'http://' + window.location.host + '/api/aggregation/chart_avg_sums_hour/' + startTime + '/' + endTime;

    //url = _solrProtocol + '://' + window.location.host + '/api/sumstreamsV1/wind303W_RainSum_log_m/' + startTime + '/' + endTime + '?singlenode=true&callback=?';

    $.getJSON(url, function (chartData) {
        var ranges = [];
        var averages = [];

        var lastAvg = 0;
        $.each(chartData.data, function (key, val) {
            var time = val.time;
            var v1 = val.D73937763phaseRealPower_log1m;
            var v2 = val.D73937773phaseRealPower_log1m;

            if (v1 === undefined) {
                v1 = v2;
            }

            if (v2 === undefined) {
                v2 = v1;
            }

            if ((v1 === undefined) && (v2 == undefined)) {
                v1 = lastAvg;
                v2 = lastAvg;
            }

            ranges.push([time, v1 / 1000, v2 / 1000]);

            var avg = val.value / 2 / 1000;
            lastAvg = avg;

            averages.push([val.time, avg]);
        });

        var options = {
            chart: {
                renderTo: 'container'
            },

            title: {
                text: '3phaseRealPower D73937773/D73937763'
            },

            xAxis: {
                type: 'datetime'
            },

            yAxis: {
                title: {
                    text: null
                },
                min: 0,
                max: 90
            },

            tooltip: {
                crosshairs: true,
                shared: true,
                valueSuffix: 'W'
            },

            legend: {},

            series: [{
                name: 'Kilowatt',
                data: averages,
                zIndex: 1,
                marker: {
                    fillColor: 'white',
                    lineWidth: 2,
                    lineColor: Highcharts.getOptions()
                        .colors[0]
                }
       }, {
                name: 'Range',
                data: ranges,
                type: 'arearange',
                lineWidth: 0,
                linkedTo: ':previous',
                color: Highcharts.getOptions()
                    .colors[0],
                fillOpacity: 0.3,
                zIndex: 0
       }]

        };

        var theme_alre = {
            colors: ["#DDDF0D", "#55BF3B", "#DF5353", "#7798BF", "#aaeeee", "#ff0066", "#eeaaee",
         "#55BF3B", "#DF5353", "#7798BF", "#aaeeee"],
            chart: {
                backgroundColor: {
                    linearGradient: {
                        x1: 0,
                        y1: 0,
                        x2: 1,
                        y2: 1
                    },
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
                            linearGradient: {
                                x1: 0,
                                y1: 0,
                                x2: 0,
                                y2: 1
                            },
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
                        linearGradient: {
                            x1: 0,
                            y1: 0,
                            x2: 0,
                            y2: 1
                        },
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
                                linearGradient: {
                                    x1: 0,
                                    y1: 0,
                                    x2: 0,
                                    y2: 1
                                },
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
                                linearGradient: {
                                    x1: 0,
                                    y1: 0,
                                    x2: 0,
                                    y2: 1
                                },
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
                    linearGradient: {
                        x1: 0,
                        y1: 0,
                        x2: 0,
                        y2: 1
                    },
                    stops: [
                  [0.4, '#888'],
                  [0.6, '#555']
               ]
                },
                barBorderColor: '#CCC',
                buttonArrowColor: '#CCC',
                buttonBackgroundColor: {
                    linearGradient: {
                        x1: 0,
                        y1: 0,
                        x2: 0,
                        y2: 1
                    },
                    stops: [
                  [0.4, '#888'],
                  [0.6, '#555']
               ]
                },
                buttonBorderColor: '#CCC',
                rifleColor: '#FFF',
                trackBackgroundColor: {
                    linearGradient: {
                        x1: 0,
                        y1: 0,
                        x2: 0,
                        y2: 1
                    },
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

        var theActualChart = new Highcharts.Chart(Highcharts.merge(options, theme_alre));

    })
})