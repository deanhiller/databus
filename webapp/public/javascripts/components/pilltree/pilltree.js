/* http://jsfiddle.net/fXzHS/43/ */

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
        
    });
});

/*
 * HTML USAGE:
 *  <!-- We will create a family pilltree using just CSS(3) The markup will be simple nested lists -->
 *  <div class="pilltree">
 *      <ul>
 *          <li>	<a href="#">Parent</a>
 *  
 *              <ul>
 *                  <li>	<a href="#">Child</a>
 *  
 *                      <ul>
 *                          <li>	<a href="#">Grand Child</a>
 *  
 *                          </li>
 *                      </ul>
 *                  </li>
 *                  <li>	<a href="#">Child</a>
 *  
 *                      <ul>
 *                          <li><a href="#">Grand Child</a>
 *                          </li>
 *                          <li>	<a href="#">Grand Child</a>
 *  
 *                              <ul>
 *                                  <li>	<a href="#">Great Grand Child</a>
 *  
 *                                  </li>
 *                                  <li>	<a href="#">Great Grand Child</a>
 *  
 *                                  </li>
 *                                  <li>	<a href="#">Great Grand Child</a>
 *  
 *                                  </li>
 *                              </ul>
 *                          </li>
 *                          <li><a href="#">Grand Child</a>
 *                          </li>
 *                      </ul>
 *                  </li>
 *              </ul>
 *          </li>
 *      </ul>
 *  </div>
 */
 
 /*
  * CSS:
  *  	.pilltree li {
  *  	    margin: 0px 0;
  *  	    list-style-type: none;
  *  	    position: relative;
  *  	    padding: 20px 5px 0px 5px;
  *  	}
  *  	.pilltree li::before {
  *  	    content:'';
  *  	    position: absolute;
  *  	    top: 0;
  *  	    width: 1px;
  *  	    height: 100%;
  *  	    right: auto;
  *  	    left: -20px;
  *  	    border-left: 1px solid #ccc;
  *  	    bottom: 50px;
  *  	}
  *  	.pilltree li::after {
  *  	    content:'';
  *  	    position: absolute;
  *  	    top: 30px;
  *  	    width: 25px;
  *  	    height: 20px;
  *  	    right: auto;
  *  	    left: -20px;
  *  	    border-top: 1px solid #ccc;
  *  	}
  *  	.pilltree li a {
  *  	    display: inline-block;
  *  	    border: 1px solid #ccc;
  *  	    padding: 5px 10px;
  *  	    text-decoration: none;
  *  	    color: #666;
  *  	    font-family: arial, verdana, tahoma;
  *  	    font-size: 11px;
  *  	    border-radius: 5px;
  *  	    -webkit-border-radius: 5px;
  *  	    -moz-border-radius: 5px;
  *  	}
  *  	//Remove connectors before root//
  *  	 .pilltree > ul > li::before, .pilltree > ul > li::after {
  *  	    border: 0;
  *  	}
  *  	//Remove connectors after last child//
  *  	 .pilltree li:last-child::before {
  *  	    height: 30px;
  *  	}
  *  	//Time for some hover effects//
  *  	
  *  	//We will apply the hover effect the the lineage of the element also//
  *  	 .pilltree li a:hover, .pilltree li a:hover+ul li a {
  *  	    background: #c8e4f8;
  *  	    color: #000;
  *  	    border: 1px solid #94a0b4;
  *  	}
  *  	//Connector styles on hover//
  *  	 .pilltree li a:hover+ul li::after, .pilltree li a:hover+ul li::before, .pilltree li a:hover+ul::before, .pilltree li a:hover+ul ul::before {
  *  	    border-color: #94a0b4;
  *  	}
  * 		
  * 		.pilltree_selected {
  * 		    background: #c8e4f8;
  * 		    color: #000;  * 		    
  * 		    border-radius: 5px;
  * 		    -webkit-border-radius: 5px;
  * 		    -moz-border-radius: 5px;
  * 		}
  * 
  *  
  */
 
 
 
 
 