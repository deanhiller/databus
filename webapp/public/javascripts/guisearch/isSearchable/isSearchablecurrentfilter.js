(function ($) {
	AjaxSolr.CurrentSearchWidget = AjaxSolr.AbstractWidget.extend({
		start: 0,
		
		afterRequest: function () {
			var self = this;
			var links = [];
			
			var q = this.manager.store.get('q').val();
			if (q != '*:*') {
				var link = $('<a href="#"/>').text(' ' + q).click(function () {
					self.manager.store.get('q').val('*:*');
					self.doRequest();
					return false;
				});
				link.prepend($('<i class="icon-ban-circle" style="margin-top: 2px;"></i>'));				
				links.push(link);
			}
			
			var fq = this.manager.store.values('fq');
			for (var i = 0, l = fq.length; i < l; i++) {
				var value = fq[i];
				var newValue = value;
				
				if(value.indexOf("isSearchable_texts") != -1) {
					continue;
				} else if(value.indexOf("column_texts") != -1) {
					newValue = value.replace("column_texts:", "Table Data: ");
				} else if(value.indexOf("database_texts") != -1) {
					newValue = value.replace("database_texts:", "Database: ");
				} else if(value.indexOf("creator_texts") != -1) {
					newValue = value.replace("creator_texts:", "Users: ");
				}
				
				var link = $('<a href="#"/>').text(' ' + newValue).click(self.removeFacet(fq[i]));
				link.prepend($('<i class="icon-ban-circle" style="margin-top: 2px;"></i>'));
				
				links.push(link);
			}
			
			if (links.length > 1) {
				var link = $('<a href="#"/>').text('remove all').click(function () {
					self.manager.store.get('q').val('*:*');
					self.manager.store.remove('fq');
					self.doRequest();
					return false;
				});
				link.prepend($('<i class="icon-remove" style="margin-top: 2px; margin-right: 4px;"></i>'));
				
				links.unshift(link);
			}
		},
		
		removeFacet: function (facet) {
			var self = this;
			
			return function () {
				if (self.manager.store.removeByValue('fq', facet)) {
					self.doRequest();
				}
				
				return false;
			};
		}
	});
})(jQuery);