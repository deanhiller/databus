
/**
 * Ajax Solr MANAGER 
 * 
 */
var GlobalManager;
(function ($) {
	$(function () {
		GlobalManager = new AjaxSolr.Manager({
			/**
			 * Important:
			 * 
			 *	Had to modify AbstractManager.js and change the
			 *  	servlet variable to an empty string.
			 *  		servlet: '',
			 */
			solrUrl: _solrProtocol + '://' + window.location.host + '/api/search/' + _globalSearchID
		});
		
		/**
		 * Results 
		 */
		GlobalManager.addWidget(new AjaxSolr.ResultWidget({
			id: 'result',
			target: '#docs'
		}));
		
		/**
		 * Paging 
		 */
		GlobalManager.addWidget(new AjaxSolr.CurrentPagerWidget({
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
		GlobalManager.addWidget(new AjaxSolr.PagerWidget({
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
		GlobalManager.addWidget(new AjaxSolr.TagcloudWidget({
			id: 'main_facet_widget',
			target: '#dynamic_facets'
		}));
		
		/**
		 * Current Filter Controls 
		 */
		GlobalManager.addWidget(new AjaxSolr.CurrentSearchWidget({
			id: 'currentsearch',
			target: '#selection'
		}));
		
		/**
		 * GLobal Search Field Input
		 */
		GlobalManager.addWidget(new AjaxSolr.TextWidget({
			id: 'global_search_field',
			target: '#global_search'
		}));
		
		GlobalManager.init();
		
		GlobalManager.store.addByValue('q', _globalSearchItem);
		
		var params = {
			facet: true,
			'facet.field': [],
			'facet.limit': 20,
			'facet.mincount': 1,
			'f.topics.facet.limit': 50,
			'json.nl': 'map'
		};
		
		for (var name in params) {
			GlobalManager.store.addByValue(name, params[name]);
		}
		
		if(_results === 'false') {
		
		} else {
			GlobalManager.doRequest();
		}
  });
})(jQuery);


