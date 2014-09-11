/** 
 * THEME OUTPUT
 * 

(11:08:27 AM) Justin Collins: allTerms_texts : EVERYTHING I can index related to this item.  This is just a dumping ground for everything I can find
(11:08:53 AM) Justin Collins: columns_texts : names of columns in this table
(11:09:07 AM) Justin Collins: id : name of table
(11:09:16 AM) Justin Collins: type : 'table' (right now)
(11:09:58 AM) Justin Collins: database_texts : name of owning database(schema)
(11:10:18 AM) Justin Collins: databaseDescription_texts : description from owning database (schema)
(11:10:59 AM) Justin Collins: (those two are really denormalized down from the owning schema, so you'll see them duplicated in each table owned by the same schema)
(11:11:04 AM) Justin Collins: I think thats it
(11:11:17 AM) Justin Collins: version : is internal to solr
(11:11:22 AM) Justin Collins: forgot that one
(11:11:29 AM) Justin Collins: sigh
(11:11:37 AM) Justin Collins: IM formatted that on my side at least.
(11:11:39 AM) Justin Collins: it's
(11:11:49 AM) Justin Collins: <underscore>version<underscore>


		SOLR OBJECT:
			Results are based on object 'type'.  All fields possible in Object:
			
				allTerms_texts
				column_texts
				description_texts
				creator_texts
				databaseDescription_texts
				database_texts
				id
				type
			
			Type = 'database'
				allTerms_texts 			- All terms found in INDEXING
				column_texts 				- NULL (JS treats as undefined)
				description_texts			- Database description
				creator_texts				- Database creator
				databaseDescription_texts	- Database description (same as description_texts)
				database_texts				- NULL (JS treats as undefined)
				id						- Database name
				type						- 'database'
			
			Type = 'table'
				allTerms_texts 			- All terms found in INDEXING
				column_texts 				- Table column names
				description_texts			- Table description
				creator_texts				- Table creator
				databaseDescription_texts	- Owning database description
				database_texts				- Owning database name
				id						- Table name
				type						- 'table'
				
			Type = 'aggregation'
				allTerms_texts 			- All terms found in INDEXING
				tables_in_texts			- ?? Tables within aggregation ??
				table_column_texts			- ?? Array of above tables w/ columns??
				description_texts			- Aggregation description
				creator_texts				- Aggregation creator
				databaseDescription_texts	- Owning database description
				database_texts				- Owning database name
				databases_in_texts			- ?? Databases within aggregation
				id						- Aggregation name
				type						- 'aggregation'
			
			
		Empty Fields (Solr just doenst sent them.  If not there JS puts "undefined".)
			1) http://grokbase.com/t/lucene/solr-user/10a1e90pwm/how-to-tell-solr-to-return-all-fields-including-empty-fields
			2) or do nothing but if/elses
 
 */
(function ($) {
	AjaxSolr.theme.prototype.result = function (doc, snippet) {
		var output = 	'<table class="guisearch_result">';
		
		if(doc.type == 'aggregation') {
			output += '<td class="guisearch_result_icon_td"><img src="/public/images/icons/x48/aggregation_1_48.png"></td>';
		} else if(doc.type == 'database') {
			output += '<td class="guisearch_result_icon_td"><img src="/public/images/icons/x48/database_1_48.png"></td>';
		} else if(doc.type == 'table') {
			output += '<td class="guisearch_result_icon_td"><img src="/public/images/icons/x48/table_2_48.png"></td>';
		}
		output += '<td class="guisearch_result_summary_td"><table class="table outer_result_table"><tr><td class="result_title_value">' + doc.id + '</td></tr>';
		output += snippet + '</table>';
		output += "</td></tr></table>";
		
		return output;
	};
	
	AjaxSolr.theme.prototype.snippet = function (doc) {
		/**
		 * 
		 * Sometimes the JS DOM will print "undefined" when it encounters a 
		 * null field, other times will crash.  We need to check all of the 
		 * possible variables that we'll use in order to make sure we always
		 * have a value to show. 
		 */
		 
		 var item_description;
		 
		 var output = '<tr><td class="guisearch_result_summary">';
		 if(typeof doc.description_texts === 'undefined') {
		 	//output += '<i>No Description</i>&nbsp;&nbsp;<i class="icon-download"></i></td></tr>';
		 	output += '<i>No Description</i></td></tr>';
		 } else {
		 	if(doc.type == 'aggregation') {		 		
		 		if(doc.description_texts.length == 1) {
		 			output += '<table class="inner_result_table">';
		 			output += '<tr><td class="inner_result_td">';
		 			output += doc.description_texts[0];
		 			output += '</td></tr></table>';
		 		} else if(doc.description_texts.length == 2) {
		 			output += '<table class="inner_result_table">';
		 			output += '<tr><td class="inner_result_td">';
		 			output += doc.description_texts[0];
		 			output += '</td></tr>';
		 			output += '<tr><td class="inner_result_td">';
		 			output += doc.description_texts[1];
		 			//output += '&nbsp;&nbsp;<i class="icon-download"></i>';
		 			output += '</td></tr></table>';
		 		} else if(doc.description_texts.length > 2) {
		 			output += '<table class="inner_result_table">';
		 			output += '<tr><td class="inner_result_td">';
		 			output += doc.description_texts[0];
		 			output += '</td></tr>';
		 			output += '<tr><td class="inner_result_td">';
		 			output += doc.description_texts[1];
		 			output += '</td></tr>';
		 			output += '<tr><td class="inner_result_td">';
		 			//output += '...&nbsp;&nbsp;<i class="icon-download"></i>';
		 			output += '...';
		 			output += '</td></tr></table>';
		 		} else {
		 			output += '<table class="inner_result_table"><tr><td><i>No URLs</i></td></tr></table>';
		 		}
		 	} else {
		 		//output += doc.description_texts + '<span style="white-space: nowrap;">&nbsp;&nbsp;<i class="icon-download"></i></span></td></tr>';
		 		output += doc.description_texts + '</td></tr>';
		 	}
		 }
		
		/*
		if(doc.type == 'aggregation') {
			output += '<tr><td class="guisearch_result_title_td">Owning Database:</td><td class="guisearch_result_value_td" style="border: 0px;">' + db_parent + '</td></tr>';
			output += '<tr><td class="guisearch_result_title_td">Database Description:</td><td class="guisearch_result_value_td">' + db_desc + '</td></tr>';
			output += '<tr><td class="guisearch_result_title_td">Aggregation Creator:</td><td class="guisearch_result_value_td">' + item_creator + '</td></tr>';
			output += '<tr><td class="guisearch_result_title_td">Aggregation URLs:</td><td class="guisearch_result_value_td">' + item_description + '</td></tr>';
			//output += '<tr><td class="guisearch_result_title_td">All Matching Terms:</td><td class="guisearch_result_value_td">' + doc.allTerms_texts + '</td></tr>';
		} else if(doc.type == 'database') {
			output += '<tr><td class="guisearch_result_title_td">Database Creator:</td><td class="guisearch_result_value_td" style="border: 0px;">' + item_creator + '</td></tr>';
			output += '<tr><td class="guisearch_result_title_td">Database Description:</td><td class="guisearch_result_value_td">' + db_desc + '</td></tr>';
			//output += '<tr><td class="guisearch_result_title_td">All Matching Terms:</td><td class="guisearch_result_value_td">' + doc.allTerms_texts + '</td></tr>';
		} else if(doc.type == 'table') {
			output += '<tr><td class="guisearch_result_title_td">Owning Database:</td><td class="guisearch_result_value_td" style="border: 0px;">' + db_parent + '</td></tr>';
			output += '<tr><td class="guisearch_result_title_td">Database Description:</td><td class="guisearch_result_value_td">' + db_desc + '</td></tr>';
			output += '<tr><td class="guisearch_result_title_td">Table Creator:</td><td class="guisearch_result_value_td">' + item_creator + '</td></tr>';
			output += '<tr><td class="guisearch_result_title_td">Table Description:</td><td class="guisearch_result_value_td">' + item_description + '</td></tr>';
			output += '<tr><td class="guisearch_result_title_td">Table Columns:</td><td class="guisearch_result_value_td">' + table_columns + '</td></tr>';
			//output += '<tr><td class="guisearch_result_title_td">All Matching Terms:</td><td class="guisearch_result_value_td">' + doc.allTerms_texts + '</td></tr>';
		}
		*/	
		
		/**
		 * Drop Down Content
		 */
		 var db_parent;
		 var db_desc;
		 var item_creator;
		 var table_columns;
		 
		 // db_parent
		 if(typeof doc.database_texts === 'undefined') {
		 	db_parent = "<i>unknown</i>";
		 } else {
		 	db_parent = doc.database_texts[0];
		 }
		 
		 // db_desc
		 if(typeof doc.databaseDescription_texts === 'undefined') {
		 	db_desc = "<i>No Description</i>";
		 } else {
		 	db_desc = doc.databaseDescription_texts[0];
		 }
		 
		 // item_creator
		 if(typeof doc.creator_texts === 'undefined') {
		 	item_creator = "<i>unknown</i>";
		 } else {
		 	item_creator = doc.creator_texts[0];
		 }
		// table_columns
		 if(typeof doc.column_texts === 'undefined') {
		 	table_columns = "<i>none</i>";
		 } else {
		 	table_columns = '<table class="inner_result_table">';
		 	
		 	for(var i = 0; i < doc.column_texts.length; i++) {
		 		table_columns += '<tr class="inner_result_tr"><td class="inner_result_td">' + doc.column_texts[i] + '</td></tr>';
		 	}
		 	
		 	table_columns += '</table>';
		 }
		
		return output;
	};
	
	/**
	 * AjaxSolr TAG theme:
	 *	
	 *  	This method is confusing (as w/ the next one).  Its "tag" but what it really
	 *	is are the left FACET values. 
	 */
	AjaxSolr.theme.prototype.tag = function (value, handler) {
		return $('<a href="#" class="tagcloud_item"/>').text(value).click(handler);
	};
	
	/**
	 * AjaxSolr facet_link theme:
	 *	
	 *  	This method is confusing (as w/ the previous one).  Its "facet_link" but what it really
	 *	is are the searchable values associated with each individual result. 
	 */
	AjaxSolr.theme.prototype.facet_link = function (value, handler) {
		return $('<a href="#"/>').text(value).click(handler);
	};
	
	AjaxSolr.theme.prototype.no_items_found = function () {
		return 'no items found in current selection';
	};
})(jQuery);