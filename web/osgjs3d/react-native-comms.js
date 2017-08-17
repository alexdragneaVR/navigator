var ReactNativeComms = {
  _postMessage : function(m){
    var p = window.postMessage || function(message){
      console.info("DEBUG MODE", message);
    };

    p(m);
  },
  /**
   * Attaches a message handler that is called when a React Native message is received
   *    fn - the message handler
   *    [ctx] - optional context object (i.e the 'this' of the fn)
   */
  addReactNativeEventListener : function(fn,ctx){
    document.addEventListener("message", function(e){
      try{
        fn.call(ctx, JSON.parse(e.data));
      }catch(e){
        fn.call(ctx, null, e);
      }
    });
  },
  /**
   * Send a message to react native. message can be any valid json value.
   *    data - any valid json value except functions
   */
  postToReactNative : function(data){
    var value = JSON.stringify({data : data});
    try{
      this._postMessage(value);
    }catch(e){
      console.error("Error posting to React Native");
      console.trace(e);
    }
  }
};
