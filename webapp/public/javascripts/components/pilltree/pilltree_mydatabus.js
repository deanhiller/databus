$(function () {
    $('.pilltree li').hide();
    $('.pilltree li:first').show();
    $('.pilltree li').on('click', function (e) {        
        var children = $(this).find('> ul > li');
        children.find('a').removeClass();
        if (children.is(":visible")) {
            children.hide('fast');            
        }
        else {
            children.show('fast');
        }        
        e.stopPropagation();
    });
    
    $('.pilltree a').on('click', function (e) {
        var parents = $(this).parents('li');
        var siblings = parents.find('> ul > li');
        siblings.find('a').removeClass();
        
        $(this).parents('li').find('> a').addClass('pilltree_selected label-info');
        
        // Change the Databases tab CSS      
        if($.inArray('pilltree_selected', $('#database_main_parent').classes()) > -1) {
        	$('#database_main_parent').addClass('pilltree_database_main_parent');
        }
        
        // Now match the database subcategories to their respected CSS        
        var parentAnchors = parents.find('> a');
        for (var i = 0; i < parentAnchors.length; ++i) {
        	var anchor = parentAnchors[i];        	
        	var anchorId = anchor.id;
        	
		if (anchorId != undefined) {
			if(anchorId.indexOf("_admin") >= 0) {
				$('#' + anchorId).addClass('pilltree_database_admin');
			}
			
			if(anchorId.indexOf("_readwrite") >= 0) {
				$('#' + anchorId).addClass('pilltree_database_readwrite');
			}
			
			if(anchorId.indexOf("_readonly") >= 0) {
				$('#' + anchorId).addClass('pilltree_database_readonly');
			}
			
			if(anchorId.indexOf("_system") >= 0) {
				$('#' + anchorId).addClass('pilltree_database_system');
			}
			
			if(anchorId.indexOf('_db') >= 0) {
	        		$('#' + anchorId).addClass('pilltree_database_chosen');
	        	}
		}
        }
    });
});


/**
 * 
 * The following method returns ALL classes associated with a given element as
 * an array.
 *  
 * Usage:		$('#ID').classes()
 */
;!(function ($) {
    $.fn.classes = function (callback) {
        var classes = [];
        $.each(this, function (i, v) {
            var splitClassName = v.className.split(/\s+/);
            for (var j in splitClassName) {
                var className = splitClassName[j];
                if (-1 === classes.indexOf(className)) {
                    classes.push(className);
                }
            }
        });
        if ('function' === typeof callback) {
            for (var i in classes) {
                callback(classes[i]);
            }
        }
        return classes;
    };
})(jQuery);