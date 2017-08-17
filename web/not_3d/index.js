'use strict';

var main = function() {
  ReactNativeComms.addReactNativeEventListener(function(e){
    document.getElementById("content").innerHTML = e.data["large-string"];
  });
};

window.addEventListener('load', main, true);
