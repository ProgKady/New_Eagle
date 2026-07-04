

//Extract error information from error stack (ie. from e.stack).
/*
var s = "ReferenceError: sdfsf is not defined\n" +
        "at clean (/storage/emulated/0/DroidScript/aaaCleaner/utils.js:8:5\n)" +
        "at Obj.btn...)"

var s = `/storage/emulated/0/DroidScript/aaaCleaner/utils.js:8
        var glob = require( "glob" )
        ^^^

    SyntaxError: Unexpected token 'var'
        at wrapSafe (internal/modules/cjs/loader.js:931:16)
        at Module._compile (internal/modules/cjs/loader.js:979:27)
        `
var s = `lay is not defined
        at Module.OnStart (file:///storage/emulated/0/DroidScript/ES6%20modules/ES6%20modules.mjs:39:9)
        at OnStart (/data/data/com.smartphoneremote.androidscriptfree/app_node/mjs.js:6:10)
`
alert( JSON.stringify(_parseError(s)) )
*/

global._parseError = function( errorStack )
{
    var ret = { msg:"error", file:"", line:-1 }
    try {
        console.log( unescape(errorStack) )
        var lines = errorStack.split('\n')
        var syntErr = lines[2].includes("^^^")
        var refErr = lines[1].includes("is not defined")

        ret.msg = syntErr ? lines[4] : lines[0]
        var src = (syntErr ? lines[0] : lines[1]).replace("file://","").split(":")
        ret.file = unescape(src[0].split("/").slice(-1)[0])
        if( __mainFile && __mainFile.includes(ret.file) ) ret.file = ret.file.replace(".mjs",".js")
        ret.line = parseInt(src[1])
    }
    catch(e){}
    return ret
}

