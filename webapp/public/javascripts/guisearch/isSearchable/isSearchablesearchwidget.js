/**
 * WIDGETS
 *  
 */
(function ($) {

AjaxSolr.ResultWidget = AjaxSolr.AbstractWidget.extend({
  start: 0,

  beforeRequest: function () {
    $(this.target).html($('<img/>').attr('src', '/public/images/ajax-loader.gif'));
  },

  facetLinks: function (facet_field, facet_values) {
    var links = [];
    if (facet_values) {
      for (var i = 0, l = facet_values.length; i < l; i++) {
        if (facet_values[i] !== undefined) {
          links.push(AjaxSolr.theme('facet_link', facet_values[i], this.facetHandler(facet_field, facet_values[i])));
        }
        else {
          links.push(AjaxSolr.theme('no_items_found'));
        }
      }
    }
    return links;
  },

  facetHandler: function (facet_field, facet_value) {
    var self = this;
    return function () {
      self.manager.store.remove('fq');
      self.manager.store.addByValue('fq', facet_field + ':' + AjaxSolr.Parameter.escapeValue(facet_value));
      self.doRequest();
      return false;
    };
  },

/**
 * 
 * We have no absolute search terms for this
 */
  afterRequest: function () {  
    $(this.target).empty();
    for (var i = 0, l = this.manager.response.response.docs.length; i < l; i++) {
      var doc = this.manager.response.response.docs[i];
      
      $(this.target).append(AjaxSolr.theme('result', doc, AjaxSolr.theme('snippet', doc)));
    }
    
    /**
	* Now add all the table links
     */
    for (var i = 0, l = this.manager.response.response.docs.length; i < l; i++) {
    		var doc = this.manager.response.response.docs[i];
    		if(doc.type == 'table') {
    			
    			/**
    			 * See if this table has isSearchable_texts with an element of true 
    			 */
    			if (doc.hasOwnProperty("isSearchable_texts")) {
    				if(doc.isSearchable_texts[0] === 'true') {
					var docID = doc.id;
					
						/**
					 * Declare an anonymous function so that the docID is not overwritten
					 * for all members in the for-loop w/ the LAST item in the LAST index 
					 * http://stackoverflow.com/questions/9105731/jquery-variable-overwrite-in-loop-want-to-retain-different-values 
					 */
					(function(docID){
						var tableId = "#table_" + docID;
						
						$(tableId).click(function(){
							// Clear the menu items
							$( "li" ).each(function() {
								if($(this).hasClass("db_main_nav_item")) {
									$(this).removeClass("active");
								}
							});
						
							var url = _detailURL + '?searchTable=' + docID;
							
							// TEMP UNTIL I CAN FIX							
							//$("#content").empty();
							//$.get(url, function(data) {
							//	$('#content').html(data);
							//});
						});			
					})(docID);
				}
			}
		}
    }
  },

  init: function () {
    $('a.more').livequery(function () {
      $(this).toggle(function () {
        $(this).parent().find('span').show();
        $(this).text('less');
        return false;
      }, function () {
        $(this).parent().find('span').hide();
        $(this).text('more');
        return false;
      });
    });
  }
});

})(jQuery);

