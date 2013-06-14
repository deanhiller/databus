(function ($) {
	AjaxSolr.TagcloudWidget = AjaxSolr.AbstractFacetWidget.extend({
		afterRequest: function () {
			if (this.manager.response.facet_counts.facet_fields[this.field] === undefined) {
				$(this.target).html(AjaxSolr.theme('no_items_found'));
				return;
			}
			
			var maxCount = 0;
			var objectedItems = [];
			for (var facet in this.manager.response.facet_counts.facet_fields[this.field]) {
				var count = parseInt(this.manager.response.facet_counts.facet_fields[this.field][facet]);
				if (count > maxCount) {
					maxCount = count;
				}
				
				objectedItems.push({ facet: facet, count: count });
			}
			
			objectedItems.sort(function (a, b) {
				return a.facet < b.facet ? -1 : 1;
			});
			
			$(this.target).empty();
			
			var output = $('<table class="search_nav_facet_table">');
			
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
				
				var themeObject = AjaxSolr.theme('tag', facet, this.clickHandler(facet));
				
				td1.append(themeObject);
				td2.append(span.append(objectedItems[i].count));
				tr.append(td1);
				tr.append(td2);
				output.append(tr);
			}			
			
			$(this.target).append(output);
			
			/**
			 * Now insert the total count of this facet into its title
			 */
			if(this.field === 'column_texts') {
				$('#column_count_total').html(objectedItems.length);
			} else if(this.field === 'database_texts') {
				$('#database_count_total').html(objectedItems.length);
			} else if(this.field === 'creator_texts') {
				$('#user_count_total').html(objectedItems.length);
			}
		}
	});
})(jQuery);