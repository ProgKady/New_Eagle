(function(win) {
"use strict";
var _nextId = 1, _map = {}, _started = false, _cbId = 0, _cbMap = {};
var _root = document.getElementById('app') || document.body;
var _pendingHtml = [];

function _id() { return _nextId++; }

function _Cbm(fn) { if(typeof fn!='function') return ''; var id = 'cb'+(++_cbId); _cbMap[id]=fn; return id; }

function _execCb(id,val) { var fn=_cbMap[id]; if(fn) { delete _cbMap[id]; fn(val); } }

function _el( tag, cls, parent ) {
    var e = document.createElement(tag);
    if(cls) e.className = 'ds-'+cls;
    if(parent && parent._el) parent._el.appendChild(e);
    else if(parent && parent.appendChild) parent.appendChild(e);
    return e;
}

// ---- Obj base class (all UI objects) ----
var Obj = function( typeId, tag, cls ) {
    this.id = _id();
    this.typeId = typeId;
    this.data = {};
    this._el = _el(tag||'div', cls||typeId);
    this._el.dsObj = this;
    this._children = [];
    _map[this.id] = this;
};
Obj.prototype = {
    GetType: function() { return this.typeId; },
    SetVisibility: function(mode) {
        this._el.style.display = mode==='Hide'?'none':mode==='Gone'?'none':'';
    },
    GetVisibility: function() { return this._el.style.display==='none'?'Hide':'Show'; },
    Show: function() { this._el.style.display = ''; },
    Hide: function() { this._el.style.display = 'none'; },
    SetEnabled: function(e) { this._el.style.pointerEvents = e?'':'none'; this._el.style.opacity = e?'1':'0.4'; },
    IsEnabled: function() { return this._el.style.pointerEvents!=='none'; },
    SetPadding: function(l,t,r,b) {
        this._el.style.padding = (t||0)+'px '+(r||0)+'px '+(b||0)+'px '+(l||0)+'px';
    },
    SetMargins: function(l,t,r,b) {
        this._el.style.margin = (t||0)+'px '+(r||0)+'px '+(b||0)+'px '+(l||0)+'px';
    },
    SetPosition: function(l,t,w,h) {
        if(w!==undefined&&w!==null&&w!=='') this._el.style.width = typeof w==='number'?w+'px':w;
        if(h!==undefined&&h!==null&&h!=='') this._el.style.height = typeof h==='number'?h+'px':h;
        if(l!==undefined&&l!==null&&l!=='') this._el.style.left = typeof l==='number'?l+'px':l;
        if(t!==undefined&&t!==null&&t!=='') this._el.style.top = typeof t==='number'?t+'px':t;
    },
    SetSize: function(w,h) {
        if(w!==undefined&&w!==null&&w!=='') this._el.style.width = typeof w==='number'?w+'px':w;
        if(h!==undefined&&h!==null&&h!=='') this._el.style.height = typeof h==='number'?h+'px':h;
    },
    GetWidth: function() { return this._el.offsetWidth; },
    GetHeight: function() { return this._el.offsetHeight; },
    SetBackColor: function(clr) { this._el.style.backgroundColor = clr; },
    SetBackAlpha: function(a) { this._el.style.opacity = a; },
    SetCornerRadius: function(r) { this._el.style.borderRadius = r+'px'; },
    Focus: function() { this._el.focus(); },
    Destroy: function() {
        if(this._el && this._el.parentNode) this._el.parentNode.removeChild(this._el);
        delete _map[this.id];
    },
    Release: function() { this.Destroy(); }
};

// ---- Layout ----
var Lay = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'Layout','div','Layout');
    this._el.style.display = 'flex';
    this._el.style.flexDirection = 'column';
    this._el.style.position = 'relative';
};
Lay.prototype = Object.create(Obj.prototype);
Lay.prototype.SetOrientation = function(o) {
    this._el.style.flexDirection = o==='Horizontal'||o==='Row'?'row':'column';
};
Lay.prototype.SetGravity = function(g) {
    if(!g) return;
    var parts = g.toLowerCase().split(',');
    var justify = '', align = '';
    parts.forEach(function(p) {
        if(p==='left'||p==='start') align='flex-start';
        else if(p==='right'||p==='end') align='flex-end';
        else if(p==='center'||p==='middle') { if(!align) align='center'; justify='center'; }
        else if(p==='top'||p==='start') justify='flex-start';
        else if(p==='bottom') justify='flex-end';
        else if(p==='fillx') align='stretch';
        else if(p==='filly') justify='stretch';
    });
    this._el.style.justifyContent = justify;
    this._el.style.alignItems = align;
};
Lay.prototype.AddChild = function(child) {
    if(child && child._el) { this._el.appendChild(child._el); child._parent=this; }
};
Lay.prototype.RemoveChild = function(child) {
    if(child && child._el && child._el.parentNode) { child._el.parentNode.removeChild(child._el); child._parent=null; }
};
Lay.prototype.SetScrollable = function(s) {
    this._el.style.overflow = s?'auto':'hidden';
};

// ---- Text ----
var Txt = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'Text','div','Text');
    this._el.style.fontSize = '16px';
    this._el.style.color = '#222';
    this._el.style.padding = '4px';
};
Txt.prototype = Object.create(Obj.prototype);
Txt.prototype.SetText = function(t) { this._el.textContent = t||''; };
Txt.prototype.GetText = function() { return this._el.textContent; };
Txt.prototype.SetTextSize = function(s) { this._el.style.fontSize = s+'px'; };
Txt.prototype.SetTextColor = function(c) { this._el.style.color = c; };
Txt.prototype.SetTextBold = function(b) { this._el.style.fontWeight = b?'bold':'normal'; };
Txt.prototype.SetTextAlignment = function(a) {
    this._el.style.textAlign = a ? a.toLowerCase() : 'left';
};

// ---- Button ----
var Btn = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'Button','button','Button');
    this._el.style.padding = '10px 20px';
    this._el.style.border = 'none';
    this._el.style.borderRadius = '6px';
    this._el.style.fontSize = '16px';
    this._el.style.cursor = 'pointer';
    this._el.style.background = '#6c5ce7';
    this._el.style.color = '#fff';
    var self = this;
    this._el.onclick = function() { if(self._onClick) self._onClick(); };
};
Btn.prototype = Object.create(Obj.prototype);
Btn.prototype.SetText = function(t) { this._el.textContent = t||''; };
Btn.prototype.GetText = function() { return this._el.textContent; };
Btn.prototype.SetOnClick = function(fn) { this._onClick = fn; };

// ---- TextEdit ----
var Txe = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'TextEdit','input','TextEdit');
    this._el.type = 'text';
    this._el.style.padding = '8px';
    this._el.style.border = '1px solid #ccc';
    this._el.style.borderRadius = '4px';
    this._el.style.fontSize = '16px';
    this._el.style.width = '100%';
};
Txe.prototype = Object.create(Obj.prototype);
Txe.prototype.SetText = function(t) { this._el.value = t||''; };
Txe.prototype.GetText = function() { return this._el.value; };
Txe.prototype.SetInputType = function(t) {
    var m = { 'text':'text','number':'number','password':'password','email':'email','phone':'tel','url':'url' };
    this._el.type = m[t]||'text';
};
Txe.prototype.SetOnChange = function(fn) { this._el.oninput = function() { fn(this.value); }; };

// ---- Image ----
var Img = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'Image','img','Image');
    this._el.style.maxWidth = '100%';
};
Img.prototype = Object.create(Obj.prototype);
Img.prototype.SetFile = function(f) { this._el.src = f; };
Img.prototype.SetSize = function(w,h) {
    if(w) this._el.style.width = typeof w==='number'?w+'px':w;
    if(h) this._el.style.height = typeof h==='number'?h+'px':h;
};

// ---- WebView ----
var Web = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'WebView','iframe','WebView');
    this._el.style.width = '100%';
    this._el.style.height = '300px';
    this._el.style.border = 'none';
};
Web.prototype = Object.create(Obj.prototype);
Web.prototype.LoadUrl = function(url) { this._el.src = url; };
Web.prototype.LoadHtml = function(html,base) {
    this._el.srcdoc = html;
};

// ---- List ----
var Lst = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'List','div','List');
    this._el.style.overflow = 'auto';
    this._el.style.border = '1px solid #ddd';
    this._el.style.borderRadius = '4px';
    this._items = [];
    this._onClick = null;
};
Lst.prototype = Object.create(Obj.prototype);
Lst.prototype.SetList = function(items) {
    this._items = items||[];
    this._el.innerHTML = '';
    var self = this;
    (items||[]).forEach(function(item,i) {
        var d = _el('div','ListItem');
        d.textContent = typeof item==='string'?item:item.Name||item.name||'';
        d.style.padding = '10px 12px';
        d.style.borderBottom = '1px solid #eee';
        d.style.cursor = 'pointer';
        d.onclick = function() { if(self._onClick) self._onClick(i,self._items[i]); };
        self._el.appendChild(d);
    });
};
Lst.prototype.GetList = function() { return this._items; };
Lst.prototype.SetOnTouch = function(fn) { this._onClick = fn; };
Lst.prototype.SetOnClick = function(fn) { this._onClick = fn; };

// ---- Switch ----
var Swi = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'Switch','label','Switch');
    this._el.style.display = 'inline-flex';
    this._el.style.alignItems = 'center';
    this._el.style.gap = '6px';
    this._el.style.cursor = 'pointer';
    var cb = document.createElement('input');
    cb.type = 'checkbox';
    cb.style.width = '20px'; cb.style.height = '20px';
    this._el.appendChild(cb);
    this._checkbox = cb;
    this._label = document.createElement('span');
    this._el.appendChild(this._label);
};
Swi.prototype = Object.create(Obj.prototype);
Swi.prototype.SetChecked = function(c) { this._checkbox.checked = c; };
Swi.prototype.IsChecked = function() { return this._checkbox.checked; };
Swi.prototype.SetText = function(t) { this._label.textContent = t||''; };
Swi.prototype.SetOnChange = function(fn) {
    var self = this;
    this._checkbox.onchange = function() { fn(self.IsChecked()); };
};

// ---- CheckBox ----
var Chk = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'CheckBox','label','CheckBox');
    this._el.style.display = 'inline-flex';
    this._el.style.alignItems = 'center';
    this._el.style.gap = '6px';
    this._el.style.cursor = 'pointer';
    var cb = document.createElement('input');
    cb.type = 'checkbox';
    cb.style.width = '18px'; cb.style.height = '18px';
    this._el.appendChild(cb);
    this._checkbox = cb;
    this._label = document.createElement('span');
    this._el.appendChild(this._label);
};
Chk.prototype = Object.create(Obj.prototype);
Chk.prototype.SetChecked = function(c) { this._checkbox.checked = c; };
Chk.prototype.IsChecked = function() { return this._checkbox.checked; };
Chk.prototype.SetText = function(t) { this._label.textContent = t||''; };
Chk.prototype.SetOnChange = function(fn) {
    var self = this;
    this._checkbox.onchange = function() { fn(self.IsChecked()); };
};

// ---- Toggle ----
var Tgl = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'Toggle','label','Toggle');
    this._el.style.display = 'inline-flex';
    this._el.style.alignItems = 'center';
    this._el.style.gap = '6px';
    this._el.style.cursor = 'pointer';
    var cb = document.createElement('input');
    cb.type = 'checkbox';
    cb.style.width = '18px'; cb.style.height = '18px';
    this._el.appendChild(cb);
    this._checkbox = cb;
    this._label = document.createElement('span');
    this._el.appendChild(this._label);
};
Tgl.prototype = Object.create(Obj.prototype);
Tgl.prototype.SetChecked = function(c) { this._checkbox.checked = c; };
Tgl.prototype.IsChecked = function() { return this._checkbox.checked; };
Tgl.prototype.SetText = function(t) { this._label.textContent = t||''; };
Tgl.prototype.SetOnChange = function(fn) {
    var self = this;
    this._checkbox.onchange = function() { fn(self.IsChecked()); };
};

// ---- Spinner ----
var Spn = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'Spinner','select','Spinner');
    this._el.style.padding = '8px';
    this._el.style.border = '1px solid #ccc';
    this._el.style.borderRadius = '4px';
    this._el.style.fontSize = '16px';
    this._items = [];
};
Spn.prototype = Object.create(Obj.prototype);
Spn.prototype.SetList = function(items) {
    this._items = items||[];
    this._el.innerHTML = '';
    var self = this;
    (items||[]).forEach(function(item) {
        var o = document.createElement('option');
        o.textContent = typeof item==='string'?item:item.Name||item.name||'';
        self._el.appendChild(o);
    });
};
Spn.prototype.GetList = function() { return this._items; };
Spn.prototype.GetText = function() { return this._el.options[this._el.selectedIndex]?.text||''; };
Spn.prototype.GetSelected = function() { return this._el.selectedIndex; };
Spn.prototype.SetSelected = function(i) { if(i>=0&&i<this._el.options.length) this._el.selectedIndex=i; };
Spn.prototype.SetOnChange = function(fn) {
    var self = this;
    this._el.onchange = function() { fn(self.GetSelected(),self.GetText()); };
};

// ---- SeekBar ----
var Skb = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'SeekBar','input','SeekBar');
    this._el.type = 'range';
    this._el.min = '0';
    this._el.max = '100';
    this._el.value = '0';
    this._el.style.width = '100%';
    this._onChange = null;
};
Skb.prototype = Object.create(Obj.prototype);
Skb.prototype.SetProgress = function(v) { this._el.value = Math.min(100,Math.max(0,v)); };
Skb.prototype.GetProgress = function() { return parseFloat(this._el.value); };
Skb.prototype.SetRange = function(min,max) { this._el.min=min; this._el.max=max; };
Skb.prototype.SetOnChange = function(fn) {
    var self = this;
    this._el.oninput = function() { fn(self.GetProgress()); };
};

// ---- Dialog ----
var Dlg = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'Dialog','div','Dialog');
    this._el.style.position = 'fixed';
    this._el.style.top = '0'; this._el.style.left = '0';
    this._el.style.width = '100%'; this._el.style.height = '100%';
    this._el.style.background = 'rgba(0,0,0,0.4)';
    this._el.style.display = 'flex';
    this._el.style.alignItems = 'center';
    this._el.style.justifyContent = 'center';
    this._el.style.zIndex = '9999';
    this._content = _el('div','DialogContent');
    this._content.style.background = '#fff';
    this._content.style.borderRadius = '12px';
    this._content.style.padding = '20px';
    this._content.style.minWidth = '260px';
    this._content.style.maxWidth = '80%';
    this._content.style.boxShadow = '0 4px 24px rgba(0,0,0,0.15)';
    this._el.appendChild(this._content);
    this._el.style.display = 'none';
};
Dlg.prototype = Object.create(Obj.prototype);
Dlg.prototype.Show = function() { this._el.style.display = 'flex'; };
Dlg.prototype.Hide = function() { this._el.style.display = 'none'; };
Dlg.prototype.AddChild = function(child) {
    if(child && child._el) this._content.appendChild(child._el);
};
Dlg.prototype.SetTitle = function(t) {
    if(!this._titleEl) {
        this._titleEl = _el('div','DialogTitle');
        this._titleEl.style.fontSize = '18px';
        this._titleEl.style.fontWeight = 'bold';
        this._titleEl.style.marginBottom = '12px';
        this._content.insertBefore(this._titleEl,this._content.firstChild);
    }
    this._titleEl.textContent = t;
};

// ---- Scroller ----
var Scr = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'Scroller','div','Scroller');
    this._el.style.overflow = 'auto';
    this._el.style.position = 'relative';
};
Scr.prototype = Object.create(Obj.prototype);
Scr.prototype.AddChild = function(child) { if(child&&child._el) this._el.appendChild(child._el); };
Scr.prototype.SetScrollPosition = function(x,y) { this._el.scrollLeft=x; this._el.scrollTop=y; };

// ---- File ----
var Fil = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'File','div','File');
    this._path = '';
    this._mode = '';
};
Fil.prototype = Object.create(Obj.prototype);
Fil.prototype.Open = function(fn) { this._path=fn; };
Fil.prototype.Read = function() {
    // For files in the app, use fetch
    if(this._path) fetch(this._path).then(function(r){return r.text();}).catch(function(){return '';});
    return '';
};
Fil.prototype.Write = function(txt) {
    // Can't write to local files from browser
    console.log('File.Write not available in browser: '+txt);
};

// ---- VideoView ----
var Vid = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'VideoView','video','VideoView');
    this._el.style.width = '100%';
    this._el.controls = true;
};
Vid.prototype = Object.create(Obj.prototype);
Vid.prototype.SetFile = function(f) { this._el.src = f; };
Vid.prototype.Play = function() { this._el.play(); };
Vid.prototype.Pause = function() { this._el.pause(); };
Vid.prototype.Stop = function() { this._el.pause(); this._el.currentTime=0; };

// ---- CameraView placeholder ----
var Cam = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Obj.call(this,'CameraView','div','CameraView');
    this._el.textContent = '[Camera]';
    this._el.style.background = '#000';
    this._el.style.color = '#fff';
    this._el.style.padding = '40px';
    this._el.style.textAlign = 'center';
};

// ---- YesNoDialog ----
var Ynd = function(id) {
    if(id) { this.id=id; this._el=_map[id]?._el; return; }
    Dlg.call(this,id);
    this.typeId = 'YesNoDialog';
    this._onYes = null; this._onNo = null;
    var self = this;
    var btnRow = _el('div','DialogBtns');
    btnRow.style.display = 'flex';
    btnRow.style.gap = '10px';
    btnRow.style.justifyContent = 'flex-end';
    btnRow.style.marginTop = '16px';
    var yBtn = document.createElement('button');
    yBtn.textContent = 'Yes';
    yBtn.style.cssText = 'padding:8px 24px;border:none;border-radius:6px;background:#6c5ce7;color:#fff;cursor:pointer';
    yBtn.onclick = function(){self.Hide();if(self._onYes)self._onYes();};
    btnRow.appendChild(yBtn);
    var nBtn = document.createElement('button');
    nBtn.textContent = 'No';
    nBtn.style.cssText = 'padding:8px 24px;border:none;border-radius:6px;background:#ddd;color:#333;cursor:pointer';
    nBtn.onclick = function(){self.Hide();if(self._onNo)self._onNo();};
    btnRow.appendChild(nBtn);
    this._content.appendChild(btnRow);
};
Ynd.prototype = Object.create(Dlg.prototype);
Ynd.prototype.SetOnYes = function(fn) { this._onYes = fn; };
Ynd.prototype.SetOnNo = function(fn) { this._onNo = fn; };

// ---- Global app object ----
var app = {
    //--- Startup ---
    Start: function() {
        if(typeof OnStart==='function') OnStart();
        if(typeof onStart==='function') onStart();
        _started = true;
    },

    //--- Layout Management ---
    AddLayout: function(lay) { if(lay&&lay._el) _root.appendChild(lay._el); },
    RemoveLayout: function(lay) { if(lay&&lay._el&&lay._el.parentNode) lay._el.parentNode.removeChild(lay._el); },

    //--- Object Creation ---
    CreateLayout: function(type,options) { var l=new Lay(); if(options) l.SetGravity(options); return l; },
    CreateText: function(text,w,h,opts) { var t=new Txt(); t.SetText(text); return t; },
    CreateButton: function(text,w,h,opts) { var b=new Btn(); b.SetText(text); return b; },
    CreateTextEdit: function(text,w,h,opts) { var t=new Txe(); t.SetText(text); return t; },
    CreateImage: function(file,w,h,opts) { var i=new Img(); i.SetFile(file); if(w||h) i.SetSize(w,h); return i; },
    CreateWebView: function(w,h,opts) { return new Web(); },
    CreateList: function(items,w,h,opts) { var l=new Lst(); if(items) l.SetList(items); return l; },
    CreateSwitch: function(text,w,h,opts) { var s=new Swi(); s.SetText(text); return s; },
    CreateCheckBox: function(text,w,h,opts) { var c=new Chk(); c.SetText(text); return c; },
    CreateToggle: function(text,w,h,opts) { var t=new Tgl(); t.SetText(text); return t; },
    CreateSpinner: function(items,w,h,opts) { var s=new Spn(); if(items) s.SetList(items); return s; },
    CreateSeekBar: function(w,h,opts) { return new Skb(); },
    CreateDialog: function(title,opts) { var d=new Dlg(); if(title) d.SetTitle(title); return d; },
    CreateYesNoDialog: function(msg,opts) { var y=new Ynd(); if(msg) y.SetTitle(msg); return y; },
    CreateScroller: function(w,h,opts) { return new Scr(); },
    CreateVideoView: function(w,h,opts) { return new Vid(); },
    CreateCameraView: function(w,h,opts) { return new Cam(); },
    CreateMediaPlayer: function() { return {_playing:false,SetFile:function(){},Play:function(){},Pause:function(){},Stop:function(){},GetType:function(){return'MediaPlayer';}}; },
    CreateFile: function(file,mode) { var f=new Fil(); f._path=file; f._mode=mode; return f; },
    CreateCanvas: function(w,h,opts) {
        var c = document.createElement('canvas');
        c.width = w||300; c.height = h||150;
        c.style.border = '1px solid #ccc';
        var obj = new Img();
        obj._el = c;
        obj.typeId = 'ImageView';
        obj.ctx = c.getContext('2d');
        return obj;
    },

    //--- Popups ---
    ShowPopup: function(msg,opts) {
        // Use a temporary toast overlay
        var t = document.createElement('div');
        t.textContent = msg;
        t.style.cssText = 'position:fixed;bottom:24px;left:50%;transform:translateX(-50%);background:#333;color:#fff;padding:12px 24px;border-radius:8px;font-size:14px;z-index:99999;box-shadow:0 2px 12px rgba(0,0,0,0.2);transition:opacity .3s';
        document.body.appendChild(t);
        setTimeout(function(){t.style.opacity='0';setTimeout(function(){if(t.parentNode)t.parentNode.removeChild(t);},300);},2000);
    },
    ShowProgress: function(msg,opts) { this.ShowPopup(msg); },
    HideProgress: function() {},
    Alert: function(msg,title) { alert(title?title+': '+msg:msg); },

    //--- Data Storage ---
    LoadText: function(name,dflt) {
        try { return localStorage.getItem('ds_'+name)||dflt||''; } catch(e){return dflt||'';}
    },
    SaveText: function(name,val) {
        try { localStorage.setItem('ds_'+name,val); } catch(e){}
    },
    LoadNumber: function(name,dflt) { return parseFloat(this.LoadText(name,dflt))||0; },
    SaveNumber: function(name,val) { this.SaveText(name,String(val)); },
    LoadJson: function(name,dflt) {
        try { return JSON.parse(this.LoadText(name,'{}')); } catch(e){return dflt||{};}
    },
    SaveJson: function(name,data) { this.SaveText(name,JSON.stringify(data)); },
    ClearData: function() { try { localStorage.clear(); } catch(e){} },

    //--- Screen Info ---
    GetScreenWidth: function() { return window.innerWidth; },
    GetScreenHeight: function() { return window.innerHeight; },
    GetScreenDensity: function() { return window.devicePixelRatio||1; },
    GetDisplayWidth: function() { return screen.width; },
    GetDisplayHeight: function() { return screen.height; },
    GetOrientation: function() { return window.innerWidth>window.innerHeight?'Landscape':'Portrait'; },
    IsPortrait: function() { return this.GetOrientation()==='Portrait'; },
    SetOrientation: function(o) {
        try { if(screen.orientation&&screen.orientation.lock) screen.orientation.lock(o==='Portrait'?'portrait':'landscape'); } catch(e){}
    },

    //--- App Info ---
    GetVersion: function() { return 1.0; },
    GetModel: function() { return navigator.userAgent; },
    GetPackageName: function() { return 'com.droidscript.app'; },
    GetAppName: function() { return document.title||'App'; },
    GetDeviceId: function() {
        try {
            var id = localStorage.getItem('ds_deviceId');
            if(!id) { id='dev_'+(Math.random().toString(36).substr(2,9)); localStorage.setItem('ds_deviceId',id); }
            return id;
        } catch(e){return 'unknown';}
    },
    GetType: function() { return 'Android'; },
    GetDebug: function() { return '0'; },
    Exit: function(kill) {
        document.body.innerHTML = '<div style="text-align:center;padding:40px;color:#999">App closed</div>';
    },

    //--- File Operations (browser-compatible) ---
    FileExists: function(f) { return false; },
    FolderExists: function(f) { return false; },
    ReadFile: function(f,enc) { return ''; },
    WriteFile: function(f,t,m,enc) {},
    DeleteFile: function(f) {},
    GetPrivateFolder: function() { return '/data'; },
    GetPublicFolder: function() { return '/sdcard'; },
    GetTempFolder: function() { return '/tmp'; },

    //--- Execute JS ---
    Execute: function(js) {
        try { eval(js); } catch(e){console.error(e);}
    },

    //--- Script Loading ---
    Script: function(file) {
        var s = document.createElement('script');
        s.src = file;
        document.head.appendChild(s);
    },
    LoadScript: function(url,cb) {
        var s = document.createElement('script');
        s.src = url;
        s.onload = function(){if(cb)cb();};
        document.head.appendChild(s);
    },

    //--- Misc ---
    Vibrate: function(pattern) {
        try { if(navigator.vibrate) navigator.vibrate(pattern||200); } catch(e){}
    },
    SetTitle: function(t) { document.title = t; },
    SetBackColor: function(c) { document.body.style.backgroundColor = c; },
    ShowKeyboard: function(obj) { if(obj&&obj._el) obj._el.focus(); return true; },
    HideKeyboard: function() { if(document.activeElement) document.activeElement.blur(); },
    GetLastButton: function() { return null; },
    GetLanguage: function() { return navigator.language||'en'; },
    GetLanguageCode: function() { return (navigator.language||'en').substr(0,2); },
    IsTablet: function() { return Math.min(screen.width,screen.height)>600; },
    IsConnected: function() { return navigator.onLine; },
    HasExternalStorage: function() { return true; },
    EnableBackKey: function(e) {},
    SetScreenMode: function(m) {},
    SetOptions: function(o) {},
    SetOnError: function(fn) { window.onerror = function(m,u,l){fn(m,u,l);return true;}; },
    MakeFolder: function(f) {},
    CopyFile: function(s,d) {},
    OpenUrl: function(url) { window.open(url,'_blank'); },
    SendMail: function(a,s,b) { window.location.href='mailto:'+a+'?subject='+encodeURIComponent(s)+'&body='+encodeURIComponent(b); },
    SendSMS: function(msg,num) { window.location.href='sms:'+num+'?body='+encodeURIComponent(msg); },
    TextToSpeech: function(t,p,r,cb) {
        if('speechSynthesis' in window) {
            var u = new SpeechSynthesisUtterance(t);
            u.onend = function(){if(cb)cb();};
            speechSynthesis.speak(u);
        }
    },
    GetTextBounds: function(txt,size,w) { return {width:txt.length*size*0.6,height:size*1.2}; }
};

// Helper to expose DroidScript wrapper types globally
win.Obj = Obj; win.Lay = Lay; win.Txt = Txt; win.Btn = Btn; win.Txe = Txe;
win.Img = Img; win.Web = Web; win.Lst = Lst; win.Swi = Swi; win.Chk = Chk;
win.Tgl = Tgl; win.Spn = Spn; win.Skb = Skb; win.Dlg = Dlg; win.Ynd = Ynd;
win.Scr = Scr; win.Fil = Fil; win.Vid = Vid; win.Cam = Cam;
win.app = app;
win.OnStart = win.OnStart || null;
win.onStart = win.onStart || null;

})(window);
