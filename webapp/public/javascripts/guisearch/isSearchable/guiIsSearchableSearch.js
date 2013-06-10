
/**
 * Ajax Solr MANAGER 
 * 
 */
var IsSearchableManager;
(function ($) {
	$(function () {
		IsSearchableManager = new AjaxSolr.Manager({
			/**
			 * Important:
			 * 
			 *	Had to modify AbstractManager.js and change the
			 *  	servlet variable to an empty string.
			 *  		servlet: '',
			 */
			solrUrl: _solrProtocol + '://' + window.location.host + '/api/search/databusmeta'
		});
		
		/**
		 * Results 
		 */
		IsSearchableManager.addWidget(new AjaxSolr.ResultWidget({
			id: 'result',
			target: '#docs'
		}));
		
		/**
		 * Paging 
		 */
		IsSearchableManager.addWidget(new AjaxSolr.CurrentPagerWidget({
			id: 'pager_top',
			target: '#pager_top',
			prevLabel: '&lt;',
			nextLabel: '&gt;',
			innerWindow: 1,
			renderHeader: function (perPage, offset, total) {
				$('#pager_top-header').html($('<span/>').text('displaying ' + 
					Math.min(total, offset + 1) + ' to ' + 
					Math.min(total, offset + perPage) + ' of ' + total));
			}
		}));
		IsSearchableManager.addWidget(new AjaxSolr.PagerWidget({
			id: 'pager_bottom',
			target: '#pager_bottom',
			prevLabel: '&lt;',
			nextLabel: '&gt;',
			innerWindow: 1,
			renderHeader: function (perPage, offset, total) {
				$('#pager_bottom-header').html($('<span/>').text('displaying ' + 
					Math.min(total, offset + 1) + ' to ' + 
					Math.min(total, offset + perPage) + ' of ' + total));
			}
		}));
		
		/**
		 * Tags (Facets) 
		 */
		var fields = [ 'column_texts', 'database_texts', 'creator_texts', 'type' ];
		for (var i = 0, l = fields.length; i < l; i++) {
			IsSearchableManager.addWidget(new AjaxSolr.TagcloudWidget({
				id: fields[i],
				target: '#' + fields[i],
				field: fields[i]
			}));
		}
		
		/**
		 * Current Filter Controls 
		 */
		IsSearchableManager.addWidget(new AjaxSolr.CurrentSearchWidget({
			id: 'currentsearch',
			target: '#selection'
		}));
		
		/**
		 * Meta Search Field Input
		 */
		IsSearchableManager.addWidget(new AjaxSolr.TextWidget({
			id: 'isSearchable_search_field',
			target: '#isSearchable_search'
		}));
		
		IsSearchableManager.init();
		
		IsSearchableManager.store.addByValue('q', '*:*');	
		IsSearchableManager.store.addByValue('fq', 'isSearchable_texts:true');	
		
		var params = {
			facet: true,
			'facet.field': [ 'column_texts', 'database_texts', 'creator_texts', 'type' ],
			'facet.limit': 20,
			'facet.mincount': 1,
			'f.topics.facet.limit': 50,
			'json.nl': 'map'
		};
		for (var name in params) {
			IsSearchableManager.store.addByValue(name, params[name]);
		}
		
		IsSearchableManager.doRequest();
  });
})(jQuery);


