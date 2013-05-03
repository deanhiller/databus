var days = 2;

  var endTime = (new Date()).getTime(),
    startTime = endTime - days * 24 * 3600000,
    maximized = false,
    maximizedStream,
    chartMap = {},
    height = 500,
    width = 951;

  //Set explicit start time
  //startTime = 1357596216977,
  //endTime = startTime + days*24*3600000,

  $(window).resize(function () {
    if (maximized) {
      chart.setSize($(window).width(), $(window).height(), false);
    }
  });

  Highcharts.extend(Highcharts.getOptions().lang, {
    zoomTitle: 'Zoom the chart'
  });

  Highcharts.Renderer.prototype.symbols.zoomInIcon = function (x, y, width, height) {
    return crisp([
      'M',
    x + width * 0.88575, y + height * 0.219167,
      'L',
    x + width, y + height * 1 / 3,
    x + width, y,
    x + width * 2 / 3, y,
    x + width * 0.780833, y + height * 0.114167,
    x + width * 0.5, y + height * 0.395,
    x + width * 0.219167, y + height * 0.114167,
    x + width * 1 / 3, y,
    x, y,
    x, y + height * 1 / 3,
    x + width * 0.114167, y + height * 0.219167,
    x + width * 0.395, y + height * 0.5,
    x + width * 0.114167, y + height * 0.780833,
    x, y + height * 2 / 3,
    x, y + height,
    x + width * 1 / 3, y + height,
    x + width * 0.219083, y + height * 0.88575,
    x + width * 1 / 2, y + height * 0.604917,
    x + width * 0.780833, y + height * 0.885833,
    x + width * 2 / 3, y + height,
    x + width, y + height,
    x + width, y + height * 2 / 3,
    x + width * 0.885833, y + height * 0.780833,
    x + width * 0.604917, y + height * 1 / 2,
      'Z']);
  };

  Highcharts.setOptions({
    credits: {
      enabled: false
    },
    exporting: {
      buttons: {
        exportButton: {
          enabled: false
        },
        printButton: {
          enabled: false
        },
        zoomButton: {
          symbol: 'zoomInIcon',
          x: -10,
          symbolFill: '#B5C9DF',
          hoverSymbolFill: '#779ABF',
          _titleKey: 'zoomTitle'
        }
      }
    },
    global: {
      useUTC: false
    },
    loading: {
      labelStyle: {
        color: '#FFF'
      },
      style: {
        backgroundColor: '#444'
      }
    },
    navigator: {
      margin: 50
    },
    plotOptions: {
      series: {
        dataGrouping: {
          approximation: "high",
          groupPixelWidth: 1
        },
        marker: {
          //enabled: true
        }
      }
    },
    rangeSelector: {
      inputEnabled: false,
      buttons: [{
        type: 'hour',
        count: 1,
        text: '1h'
      }, {
        type: 'hour',
        count: 6,
        text: '6h'
      }, {
        type: 'hour',
        count: 12,
        text: '12h'
      }, {
        type: 'hour',
        count: 24,
        text: '24h'
      }, {
        type: 'hour',
        count: 48,
        text: '48h'
      }, {
        type: 'all',
        text: 'All'
      }],
      selected: 5
    },
    tooltip: {
      backgroundColor: {
        linearGradient: {
          x1: 0,
          y1: 0,
          x2: 0,
          y2: 1
        },
        stops: [
          [0, '#FFF'],
          [1, '#EEE']
        ]
      },
      borderColor: 'gray',
      borderWidth: 1,
      yDecimals: 2
    },
    xAxis: {
      dateTimeLabelFormats: {
        second: '%l:%M:%S%P',
        minute: '%l:%M%P',
        hour: '%l%P',
        day: '%e %b',
        week: '%e %b'
      },
      maxZoom: 600000,
      type: 'datetime',
      title: {
        text: ''
      }
    },
    yAxis: {
      endOnTick: false
      //min: 0
    }
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

  function collapseData(data, reduce) {
    var collapsedData = [];
    $.each(data.data, function (index, value) {
      collapsedData.push([value.time, (value == "NaN") ? null : value.value / (reduce===true ? 1000 : 1)]);
    });
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

  function addChart(stream, div, reverseAxis, singleStream, title) {
    reverseAxis = typeof reverseAxis !== 'undefined' ? reverseAxis : false;
    singleStream = typeof singleStream !== 'undefined' ? singleStream : false;
    title = typeof title !== 'undefined' ? title : false;

    var url;
    if (!singleStream) {
      url = _solrProtocol + '://' + window.location.host + '/api/sumstreamsV1/' + stream + '/' + startTime + '/' + endTime + '?singlenode=true&callback=?';
    } else {
      url = _solrProtocol + '://' + window.location.host + '/api/rangecleanV1/-2000000/2000000/rawdataV1/' + stream + '/' + startTime + '/' + endTime + '?singlenode=true&callback=?';
    }
    console.log(stream + ': ' + url);
    $(div).append($('<div class="chart" />').attr('id', stream));
    $.getJSON(url, function (data) {
      var chart = new Highcharts.StockChart({
        chart: {
          renderTo: stream
        },
        exporting: {
          buttons: {
            zoomButton: {
              onclick: function () {
                $('#' + this.options.chart.renderTo).toggleClass('overlay');
                if (!maximized) {
                  this.setSize($(window).width(), $(window).height(), false);
                  maximized = true;
                  maximizedStream = stream;
                } else {
                  this.setSize($('.chart').width(), $('.chart').height(), false);
                  maximized = false;
                }
              }
            }
          }
        },
        legend: {
          align: 'center',
          enabled: true,
          floating: true,
          verticalAlign: 'bottom',
          y: -57
        },
        navigator: {
          yAxis: {
            reversed: reverseAxis
          }
        },
        series: [{
          name: 'Aggregation',
          data: collapseData(data, (singleStream!==true ? true : false))
        }],
        title: {
          text: (title!==false ? title : stream) + ' - ' + days + ' Days'
        },
        xAxis: {
          ordinal: false
        },
        yAxis: {
          title: {
            text: ''
          },
          labels: {
            align: "right",
            x: -2,
            y: 4
          },
          reversed: reverseAxis
        }
      });
      
      chart.showLoading();
      chartMap[stream] = chart;

      if (!singleStream) {
        $.getJSON(_solrProtocol + '://' + window.location.host + '/api/sumstreamsV1/aggregation/' + stream + '?singlenode=true&callback=?', function (data) {
          var urls = data.urls,
            responses = [],
            aggregation = [];
          //urls = ['splinesV1/limitderivative/60000/rangecleanV1/-2000000/2000000/rawdataV1/D73937763phaseRealPower', 'rangecleanV1/-2000000/2000000/rawdataV1/D73937763phaseRealPower'];
          //if (urls.length > 1) {
          $.each(urls, function (urlIndex, value) {
            var name = value.substring(value.lastIndexOf('/') + 1);
            chart.addSeries({
              name: name,
              data: null
            }, true, false);
            // Raw Data:
            var url = _solrProtocol + '://' + window.location.host + '/api/' + (value.indexOf('invertV1/') === 0 ? 'invertV1/' : '') + value.substring(value.indexOf('rangecleanV1/-2000000/2000000/rawdataV1/')) + '/' + startTime + '/' + endTime + '?singlenode=true&callback=?';
            // Splined Data:
            //var url = _solrProtocol + '://' + window.location.host + '/api/' + value + '/' + startTime + '/' + endTime + '?singlenode=true&callback=?';
            $.getJSON(url).then(

            function (data) {
              data = collapseData(data, true);
              responses.push(data);
              chart.series[urlIndex + 2].setData(data);
              /*if (urlIndex === 0) {
                chart.addSeries({
                  name: 'Custom Spline',
                  data: data,
                  type: 'spline'
                }, true, false);
              }*/
              if (responses.length == urls.length) {
                window.responses = responses;
                var i, j, sum, lengths = [],
                  minLength;
                $.each(responses, function (i) {
                  lengths.push(responses[i].length);
                });
                minLength = Math.min.apply(null, lengths);
                for (i = 0; i < minLength; i++) {
                  sum = 0;
                  for (j = 0; j < responses.length; j++) {
                    sum += responses[j][i][1];
                  }
                  aggregation.push([responses[0][i][0], sum]);
                }
                /*chart.addSeries({
              name: 'Sum',
              data: aggregation
            }, true, false);*/
                chart.hideLoading();
              }
            },

            function () {
              console.log("ERROR fetching " + name);
            });
            console.log(stream + ': ' + url);
          });
          /*}
          else {
            chart.hideLoading();
          }*/
        });
      }
      else {
        chart.hideLoading();
      }
    });
  }

  $(document).bind('keyup', 'esc', function () {
    if (maximized) {
      chartMap[maximizedStream].setSize(width, height, false);
      maximized = false;
      $('.overlay').toggleClass('overlay');
    }
  });

  //addChart("RSF_BUILDING");
  //addChart("RSF_PV", true);
  //addChart("RSF_DATACENTER");
  //addChart("RSF_ELEVATOR");
  //addChart("RSF_LIGHTING");
  //addChart("RSF_MECHANICAL");
  //addChart("RSF_PLUG");
  //addChart("RSF_MISC");
  //addChart("RSF_HEATING");
  //addChart("RSF_COOLING");
  //addChart("GARAGE_BUILDING");
  //addChart("GARAGE_EVCS");
  //addChart("wind103W_GloCM22", false, true, "Global Irradiance - Global CM22 Vent [W/m^2]");
  //addChart("wind103W_DirCH1", false, true, "Avg Direct Irradiance - Direct CH1 [W/m^2]");
  //addChart("wind303W_Temp", false, true, "Deck Dry Bulb Temp [deg C]");
  //addChart("wind303W_WS_AT_6", false, true, "RH [%]");
  //addChart("wind303W_RainSum", false, true, "Precipitation [mm]");
  //addChart("wind303W_EFM", false, true, "Precipitation [mm]");
;