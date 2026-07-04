
//*** Used for node apps without 'extended' mode ****

_isNode = true;
_bridge = null;
window = global;
document = function() {}

parent.Execute = function( js ) { __app.execute( "#", "App.Execute("+js ); }
_OnFunc = function( json ) { var args=JSON.parse(atob2(json)); return eval(args[0]).apply(window,args.splice(1)) }

parent.Func = function( name, args ) {
    for( var a in arguments )
        if( typeof arguments[a]=='function' ) arguments[a] = _Cbm(arguments[a])
    parent.Execute( "_OnFunc('"+btoa2(JSON.stringify(Array.from(arguments)))+"')", function(){} )
}

//Auto-binding class (also in app.js)
Bound = class {
    constructor() {
        this.bindMethods(this);
    }
    bindMethods( ctx ) {
        var meths = Object.getOwnPropertyNames(Object.getPrototypeOf(ctx))
        for (var m in meths) {
            var f = ctx[meths[m]];
            ctx[meths[m]] = f.bind(ctx);
            f._ctx = ctx; f._nohash = true; //<-- for DS callbacks.
        }
    }
}

// Pure js implementation of atob() and btoa()
if( true )
{
    //Improved versions of atob and btoa.
    atob2 = function(str) {
       return decodeURIComponent(atob(str).split('').map(function(c) {
           return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
       }).join(''));
    }

    btoa2 = function(str) {
       return btoa(encodeURIComponent(str).replace(/%([0-9A-F]{2})/g,
           function toSolidBytes(match, p1) {
               return String.fromCharCode('0x' + p1);
       }));
    }

    var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';

    function InvalidCharacterError(message) {
        this.message = message;
    }
    InvalidCharacterError.prototype = new Error;
    InvalidCharacterError.prototype.name = 'InvalidCharacterError';

    //btoa || (
    btoa = function (input) {
        var str = String(input);
        for (
          // initialize result and counter
          var block, charCode, idx = 0, map = chars, output = '';
          // if the next str index does not exist:
          //   change the mapping table to "="
          //   check if d has no fractional digits
          str.charAt(idx | 0) || (map = '=', idx % 1);
          // "8 - idx % 1 * 8" generates the sequence 2, 4, 6, 8
          output += map.charAt(63 & block >> 8 - idx % 1 * 8)
        ) {
          charCode = str.charCodeAt(idx += 3/4);
          if (charCode > 0xFF) {
            throw new InvalidCharacterError("'btoa' failed: The string to be encoded contains characters outside of the Latin1 range.");
          }
          block = block << 8 | charCode;
        }
        return output;
    }//);

    // decoder
    //atob || (
    atob = function (input) {
        var str = String(input).replace(/=+$/, '');
        if (str.length % 4 == 1) {
          throw new InvalidCharacterError("'atob' failed: The string to be decoded is not correctly encoded.");
        }
        for (
          // initialize result and counters
          var bc = 0, bs, buffer, idx = 0, output = '';
          // get next character
          buffer = str.charAt(idx++);
          // character found in table? initialize bit storage and add its ascii value;
          ~buffer && (bs = bc % 4 ? bs * 64 + buffer : buffer,
            // and if not first of each 4 characters,
            // convert the first 8 bits to one ascii character
            bc++ % 4) ? output += String.fromCharCode(255 & bs >> (-2 * bc & 6)) : 0
        ) {
          // try to find character in table (0-63, not found => -1)
          buffer = chars.indexOf(buffer);
        }
        return output;
    }//);

}
