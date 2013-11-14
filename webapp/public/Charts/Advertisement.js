--metasource-version--
1
--metasource-meta--
{
	"name": "Buy **More Charts**",
	"description":"Buy More Charts",
	"needsHighChartsLicense" : true,
	"pages" : [
			{
				"title" : "Buy More Charts",
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
			        	"type" : "url",
			        	"isRequired" : true
					},
					{
						"nameInJavascript" : "timepanel",
			        	"type" : "timepanel",
			        	"isRequired" : true
					}
					]
			}
	]
}
--metasource-header--
--metasource-largechart--