*{ Use this tag if you want a part of the page to show when logged in so when he is logged out, that part of the page will not show }*

#{if session.username && controllers.Secure.Security.invoke("check", _arg)}
    #{doBody /}
#{/if}