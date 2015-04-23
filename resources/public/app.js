var l = document.location;

var ws = new WebSocket('ws://' + l.host + '/repl');

function byId (id) {
  return document.getElementById(id);
}

byId('inp').onkeyup = function(ev){
  if(ev.which == 13 && ev.ctrlKey){
    ws.send(byId('inp').value);
  }
}

ws.onmessage = function(ev){
  console.log(ev.data);
  var html = byId('out').innerHTML;
  byId('out').innerHTML = ev.data + html;
}
