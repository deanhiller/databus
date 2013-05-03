(function ($) {
	AjaxSolr.TagcloudWidget = AjaxSolr.AbstractFacetWidget.extend({
		afterRequest: function () {
			if(this.id !== 'main_facet_widget') {
				return;
			}
			/**
			 * First need to get all the facet names
			 */
			var facetItems = [];
			for (var facetItem in this.manager.response.facet_counts.facet_fields) {
				facetItems.push(facetItem);
			}
			
			/**
			 * Now loop through each facet and do normal tag logic
			 */
			var facetFieldOutput = $('<div>');
			$(this.target).empty();
			
			for (var i in facetItems) {
				var facetItem = facetItems[i];
				
				var tagWidget = null;
				if(DetailManager.widgets.hasOwnProperty(facetItem)) {
					tagWidget = DetailManager.widgets[facetItem];
				} else {
					tagWidget = new AjaxSolr.TagcloudWidget({
						id: facetItem,
						target: '#' + facetItem,
						field: facetItem
					});
				}
				
				DetailManager.addWidget(tagWidget);
				
				var maxCount = 0;
				var objectedItems = [];
				for (var facet in this.manager.response.facet_counts.facet_fields[facetItem]) {
					var count = parseInt(this.manager.response.facet_counts.facet_fields[facetItem][facet]);
					if (count > maxCount) {
						maxCount = count;
					}
					
					objectedItems.push({ facet: facet, count: count });
				}
				
				objectedItems.sort(function (a, b) {
					return a.facet < b.facet ? -1 : 1;
				});
				
				/**
				 * We need to make these facet fields look like the rest of our search UI:
				 * 
				 * 
				 * 
				 * 	<div class="accordion" id="accordion_column_texts">
				 * 		<div class="accordion-group search_nav_group">
				 * 			<div class="accordion-heading">
				 * 				<a id="table_toggle" class="accordion-toggle search_nav_title" data-toggle="collapse" data-parent="#accordion_column_texts" href="#collapse_column_texts">
				 * 					<!-- <i id="table_icon" class="icon-pause" style="margin: 2px 2px 0px 0px;"></i> -->Table Columns &nbsp;&nbsp;
				 * 					<i id="table_icon" class="icon-chevron-right" style="margin: 3px 10px 0px 0px;"></i>
				 * 					<span id="column_count_total" class="badge facet_count_total"></span>
				 * 				</a>
				 * 			</div>
				 * 			<div id="collapse_column_texts" class="accordion-body collapse">
				 * 				<div class="accordion=inner">
				 * 					<div id="column_texts"></div>
				 * 				</div>
				 * 			</div>
				 * 		</div>
				 * 	</div>
				 */
				var accordionDiv = $('<div class="accordion" id="accordion_' + facetItem + '">');
				var accordionGroupDiv = $('<div class="accordion-group search_nav_group">');
				var accordionHeadingDiv = $('<div class="accordion-heading">');
				var tableLinksAnchor = $('<a id="' + facetItem + '_toggle" icon_ref="#' + facetItem + '_icon" class="accordion-toggle search_nav_title" data-toggle="collapse" data-parent="#accordion_' + facetItem + '" href="#collapse_' + facetItem + '">');
				tableLinksAnchor.append("" + facetItem);
				var tableLinksIcon = $('<i id="' + facetItem + '_icon" class="icon-chevron-right" style="margin: 3px 10px 0px 0px;">');
				tableLinksAnchor.append(" ");
				tableLinksAnchor.append(tableLinksIcon);
				
				var facetItemCount = $('<span id="' + facetItem + '_count_total" class="badge facet_count_total">');
				facetItemCount.append(objectedItems.length); 
				tableLinksAnchor.append(facetItemCount);
				accordionHeadingDiv.append(tableLinksAnchor);
				
				var accordionBodyDiv = $('<div id="collapse_' + facetItem + '" class="accordion-body collapse">');
				var innerAccordionBodyDiv = $('<div class="accordion=inner">');
				var facetBodyDiv = $('<div id="' + facetItem + '">');
				innerAccordionBodyDiv.append(facetBodyDiv);
				accordionBodyDiv.append(innerAccordionBodyDiv);
				accordionHeadingDiv.append(accordionBodyDiv);
				
				accordionGroupDiv.append(accordionHeadingDiv);
				accordionDiv.append(accordionGroupDiv);
				
				var facetTable = $('<table class="search_nav_facet_table">');
				
				for (var i = 0, l = objectedItems.length; i < l; i++) {
					var tr = $('<tr class="search_nav_facet_tr">');
					var td1 = $('<td class="search_nav_facet_td">');
					var td2 = $('<td class="search_nav_facet_value_td">'); //objectedItems[i].count
					var span = $('<span class="badge child_facet_count_total">');
					
					var facet = objectedItems[i].facet;
					
					/**
					 * If the value is one of the types (aggregation, database or table) then lets
					 * put its associated icon before it. 
					 */
					if(facet === 'aggregation') {
						var icontd = $('<td class="search_nav_facet_td"><img src="/public/images/icons/x32/aggregation_1_32.png" class="search_type_icon"></td>');
						tr.append(icontd);
					} else if(facet === 'database') {
						var icontd = $('<td class="search_nav_facet_td"><img src="/public/images/icons/x32/database_1_32.png" class="search_type_icon"></td>');
						tr.append(icontd);
					} else if(facet === 'table') {
						var icontd = $('<td class="search_nav_facet_td"><img src="/public/images/icons/x32/table_2_32.png" class="search_type_icon"></td>');
						tr.append(icontd);
					}
					
					var themeObject = AjaxSolr.theme('tag', facet, tagWidget.clickHandler(facet));
					
					td1.append(themeObject);
					td2.append(span.append(objectedItems[i].count));
					tr.append(td1);
					tr.append(td2);
					facetTable.append(tr);
				}
				facetBodyDiv.append(facetTable);	
				
				facetFieldOutput.append(accordionDiv);
								
				/**
				 * Now add the JS to change the icon for open/closing the accordion
				 */
				tableLinksAnchor.click(function() {
					var element = this.attributes.icon_ref.value;
					if($(element).hasClass("icon-chevron-down")) {
						$(element).removeClass("icon-chevron-down");
						$(element).attr('class', 'icon-chevron-right');
					} else {
						$(element).removeClass("icon-chevron-right");
						$(element).attr('class', 'icon-chevron-down');
					}
				});
			}
			
			$(this.target).append(facetFieldOutput);
		}
	});
})(jQuery);