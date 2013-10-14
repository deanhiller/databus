--metasource-version--
1
--metasource-meta--
{
	"name": "Basic chart using flot",
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
			        	"nameInJavascript": "from",
			        	"label": "From",
			        	"help" : "The date time the chart should begin",
			        	"type" : "datetime",
			        	"isRequired" : true
					},
					{
			        	"nameInJavascript": "to",
			        	"label": "To",
			        	"help" : "The date time the chart should end",
			        	"type" : "datetime",
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
			        	"type" : "String",
			        	"defaultValue" : "time",
			        	"isRequired" : true
					},
					{
			        	"nameInJavascript": "valueColumn",
			        	"label": "Value Column Name",
			        	"help" : "The name of the column containing the value in milliseconds",
			        	"type" : "String",
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
    
<script type="text/javascript" src="/public/includes/jquery.flot.js"></script>


--metasource-largechart--
*{
 Notice this large chart has extra features like ZOOM and buttons and such why the small chart version is automatically used in
 stuff like 4 quadrant dashboards such that you can remove zoom and other extraneous features from your chart
 Also, this is a type of comment that is stripped out of the javascript going to the web page completely.
}*

$(function() {
	
	var _title = '${title}';
	var _url = '${url}'+'/${from}'+'/${to}'; //OR specially we could write var _url = '${fullUrl}'; as well which is the same
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
	  var options = {
			  credits: {
			      enabled: false
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
			          
			        }
			      }
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
			      type: 'datetime',
			      title: {
			        text: ''
			      }
			    },
			    yAxis: {
			      endOnTick: false
			       }
			};
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

	var data = [];
	$("#theChart").css("height",height).css("width",width);
	var alreadyFetched = {};

	(function () {
		function onDataReceived(series) {
			data.push(collapseData(series,'value'));
			$.plot("#theChart", data, options);
		}

		$.ajax({
			url: _url,
			type: "GET",
			dataType: "json",
			success: onDataReceived
		});
	})();
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
	
});
