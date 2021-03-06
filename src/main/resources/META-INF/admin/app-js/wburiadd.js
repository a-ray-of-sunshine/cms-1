/*
* Copyright 2014 Webpagebytes
* http://www.apache.org/licenses/LICENSE-2.0.txt
*/
var errorsGeneral = {
	'ERROR_URI_START_CHAR': "Site url must to start with /",
	'ERROR_URI_LENGTH': 'Site url length must be between 1 and 255 characters',
	'ERROR_INVALID_VALUE':'Invalid value'
	
};

$().ready( function () {
	var wbUriValidationRules = {
								'uri': [ {rule:{startsWith: '/'}, error: 'ERROR_URI_START_CHAR'}, {rule:{customRegexp:{pattern:"^/([0-9a-zA-Z_~.-]*(\{[0-9a-zA-Z_.*-]+\})*[0-9a-zA-Z_~.-]*/?)*$", modifiers:"gi"}}, error:"ERROR_INVALID_VALUE"}, { rule:{rangeLength: { 'min': 1, 'max': 250 } }, error:"ERROR_URI_LENGTH"} ],
								'controllerClass': [{ rule:{ maxLength: 250 }, error: "ERROR_INVALID_VALUE"}, {rule:{customRegexp:{pattern:"^[0-9a-zA-Z_.-]*$", modifiers:"gi"}}, error:"ERROR_INVALID_VALUE"}],
								'httpOperation': [{ rule: { includedInto: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS', 'PATCH', 'HEAD']}, error: "ERROR_INVALID_VALUE" }],
								'resourceType': [ { rule: { includedInto: [ '1', '2', '3' ] }, error:"ERROR_INVALID_VALUE" } ],
								'resourceExternalKey': [ {rule:{customRegexp:{pattern:"^[\\s0-9a-zA-z-]*$", modifiers:"gi"}}, error:"ERROR_INVALID_VALUE"}],
								'controllerClass': [{rule: { rangeLength: { 'min': 0, 'max': 255 } }, error: "ERROR_INVALID_VALUE" }, {rule:{customRegexp:{pattern:"^[0-9a-zA-Z_.]*$", modifiers:"gi"}}, error:"ERROR_INVALID_VALUE"}]
							  };


	$('#wburiadd').wbObjectManager( { fieldsPrefix:'wba',
									  errorLabelsPrefix: 'erra',
									  errorGeneral:"erregeneral",
									  errorLabelClassName: 'errorvalidationlabel',
									  errorInputClassName: 'errorvalidationinput',
									  fieldsDefaults: { 'uri': '/', 'httpOperation': 'GET', 'enabled': 0, 'resourceType': 1 },
									  validationRules: wbUriValidationRules
									});
	
	$('.btn-clipboard').WBCopyClipboardButoon({basePath: getAdminPath(), selector: '.btn-clipboard'});

	var ResourceExternalBlur = function (e) {
		var value = $.trim($(e.target).val());	
		var urlValue = "./search";
		if ($('input[name="resourceType"]:checked').val() == "1") {
			urlValue = "./search?externalKey={0}&class=wbpage".format(encodeURIComponent(value));
		} else if ($('input[name="resourceType"]:checked').val() == "2") {
			urlValue = "./search?externalKey={0}&class=wbfile".format(encodeURIComponent(value));			
		} 
		$('#wburiadd').wbCommunicationManager().ajax ( { url: urlValue,
			 httpOperation:"GET", 
			 payloadData:"",
			 functionSuccess: fSuccessSearch,
			 functionError: fErrorSearch
			} );
			
	};

	$('input[name="resourceType"]').on("change", function() {
		var val = $('input[name="resourceType"]:checked').val();
		if (val == 1 || val == 2) {
			$(".wbResourceExternalKey").show();
			$(".wbUrlController").hide();
			$("#wbaresourceExternalKey").trigger("change");
		} else if (val == 3) {
			$(".wbResourceExternalKey").hide();
			$(".wbUrlController").show();			
		}
		
	});
	
	$("#wbaresourceType").trigger("change");
	
	var oResourceExternalKey = "";
	var externalKeysArrays = { 'files':[], 'pages': [] }
	var notFoundMessage = "RESOURCE NOT FOUND";
	var fSuccessSearch = function (data) {
		var result = data.data;
		var html = escapehtml(notFoundMessage); 
		if (result.length == 1) {
			if ($('input[name="resourceType"]:checked').val() == "1") {
				var page = result[0];
				html = '<a href="./webpage.html?extKey={0}"> {1} </a>'.format(encodeURIComponent(page['externalKey']), escapehtml(page['name']));
			} else if ($('input[name="resourceType"]:checked').val() == "2") {
				var file = result[0];
				html = '<a href="./webfile.html?extKey={0}"> {1} </a>'.format(encodeURIComponent(file['externalKey']), escapehtml(file['fileName']));			
			} 			
		}
		$('#wbresourcelink').html(html);			
	}

	var fErrorSearch = function (data) {
		alert(data);
	}

	$("#wbaresourceExternalKey").on("change", ResourceExternalBlur);

	var fS_GetFilesPageSummary = function (data) {		
		var array = { 'files':[], 'pages': [] };
		for (var i in data['data_files']) {
			var item = data['data_files'][i];
			var val = "{0} {{1}}".format(item['name'], item['externalKey']);
			array['files'].push(val);
		};
		for (var i in data['data_pages']) {
			var item = data['data_pages'][i];
			var val = "{0} {{1}}".format(item['name'], item['externalKey']);
			array['pages'].push(val);
		}		
		externalKeysArrays = array;
	};
	
	var fE_GetFilesPageSummary = function (data) {
		alert(errors);
	};
	
	var updaterFunction = function(item) {
		//return item;
		x = item.lastIndexOf('{');
		if (x>=0) {
			y = item.lastIndexOf('}');
			if (y>=0) {
				return item.substring(x+1,y);
			}
		}
		return item;
	};
	
	var sourceFunction = function(query, process) {
		//what is selected files or pages ?
		if ($('input[name="resourceType"]:checked').val() == "1") {
			return externalKeysArrays['pages'];
		} else if ($('input[name="resourceType"]:checked').val() == "2") {
			return externalKeysArrays['files'];			
		} 
		return [];		
	}
	
	$('#wbaresourceExternalKey').typeahead( {
		source: sourceFunction,
		items: 3,
		updater: updaterFunction
	});

	
	$('#wburiadd').wbCommunicationManager().ajax ( { url:"./wbsummary_pages_files",
		 httpOperation:"GET", 
		 payloadData:"",
		 functionSuccess: fS_GetFilesPageSummary,
		 functionError: fE_GetFilesPageSummary
		} );
	
	var fSuccessAdd = function ( data ) {
		window.location.href = "./weburis.html";
	}
	var fErrorAdd = function (errors, data) {
		$('#wburiadd').wbObjectManager().setErrors(errors);
	}

	$('.wbUriAddBtnClass').click( function (e) {
		e.preventDefault();
		var errors = $('#wburiadd').wbObjectManager().validateFieldsAndSetLabels( errorsGeneral );
		if ($.isEmptyObject(errors)) {
			var uri = $('#wburiadd').wbObjectManager().getObjectFromFields();
			var jsonText = JSON.stringify(uri);
			$('#wburiadd').wbCommunicationManager().ajax ( { url: "./wburi",
															 httpOperation:"POST", 
															 payloadData:jsonText,
															 wbObjectManager : $('#wburiadd').wbObjectManager(),
															 functionSuccess: fSuccessAdd,
															 functionError: fErrorAdd
															 } );
		}
	});
	
	$('.wbUriAddCancelBtnClass').click ( function (e) {
		e.preventDefault();
		window.location.href = "./weburis.html";
	});

	var qType = getURLParameter("qtype") || "";
	var qParam = false;
	if (qType == 'file') {
		$('input[name="resourceType"]').val(["2"]);
		qParam = true;
	} if (qType == 'page') {
		$('input[name="resourceType"]').val(["1"]);
		qParam = true;
	} 
	var qValue = getURLParameter("qextKey") || ""
	qParam = qParam && (qValue.length > 0);
	if (qParam) {
		$("#wbaresourceExternalKey").val(qValue);
		$("#wbaresourceExternalKey").trigger("change");
	}
	
		
});