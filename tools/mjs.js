
var _mod = null

OnStart = async function() {
     try {
        _mod = await import( __mainFile )
        _mod.OnStart()
    }
    catch(e) { _error(e) }
}

OnBack = function() {
    if( _mod && _mod.OnBack ) _mod.OnBack()
}

OnPause = function() {
    if( _mod && _mod.OnPause ) _mod.OnPause()
}

OnResume = function() {
    if( _mod && _mod.OnResume ) _mod.OnResume()
}

OnConfig = function() {
    if( _mod && _mod.OnConfig ) _mod.OnConfig()
}

OnData = function() {
    if( _mod && _mod.OnData ) _mod.OnData()
}
