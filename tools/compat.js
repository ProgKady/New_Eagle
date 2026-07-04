

//Add basic compatibility with EnjineIO.

$include = (file) => app.Script(file)

//File handling
const $file = {};
$file.exists = (path) => app.FileExists(path)
$file.read = (path,encoding) => app.ReadFile(path,encoding)
$file.write = (path,data,options) => app.WriteFile(path,data,options)

//Folder handling.
const $dir = {};
$dir.getPrivate = (name) => app.GetPrivateFolder(name)
$dir.exists = (path) => app.IsFolder(path)

//Reserved for later.
const $sys = {}
const $net = {}
const $android = {}
const $ios = {}
const $windows = {}
const $osx = {}

//Temporary hack for app.GetPrivateFolder call in ui.js (obfuscation issue)
_Private = function( name,options ) { return prompt( "#", "App.GetPrivateFolder(\f"+name+"\f"+options ); }
