
//Provide app.AddTabs alternative to app.CreateTabs
if(app.AddTabs === undefined)
{
    app.AddTabs = function( lay, list, width, height, options )
    {
        var tabs = app.CreateTabs(list, width, height, options);
        lay.AddChild(tabs);
        return tabs;
    }
}

//Tabs object.
_Tabs = function( list, width, height, options )
{
	var lst = list.split(",");
	this.tabs = [];
    var self = this;
	var curTabName = null;

	options = options.toLowerCase()
    var optFade = options.includes("fade");
    var noMargins = options.includes("nomargins");
    var useFA = options.includes("fontawesome");
	var thm = app.GetThemeInfo();
	var autoSize = options.includes("autosize")

	var FillX = width && width > 0 ? "FillX" : false;
	var FillY = height && height > 0 ? "FillY" : false;
	var FillXY = FillX && FillY ? "FillXY" : FillX || FillY;
	
	//Disable debug during creation.
	var dbg = _dbg; _UseDbg( false );
	
	//Create overall tabs layout.
    this.lay = app.CreateLayout( "Card", FillXY+(autoSize?",AutoSize":"") );
    this.lay.SetBackColor( thm.dark?(thm.holo?"#00000000":"#313538"):"#fefefe" );
    //this.lay.SetBackColor( "red" )
    this.lay.SetElevation( thm.holo?0:2 );
    if(FillX || FillY) this.lay.SetSize( width, height );
    this.lay.OnChange = null;
    this.lay.parent = this;
    
    //Create main tab layout.
    this.layMain = app.CreateLayout( "Linear", FillXY );
    //this.layMain.SetBackColor( "blue" )
    this.layMain.SetMargins( noMargins?0:0.02, 0.02, noMargins?0:0.02, 0 );
    this.lay.AddChild( this.layMain ); 
    
    //Create top (tab strip) layout.
    this.layTop = app.CreateLayout( "Linear", "Horizontal," + FillX );
    //this.layTop.SetBackColor( "green" );
    this.layMain.AddChild( this.layTop );
    
    //Create body layout.
    this.layBody = app.CreateLayout( "Frame", FillXY+(autoSize?",AutoSize":"") );
    //this.layBody.SetBackColor( "yellow" );
    if(FillX || FillY) this.layBody.SetSize( width, height-0.08);
    this.layMain.AddChild( this.layBody );

    
    //Add a tab.
    this.AddTab = function( name )
    {
		var dbg = _dbg; _UseDbg( false );
		self.layTab = app.CreateLayout( "Linear", "VCenter"+(autoSize?",AutoSize":"") );
		//self.layTab.SetMargins( 0,0,0,0 );
		self.layTab.SetSize( width/lst.length, 0.06 );
		self.txtTab = app.CreateText( name, -1, 0.06, "Bold,VCenter"+self.useFA?"fontawesome":"" );
		self.txtTab.SetBackground( "/Sys/Img/Tab_.png", "stretch" );
		self.txtTab.SetTextColor( thm.textColor1  );
		//this.txtTab.SetPadding( 0,0.03,0,0 );
		if( useFA ) self.txtTab.SetTextSize( 20 )
		self.txtTab.SetOnTouch( _Tabs_OnTouch );
		self.txtTab.tabs = self;
		self.layTab.AddChild( self.txtTab );
		self.layTop.AddChild( self.layTab );
		
		//Save array of tab info.
		self.txtTab.text = name
		self.tabs[name] = { txt:self.txtTab, tab:null, content:null };
		
		//Add tab content layout to body.
		self.tabs[name].tab = self.layTab;
		self.tabs[name].content = app.CreateLayout( "Linear", "FillXY,"+options );
		self.layBody.AddChild( self.tabs[name].content );
		_UseDbg( dbg );
    }
    
    //Set tab change callback.
    this.lay.SetOnChange = function( cb ) { this.OnChange = cb; }

    //Return layout for given tab.
    this.lay.GetLayout = function ( name )
    { 
        return this.parent.tabs[name].content;
    }
    
    //Get the current tab title/name
    this.lay.GetCurrentTabName = function()
    {
        return curTabName;
    }

    //Set tab text size.
    this.lay.SetTextSize = function( size, mode )
    {
        var dbg = _dbg; _UseDbg( false );
        for( var i in self.tabs )
            self.tabs[i].txt.SetTextSize( size, mode );
        _UseDbg( dbg );
    }

    this.lay.SetSize = function( width, height, options )
    {
        prompt( self.lay.id, "Obj.SetSize(\f"+width+"\f"+height+"\f"+options )
        //prompt( self.layMain.id, "Obj.SetSize(\f"+width+"\f"+height+"\f" )
        //self.layTop.SetSize( width, -1 );
        self.layBody.SetSize( width, height-0.06, options );

        //for( var i in self.tabs ) {
        //    self.tabs[i].tab.SetSize( width, height, "FillXY,"+options )
        //    self.tabs[i].content.SetSize( width, height, "FillXY,"+options )
        //}
    }

    //Resize the tabs to fit current orientation.
    this.lay.Resize = function()
    {
        var dbg = _dbg; _UseDbg( false );

        prompt( self.lay.id, "Obj.Resize(\f" );
        self.layBody.Resize();
        for( var i in self.tabs ) {
            self.tabs[i].tab.Resize();
            //self.tabs[i].content.Resize();
            //self.tabs[i].txt.Resize();
        }
        _UseDbg( dbg );
    }

    //Set current tab.
    this.lay.ShowTab = function( name )
    {
        var self = this.parent;
		var dbg = _dbg; _UseDbg( false );
		
        //Drop out if no change.
        if( curTabName == name ) return _UseDbg( dbg );
        curTabName = name;
        
        //Get tab info.
        var tab = self.tabs[name];
        if( !tab ) return _UseDbg( dbg );
        
	    //Select chosen tab.
        tab.txt.SetBackground( "/Sys/Img/TabHi_.png", "stretch" );
        tab.txt.SetColorFilter( thm.holo?"#33B4E5":thm.highlightColor, "SRC_IN" );
        
        //Show touched tab
        if(optFade)
        {
            tab.content.Animate( "FadeIn", null, 200 );
            self.layBody.ChildToFront( tab.content );
        }
        else tab.content.Show();

        //Clear other tab selections.
		for ( var tn in self.tabs )
        {
            var tb = self.tabs[tn];
            if( tb == tab || !tb.content.IsVisible() ) continue;

            //Hide visible tab
		    tb.txt.SetBackground( "/Sys/Img/Tab_.png", "stretch" );
            if(optFade) tb.content.Animate( "FadeOut", null, 200 );
            else tb.content.Hide();
	    }
        
        _UseDbg( dbg );
        
       //Fire callback if set.
       if( this.OnChange ) this.OnChange( name );
    }
    
    //Add tabs.
    for(var i in lst) this.AddTab( lst[i] );
	
	//Set default tab.
	this.lay.ShowTab( lst[0] );
	
	//Re-enable debug.
	_UseDbg( dbg );

    // adjust tab text size when app loaded (blows up in fast mode)
	/*setTimeout(function()
	{
    	var dbg = _dbg; _UseDbg( false );
    	var w = self.layMain.GetWidth();
    	for( var i in self.tabs ) self.tabs[i].txt.SetSize( w/lst.length );
    	_UseDbg( dbg );
	}, 101);
	*/

    //Return main layout to caller.
    return this.lay;
}

//Handle tab clicks.
function _Tabs_OnTouch( ev )
{
    if( ev.action == "Down" ) 
    {
		var dbg = _dbg; _UseDbg( false );
        var txt = ev.source;
        txt.tabs.lay.ShowTab( txt.text );
        _UseDbg( dbg );
    }
}
