package org.rssowl.core.util;

import org.rssowl.core.connection.FacebookAuthentication;


public class FacebookLikeUtil {
  public static String getButtonCode(String url)
  {
/*Load FB SDK Normally */

    String buttonCode = "<div id='fb-root'>" + //$NON-NLS-1$
    "<script src='http://connect.facebook.net/en_US/all.js'></script>" //$NON-NLS-1$
    +"<script>"//$NON-NLS-1$
    +"  FB.init({"//$NON-NLS-1$
    +"  appId  : '"+FacebookAuthentication.APPLICATION_ID+"',"//$NON-NLS-1$ //$NON-NLS-2$
    +"  status : true, // check login status"//$NON-NLS-1$
    +"  cookie : true, // enable cookies to allow the server to access the session"//$NON-NLS-1$
    +"  xfbml  : true  // parse XFBML"//$NON-NLS-1$
    +"  });"//$NON-NLS-1$
    +"FB.login(function(response) {" //$NON-NLS-1$
    +"if (response.session) {" //$NON-NLS-1$
    +"} else {" //$NON-NLS-1$
    +" } " //$NON-NLS-1$
    +"});" //$NON-NLS-1$
    +"</script></div>"; //$NON-NLS-1$



    /*Load Fb SDK Async*/ /*
    String buttonCode =   "<div id='fb-root'></div>" //$NON-NLS-1$
      +"<script>"//$NON-NLS-1$
      +"      window.fbAsyncInit = function() {"//$NON-NLS-1$
      +"  FB.init({appId: '"+FacebookAuthentication.APPLICATION_ID+"', status: true, cookie: true,"//$NON-NLS-1$ //$NON-NLS-2$
      +"           xfbml: true});"//$NON-NLS-1$
      +" };"//$NON-NLS-1$
      +" (function() {"//$NON-NLS-1$
      +"   var e = document.createElement('script'); e.async = true;"//$NON-NLS-1$
      +"   e.src = document.location.protocol +"//$NON-NLS-1$
      +"     '//connect.facebook.net/en_US/all.js';"//$NON-NLS-1$
      +"   document.getElementById('fb-root').appendChild(e);"//$NON-NLS-1$
      +" }());"//$NON-NLS-1$
      +"</script>";//$NON-NLS-1$*/

    /* XFMBL
    buttonCode +="<div><script src='http://connect.facebook.net/en_US/all.js#xfbml=1'>" + //$NON-NLS-1$
    "</script>" + //$NON-NLS-1$
    "<fb:like href='"+url+"' " + //$NON-NLS-1$ //$NON-NLS-2$
    "show_faces='true' " + //$NON-NLS-1$
    "width='450' " + //$NON-NLS-1$
    "font='verdana'>" + //$NON-NLS-1$
    "</fb:like>"; //$NON-NLS-1$*/

    /* IFRAME */
    buttonCode += " <hr /> <iframe src='http://www.facebook.com/plugins/like.php?" + //$NON-NLS-1$
    "href="+url+"&amp;" +  //$NON-NLS-1$ //$NON-NLS-2$
    "layout=standard&amp;" +  //$NON-NLS-1$
    "show_faces=true&amp;" + //$NON-NLS-1$
    "width=600&amp;" + //$NON-NLS-1$
    "action=like&amp;" + //$NON-NLS-1$
    "font=verdana&amp;" + //$NON-NLS-1$
    "colorscheme=light&amp;" + //$NON-NLS-1$
    "height=80' " + //$NON-NLS-1$
    "scrolling='no' " + //$NON-NLS-1$
    "frameborder='0' " + //$NON-NLS-1$
    "style='border:none; " + //$NON-NLS-1$
    "overflow:hidden; " + //$NON-NLS-1$
    "width:600px; " + //$NON-NLS-1$
    "height:80px;' " + //$NON-NLS-1$
    "allowTransparency='true'>" + //$NON-NLS-1$
    "</iframe> "; //$NON-NLS-1$

    return buttonCode;
  }
}
