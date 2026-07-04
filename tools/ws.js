
_WebSock = function( address, id, retry, options )
{
    var m_OnMessage = null;
    var m_OnError = null;
    var m_sock = null;
    var m_timer = null;
    var m_OnOpen = null;
    var m_OnClose = null;
    var m_isOpen = false;

    if( !options ) options = ""
    options = options.toLowerCase()
    var m_log = !options.includes( "nolog" );
    CheckSocket()
	if( retry!=0 )
	    m_timer = setInterval( CheckSocket, retry?retry:7000 );
    
    function OnOpen()
    {
        app.SetOnError( null )
        _UseDbg( true )
        if( m_log ) console.log( "WebSocket open ("+id+")" );
        if (m_OnOpen) m_OnOpen(id);
        m_isOpen=true;
    }
    
    function CheckSocket()
    {
        if( !m_sock || m_sock.readyState != 1 )
        {
            if( m_log ) console.log( "Opening WebSocket ("+id+"): "+address );
            _UseDbg( false )
            app.SetOnError( function(){} )
            m_sock = new WebSocket( address );
            m_sock.onopen = OnOpen;
            m_sock.onmessage = OnMessage;
            m_sock.onclose = OnClose;
            m_sock.onerror = OnError;
        }
    }
    
    function OnClose( event )
    {
        if( m_log ) console.log( "WebSocket closed ("+id+"): "+event.code );
        if (m_OnClose) m_OnClose( event.code, id );
        m_isOpen=false;
    }

    function OnMessage( msg ) {
        if( m_log ) console.log( "WebSocket message ("+id+"): "+msg.data.substring(0,64) );
        if( m_OnMessage ) m_OnMessage( msg.data, id );
    }

    function OnError(e)
    {
        app.SetOnError( null )
        _UseDbg( true )
        if( m_log ) console.log( "WebSocket error ("+id+"): "+JSON.stringify(e) );
        if( m_OnError ) m_OnError( e, id );
    }

	this.Close = function() { m_sock.close(); }
    this.GetSocket = function() { return m_sock; }
    this.SetOnMessage = function( callback ) { m_OnMessage = callback; }
    this.SetOnError = function( callback ) { m_OnError = callback; }
    this.SetOnOpen = function( callback ) { m_OnOpen = callback; }
    this.SetOnClose = function( callback ) { m_OnClose = callback; }
    this.IsOpen = function() { return m_isOpen; }
    
    this.Send = function( msg )
    {
        if( m_sock.readyState != 1 ) {
            if( m_log ) console.log( "WebSocket not ready ("+id+")" )
        }
        else m_sock.send( msg );
    }
}
