$(document).ready(function() {
	$('.target').each(function(i) {
		var id=$(this).attr('id');
		var block=$(this);
		$("input",block).keyup(function(data) {
			$.get('/xxu/xxu/ajax',{action: 'validate', field: id, value: $(this).val()},function(data) {
				if(data) {
					$('.good',block).css('display','inline');
					$('.bad',block).css('display','none');
				} else {
					$('.bad',block).css('display','inline');
					$('.good',block).css('display','none');
				}
			},'json');
			return true;
		});
		$('input',this).autocomplete('/xxu/xxu/ajax', { extraParams: { action: 'suggest', field: id } });
	});
});