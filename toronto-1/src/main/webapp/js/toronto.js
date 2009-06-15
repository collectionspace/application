function field_jiggle() {
  $('.autowidth').each(function() {
    var parent=$(this).parents('.entry');
    var label=parent.find('.label');
    var size=parent.innerWidth()-label.outerWidth(true);
    var block=$(this).outerWidth(true)-$(this).innerWidth();
    if(size>0)
      $(this).width(size-block-1);
  });
}

function cols_jiggle() {
  var cols=$('.colleft').each(function() {
    var col1h=$(this).find('.col1').innerHeight();
    var col2h=$(this).find('.col2').innerHeight();
    if(col1h==0 || col2h==0)
      return;
    if(col1h>col2h) {
      var cur=$(this).find('.col2 .block:last').height();
      $(this).find('.col2 .block:last').height(cur+col1h-col2h);
    } else {
      var cur=$(this).find('.col1 .block:last').height();
      $(this).find('.col1 .block:last').height(cur+col2h-col1h);
    }
  });
}

function fin() {
  field_jiggle();
  cols_jiggle();
  $('.tabs').bind('tabsshow', function(event, ui) {
    field_jiggle();
    cols_jiggle();
  });
};

// Actions
$(function() {
  $('.button').click(function() {
    var type=$('#__type').text();  
    var code=$(this).attr('name');
    $.post('/toronto/ajax/'+type+'/preact',{'code':code},function(data,status) {
      var frompage={};
      for(var i=0;i<data.length;i++) {
        var result=data[i];
        if(result.action=='send-names') {
          $('.data',document).each(function(i) {
            frompage[$(this).attr('name')]=$(this).val();
          });
        }
        if(result.action=='send-key') {
          frompage[result.key]=$('#'+result.key,document).text();
        }
      }
      $.post('/toronto/ajax/'+type+'/act',{'code':code,'data': $.toJSON(frompage)},function(data,status) {
        var away=null;
        for(var i=0;i<data.length;i++) {
          var result=data[i];
          if(result.goto) {
            away=result.goto;
          }
        }
        for(var i=0;i<data.length;i++) {
          var result=data[i];
          if(result.good) {
            if(away) {
		      setCookie('good',result.good,null);
  		    } else {
		      show_good(result.good);
		    }
          }
        }
        if(away)
          document.location.href=away;
      },'json');
    },'json');
  });
});

$(function() {
  var good=getCookie('good');
  if(good) {
    setCookie('good','',null);
    show_good(good);
  }
});

// Results

function show_good(html) {
  $('#good').html(html);
  $('#good').show('highlight',{},1000,hide_good);
}

function hide_good(){
  setTimeout(function(){
    $("#good:visible").removeAttr('style').hide().fadeOut();
  }, 2000);
};
