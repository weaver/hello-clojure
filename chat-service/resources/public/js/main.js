(function($) {

   function chat(selector) {
     var root = $(selector);
     var transcript = root.children('.transcript');
     var controls = root.children('.controls');
     var message = controls.children('[name=message]');

     var socket;

     function send(msg) {
       console.log('sending', msg);
       socket.send(msg);
     }

     function receive(msg) {
       $('<p/>').text(msg.data).appendTo(transcript);
     }

     function connect() {
       socket = new WebSocket('ws://localhost:8080/lobby/chat');

       socket.onopen = function() {
         console.log('socket opened', arguments);
         send('anonymous');
       };

       socket.onerror = function() {
         console.log('socket error', arguments);
       };

       socket.onclose = function() {
         console.log('socket closed', arguments);
       };

       socket.onmessage = receive;
     }

     connect();
     controls.submit(function(ev) {
       ev.preventDefault();
       send(message.val());
       message.val('').focus();
     });
   }

   $(function() {
     chat('#chat');
   });

})(jQuery);