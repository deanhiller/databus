function show(id) {
	$("#" + id).slideDown('fast');
}
function hide(id) {
	$("#" + id).slideUp('fast');
}
// ----------------------------------------------------------- SCROLL WINDOW -- //
function scrollWin(id){
	$('html, body').animate({
		scrollTop: $("#" + id).offset().top
	}, 500);
}
// function showTip() {
	// $('.hastip').tooltipsy({
		// className: 'bubbletooltip_tip',
		// offset: [10, 0],
		// show: function (e, $el) {
			// $el.fadeIn(100);
		// },
		// hide: function (e, $el) {
			// $el.fadeOut(300);
		// }
	// });
// }
function showTip(elem) {	
	$(elem).find('div').fadeIn('fast');
}

function hideTip(elem) {
	t = setTimeout(function(){ 
		$(elem).find('div').fadeOut('fast');
	}, 200);
}



