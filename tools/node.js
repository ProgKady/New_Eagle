
//*** Used for Node Native apps and 'extended' node mode ****

_isNode = true;
_bridge = null;
window = global;
document = function() {}

alert = function( msg ) { __app.execute("#","App.Alert(\f"+msg+"\f\f\f" ), false }
_nodeDir = __app.execute("#","App.GetPrivateFolder(\fnode\f", true );
_RealPath = function(path) { return __app.execute("#","App.RealPath(\f"+path,true) }
_require = globalThis.require
globalThis.require = function(url){ return _require( url.includes(".js") ? _RealPath(url) : url ) }
//_LoadScriptSync = function( url ) { require(url.replace("/Sys",_nodeDir)) }
_Batch = function( ctrl,vals ) { for( var v in vals ) ctrl[v].apply( global, vals[v] ) }
app.Script = null

parent.Execute = function( js ) { __app.execute( "#", "App.Execute("+js ); }
parent.Func = function( name, args ) {
    for( var a in arguments )
        if( typeof arguments[a]=='function' ) arguments[a] = _Cbm(arguments[a])
    parent.Execute( "_OnFunc('"+btoa2(JSON.stringify(Array.from(arguments)))+"')", function(){} )
}

//Install a node module.
_installModule = function( name, path )
{
    console.log('Installing '+name+' to '+path+"...")
    const { PluginManager } = _require('live-plugin-manager')
    const manager = new PluginManager( { cwd: path, pluginsPath: path+"/node_modules",
            /*npmInstallMode: "noCache",*/ hostRequire: null } )
    //process.chdir( path+"/node_modules" );
    //__mainFile = path+"/node_modules"

    async function install() {
        var n = name.lastIndexOf("@")
        var mod = n>1 ? name.substring(0,n) : name
        var ver = n>1 ? name.substring(n+1) : null
        await manager.install( mod, ver )
        console.log( name+' installed!')
        console.log( "_DONE_" )
    }
    install()
}

// Pure js implementation of atob() and btoa()
if( true )
{
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

//Trap errors using try/catch if app.SetOnError is used.
//(Should be called outside OnStart function)
app.SetOnError = function( callback )
{
    app._onError = callback;

    _CbTry = function(cb) {
        try { cb() }
        catch(e){
            if( app._onError ) {
                var err=_parseError(e.stack)
                app._onError( err.msg, err.line, err.file )
            }
        }
    }

    _Call = function( id, func, params ) {
        app.eventSource=_map[id]
        if( app._onError ) {
            try { if( func ) func.apply( _map[id], params ) }
            catch(e){
                var err=_parseError(e.stack)
                app._onError( err.msg, err.line, err.file )
            }
        }
        else if( func ) func.apply( _map[id], params )
    }

    if( !app._onStart ) app._onStart = OnStart
    OnStart = function() {
        if( app._onError ) _CbTry( app._onStart )
        else app._onStart()
    }

    if( !app._setTimeout ) app._setTimeout = setTimeout
    setTimeout = function(cb,ms,...args) {
        if( app._onError ) app._setTimeout( ()=>{_CbTry(cb)}, ms, ...args )
        else app._setTimeout(cb,ms,...args)
    }

    if( !app._setInterval ) app._setInterval = setInterval
    setInterval = function(cb,ms,...args) {
        if( app._onError ) app._setInterval( ()=>{_CbTry(cb)}, ms, ...args )
        else app._setInterval(cb,ms,...args)
    }
}

