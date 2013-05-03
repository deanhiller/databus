
/**
 * Ajax Solr MANAGER 
 * 
 */
var DetailManager;
(function ($) {
	$(function () {
		DetailManager = new AjaxSolr.Manager({
			/**
			 * Important:
			 * 
			 *	Had to modify AbstractManager.js and change the
			 *  	servlet variable to an empty string.
			 *  		servlet: '',
			 */
			solrUrl: _solrProtocol + '://' + window.location.host + '/api/search/' + _searchTable
		});
		
		/**
		 * Results 
		 */
		DetailManager.addWidget(new AjaxSolr.ResultWidget({
			id: 'result',
			target: '#docs'
		}));
		
		/**
		 * Paging 
		 */
		DetailManager.addWidget(new AjaxSolr.CurrentPagerWidget({
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
		DetailManager.addWidget(new AjaxSolr.PagerWidget({
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
		 *
		 *  Since we do not know what facets can be returned, we will create
		 *  an empty facet object and then handle the facets dynamically in
		 *  the afterRequest method.
		 */
		DetailManager.addWidget(new AjaxSolr.TagcloudWidget({
			id: 'main_facet_widget',
			target: '#dynamic_facets'
		}));
		
		/**
		 * Current Filter Controls 
		 */
		DetailManager.addWidget(new AjaxSolr.CurrentSearchWidget({
			id: 'currentsearch',
			target: '#selection'
		}));
		
		/**
		 * GLobal Search Field Input
		 */
		DetailManager.addWidget(new AjaxSolr.TextWidget({
			id: 'detail_search_field',
			target: '#detail_search'
		}));
		
		DetailManager.init();
		
		DetailManager.store.addByValue('q', '*:*');
		
		var params = {
			facet: true,
			'facet.field': [],
			'facet.limit': 20,
			'facet.mincount': 1,
			'f.topics.facet.limit': 50,
			'json.nl': 'map'
		};
		
		for (var name in params) {
			DetailManager.store.addByValue(name, params[name]);
		}
		
		DetailManager.doRequest();
  });
})(jQuery);


