
_isV8 = true;

if( typeof window=="undefined" )
{
  window = (function() {return this;}());
  global = window
  
  var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';

  function InvalidCharacterError(message) {
    this.message = message;
  }
  InvalidCharacterError.prototype = new Error;
  InvalidCharacterError.prototype.name = 'InvalidCharacterError';
}

// window.btoa (base64 encode function) ?
(function checkWindowBtoaCompatibility() {
  if ('btoa' in window) {
    return;
  }
  var digits = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
  window.btoa = function windowBtoa(chars) {
    var buffer = '';
    var i, n;
    for (i = 0, n = chars.length; i < n; i += 3) {
      var b1 = chars.charCodeAt(i) & 0xFF;
      var b2 = chars.charCodeAt(i + 1) & 0xFF;
      var b3 = chars.charCodeAt(i + 2) & 0xFF;
      var d1 = b1 >> 2, d2 = ((b1 & 3) << 4) | (b2 >> 4);
      var d3 = i + 1 < n ? ((b2 & 0xF) << 2) | (b3 >> 6) : 64;
      var d4 = i + 2 < n ? (b3 & 0x3F) : 64;
      buffer += (digits.charAt(d1) + digits.charAt(d2) +
                 digits.charAt(d3) + digits.charAt(d4));
    }
    return buffer;
  };
})();

// window.atob (base64 encode function)?
(function checkWindowAtobCompatibility() {
  if ('atob' in window) {
    return;
  }
  // https://github.com/davidchambers/Base64.js
  var digits = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
  window.atob = function (input) {
    input = input.replace(/=+$/, '');
    if (input.length % 4 === 1) {
      throw new Error('bad atob input');
    }
    for (
      // initialize result and counters
      var bc = 0, bs, buffer, idx = 0, output = '';
      // get next character
      buffer = input.charAt(idx++);
      // character found in table?
      // initialize bit storage and add its ascii value
      ~buffer && (bs = bc % 4 ? bs * 64 + buffer : buffer,
        // and if not first of each 4 characters,
        // convert the first 8 bits to one ascii character
        bc++ % 4) ? output += String.fromCharCode(255 & bs >> (-2 * bc & 6)) : 0
    ) {
      // try to find character in table (0-63, not found => -1)
      buffer = digits.indexOf(buffer);
    }
    return output;
  };
})();

//Our own requestAnimationFrame (defaults to 60fps)
(function() {
    var lastTime = 0;
   
	//if (!window.requestAnimationFrame)
	window.requestAnimationFrame = function(callback, element) {
		var currTime = new Date().getTime();
		var timeToCall = Math.max(0, (window._fps?1000/window._fps:16) - (currTime - lastTime));
		var id = setTimeout(function() { callback(currTime + timeToCall); }, timeToCall);
		lastTime = currTime + timeToCall;
		return id;
	};

	// if (!window.cancelAnimationFrame)
	window.cancelAnimationFrame = function(id) {
		clearTimeout(id);
	};
}());

//XMLHttpRequest polyfill.
{
    var replaceAll = function( sString,sSearch,sReplace ) {
        return sString.split( sSearch ).join( sReplace );
    }
    XMLHttpRequest = function() {
        var self = this;
        var m_cb = null;
        var m_mode = "";
        var m_url = "";
        var m_headers = "";
        this.readyState = 0;
        this.status = 0;
        this.onreadystatechange = null;
        this.responseText = "";
        this.setRequestHeader = function( name,val ) {
            m_headers += ( m_headers ? "|" : "" ) + ( name + "~" + val );
        };
        this.open = function( mode,url ) {
            m_mode = mode.toLowerCase();
            m_url = url
        };
        this.send = function( params ) {
            if( params ) {
                if( params.includes( "&" ) ) params = replaceAll( params,"&","|" );
                else m_mode = "json";
            }
            app.HttpRequest( m_mode,m_url,"",params,( error,response,status ) => {
                    self.readyState = 4;
                    self.status = status;
                    self.responseText = response;
                    if( self.onreadystatechange ) self.onreadystatechange( self );
                }
            ,m_headers );
        };
    }
    window.XMLHttpRequest = XMLHttpRequest;
}
