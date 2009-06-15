function sidebar_accordion_load() {
  var key=$('#__key').text();
  var i=0;
  var val=-1;
  $('#sidebar-accordion .record-id').each(function(){
    if($(this).text()==key) { val=i; }
    i++;
  });
  if(val!=-1)
    $('#sidebar-accordion').accordion({ active: val});
  else
    $('#sidebar-accordion').accordion();


  $('.sidebar-accordion-nav .back').click(function() {
    accordion_nav($(this));
    return false;
  });
  $('.sidebar-accordion-nav .fwd').click(function() {
    accordion_nav($(this));
    return false;
  });
  $('.sidebar-accordion-nav .clear').click(function() {
    accordion_clear($(this));
    return false;
  });
}

// XXX remove ref to toronto here and elsewhere
function accordion_nav(anchor) {
  var dest=anchor.attr('href').split('#')[1];
  var type=$('#__type').text();
  $.post('/toronto/data/accordion/'+type+'/'+dest,{},function(data,status) {
    $('#sidebar-accordion').accordion('destroy');
    $('#sidebar-accordion-block').html(data);
    sidebar_accordion_load();
  },'text');
}

function accordion_clear(anchor) {
  $.post('/toronto/data/clear-accordion',{},function(data,status) {
    accordion_nav(anchor);
  },'text');
}
