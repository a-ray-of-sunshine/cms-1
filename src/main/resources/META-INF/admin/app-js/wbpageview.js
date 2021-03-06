/*
* Copyright 2014 Webpagebytes
* http://www.apache.org/licenses/LICENSE-2.0.txt
*/
var errorsGeneral = {
	ERROR_PARAM_NAME_LENGTH: 'Parameter name length must be between 1 and 250 characters',
	ERROR_PARAM_INVALID_OVERWRITE: 'Operation on overwrite not supported',
	ERROR_PARAM_INVALID_LOCALETYPE: 'Operation on locale not supported'
};

$().ready( function () {
	var wbParameterValidations = { 
			name: [{rule: { rangeLength: { 'min': 1, 'max': 250 } }, error: "ERROR_PARAM_NAME_LENGTH" }],
			overwriteFromUrl: [{rule: { includedInto: ['0','1'] }, error: "ERROR_PARAM_INVALID_OVERWRITE" }],
			localeType: [{rule: { includedInto: ['0','1','2'] }, error: "ERROR_PARAM_INVALID_LOCALETYPE" }]
	};

	$('#wbAddParameterForm').wbObjectManager( { fieldsPrefix:'wba',
									  errorLabelsPrefix: 'erra',
									  errorGeneral:"errageneral",
									  errorLabelClassName: 'errorvalidationlabel',
									  errorInputClassName: 'errorvalidationinput',
									  fieldsDefaults: { overwriteFromUrl: 0, localeType: 0 },
									  validationRules: wbParameterValidations
									});
	$('#wbUpdateParameterForm').wbObjectManager( { fieldsPrefix:'wbu',
									  errorLabelsPrefix: 'erru',
									  errorGeneral:"errageneral",
									  fieldsDefaults: { overwriteFromUrl: 0, localeType: 0 },
									  errorLabelClassName: 'errorvalidationlabel',
									  errorInputClassName: 'errorvalidationinput',
									  validationRules: wbParameterValidations
									});

	$('#wbDeleteParameterForm').wbObjectManager( { fieldsPrefix: 'wbd',
									 errorGeneral:"errdgeneral",
									 errorLabelsPrefix: 'errd',
									 errorLabelClassName: 'errorvalidationlabel',
									} );							

	$('.btn-clipboard').WBCopyClipboardButoon({buttonHtml:"<i class='fa fa-paste'></i><div class='wbclipboardtooltip'>Copy to clipboard</div>", basePath: getAdminPath(), selector: '.btn-clipboard'});
	$('.btn-clipboard').WBCopyClipboardButoon().on("aftercopy", function (e) {
		$('.btn-clipboard').WBCopyClipboardButoon().reset();
		$(e.target).html("<i class='fa fa-paste'></i><div class='wbclipboardtooltip'>Copied!</div>");
	});

	var tableDisplayHandler = function (fieldId, record) {
		if (fieldId=="_operations") {
			return '<a href="#" class="wbEditParameterClass" id="wbEditParam_' + encodeURIComponent(record['externalKey']) + '"><i class="icon-pencil"></i> Edit </a> | <a href="#" class="wbDeleteParameterClass" id="wbDelParam_' + encodeURIComponent(record['externalKey'])+ '"><i class="icon-trash"></i> Delete </a>'; 
		} else
		if (fieldId=="lastModified") {
			var date = new Date();
			return date.toFormatString(record[fieldId], "dd/mm/yyyy hh:mm:ss");
		}
	}
				
	$('#wbPageParametersTable').wbSimpleTable( { columns: [ {display: "Name", fieldId: "name"}, {display: "Value", fieldId: "value"},
								         {display: "Operations", fieldId:"_operations", customHandler: tableDisplayHandler}],
						 keyName: "externalKey",
						 tableBaseClass: "table table-stripped table-bordered table-color-header",
						 paginationBaseClass: "pagination",
						 noLinesContent: "<tr> <td colspan='3'>There are no parameters defined. </td></tr>",
						 textLengthToCut: 20
						});

	var urisDisplayHandler = function (fieldId, record) {
		if (fieldId=="uri") {
			var link = "./weburiedit.html?extKey={0}".format(encodeURIComponent(record['externalKey']));
			return '<a href="{0}"> {1} </a>'.format(link, escapehtml(record['uri'])); 
		} 
	}
	
	$('#wburistable').wbSimpleTable( { columns: [{display: "Site urls linked to this page", fieldId:"uri", customHandler: urisDisplayHandler}],
	         						 keyName: "externalKey",
	         						 tableBaseClass: "table table-stripped table-bordered table-color-header",
	         						 paginationBaseClass: "pagination",
	         						 noLinesContent: "<tr> <td colspan='1'>There are no site links serving this page. </td></tr>"	               						
	         						});

	var displayHandler = function (fieldId, record) {
		if (fieldId == 'lastModified') {
			return escapehtml( "Last modified: " + Date.toFormatString(record[fieldId], "today|dd/mm/yyyy hh:mm"));
		} 
		if (fieldId == 'name') {
			var innerHtml = '<a href="./webpage.html?extKey=' + encodeURIComponent(record['externalKey']) + '">' + escapehtml(record['name']) + '</a>';
			return innerHtml;
		}
		return record[fieldId];
	}
	var pageSourceHandler = function (fieldId, record) {
		if (fieldId == 'isTemplateSource') {
			var plainValue = "", templateValue = "";
			if ('isTemplateSource' in record)
			{
				if (record['isTemplateSource'] == '0') {
					plainValue='checked';
				} else if (record['isTemplateSource'] == '1') {
					templateValue = 'checked';
				}
			}
			var innerHtml = '<input class="input-xlarge" type="radio" {0} disabled="disabled"> Plain text source <input class="input-xlarge" type="radio" {1} disabled="disabled"> Template text source'.format(plainValue, templateValue); 			
			return innerHtml;
		}
		if (fieldId == 'htmlSource') {
			return record[fieldId]; // the htmlSource is displayed in a textarea element
		}
		if (fieldId == 'name') {
			return record[fieldId];
		}
		return escapehtml(record[fieldId]);
	}
	
	$('#wbPageSummary').wbDisplayObject( { fieldsPrefix: 'wbsummary', customHandler: displayHandler} );
	$('#wbPageView').wbDisplayObject( { fieldsPrefix: 'wbPageView', customHandler: pageSourceHandler} );
	
	var fSuccessGetPage = function (data) {
		$('#wbPageSummary').wbDisplayObject().display(data.data);
		$('#wbPageView').wbDisplayObject().display(data.data);
		$('#wburistable').wbSimpleTable().setRows(data.additional_data.uri_links);
		if (data.data["isTemplateSource"] != '1') {
			$('.wbModelProviderContainer').hide();
		} else {
			$('.wbModelProviderContainer').show();
		}
		console.log("fSuccessGetPage" + new Date());
	}
	
	$('input[name="isTemplateSource"]').on("change", function() {
		$('.wbModelProviderContainer').toggle();
	});

	var fErrorGetPage = function (errors, data) {
		alert(errors);
	}

	var pageExternalKey = getURLParameter('extKey');;
		
	$('.wbPageViewEditLink').click ( function (e) {
		e.preventDefault();
		window.location.href = "./webpageedit.html?extKey={0}".format(encodeURIComponent(pageExternalKey));
	} );
	
	$('#wbAddParameterBtn').click ( function (e) {
		e.preventDefault();
		$('#wbAddParameterForm').wbObjectManager().resetFields();
		$('#wbAddParameterModal').modal('show');
	});
	
	$("#wbAddUrlBtn").click ( function (e) {
		e.preventDefault();
		window.location.href = "./weburiadd.html?qtype=page&qextKey={0}".format(encodeURIComponent(pageExternalKey));
	});
	
	var fSuccessAdd = function ( data ) {
		$('#wbAddParameterModal').modal('hide');
		$('#wbPageParametersTable').wbSimpleTable().insertRow(data.data);			
	}
	var fErrorAdd = function (errors, data) {
		$('#wbAddParameterForm').wbObjectManager().setErrors(errors);
	}

	$('.wbAddParameterBtnClass').click( function (e) {
		e.preventDefault();
		var errors = $('#wbAddParameterForm').wbObjectManager().validateFieldsAndSetLabels( errorsGeneral );
		if ($.isEmptyObject(errors)) {
			var parameter = $('#wbAddParameterForm').wbObjectManager().getObjectFromFields();
			parameter['ownerExternalKey'] = pageExternalKey;
			var jsonText = JSON.stringify(parameter);
			$('#wbAddParameterForm').wbCommunicationManager().ajax ( { url: "./wbparameter",
															 httpOperation:"POST", 
															 payloadData:jsonText,
															 wbObjectManager : $('#wbAddParamaterForm').wbObjectManager(),
															 functionSuccess: fSuccessAdd,
															 functionError: fErrorAdd
															 } );
		}
	});

	var fSuccessUpdate = function ( data ) {
		$('#wbUpdateParameterModal').modal('hide');		
		$('#wbPageParametersTable').wbSimpleTable().updateRowWithKey(data.data,data.data["externalKey"]);
	}
	var fErrorUpdate = function (errors, data) {
		$('#wbUpdateParameterForm').wbObjectManager().setErrors(errors);
	}

	$('.wbUpdateParameterBtnClass').click( function (e) {
		e.preventDefault();
		var errors = $('#wbUpdateParameterForm').wbObjectManager().validateFieldsAndSetLabels( errorsGeneral );
		if ($.isEmptyObject(errors)) {
			var object = $('#wbUpdateParameterForm').wbObjectManager().getObjectFromFields();
			object['ownerExternalKey'] = pageExternalKey;
			var jsonText = JSON.stringify(object);
			$('#wbUpdateParameterForm').wbCommunicationManager().ajax ( { url: "./wbparameter/" + encodeURIComponent(object['externalKey']),
															 httpOperation:"PUT", 
															 payloadData:jsonText,
															 wbObjectManager : $('#wbUpdateParameterForm').wbObjectManager(),
															 functionSuccess: fSuccessUpdate,
															 functionError: fErrorUpdate
															 } );
		}
	});

	var fSuccessDelete = function ( data ) {
		$('#wbDeleteParameterModal').modal('hide');		
		$('#wbPageParametersTable').wbSimpleTable().deleteRowWithKey(data.data["externalKey"]);
	}
	var fErrorDelete = function (errors, data) {
		$('#wbDeleteParameterForm').wbObjectManager().setErrors(errors);
	}

	$('.wbDeleteParameterBtnClass').click( function (e) {
		e.preventDefault();
		var errors = $('#wbDeleteParameterForm').wbObjectManager().validateFieldsAndSetLabels( errorsGeneral );
		if ($.isEmptyObject(errors)) {
			var object = $('#wbDeleteParameterForm').wbObjectManager().getObjectFromFields();
			$('#wbDeleteParameterForm').wbCommunicationManager().ajax ( { url: "./wbparameter/" + encodeURIComponent(object['externalKey']),
															 httpOperation:"DELETE", 
															 payloadData:"",
															 wbObjectManager : $('#wbDeleteParameterForm').wbObjectManager(),
															 functionSuccess: fSuccessDelete,
															 functionError: fErrorDelete
															 } );
		}
	});

	
	$(document).on ("click", ".wbEditParameterClass", function (e) {
		e.preventDefault();
		$('#wbUpdateParameterForm').wbObjectManager().resetFields();
		var key = $(this).attr('id').substring("wbEditParam_".length);
		var object = $('#wbPageParametersTable').wbSimpleTable().getRowDataWithKey(key);
		$('#wbUpdateParameterForm').wbObjectManager().populateFieldsFromObject(object);
		$('#wbUpdateParameterModal').modal('show');		
	});

	$(document).on ("click", ".wbDeleteParameterClass", function (e) {
		e.preventDefault();
		$('#wbDeleteParameterForm').wbObjectManager().resetFields();
		var key = $(this).attr('id').substring("wbDelParam_".length);
		var object = $('#wbPageParametersTable').wbSimpleTable().getRowDataWithKey(key);
		$('#wbDeleteParameterForm').wbObjectManager().populateFieldsFromObject(object);
		$('#wbDeleteParameterModal').modal('show');		
	});

	var fSuccessGetParameters = function (data) {
			$('#wbPageParametersTable').wbSimpleTable().setRows(data.data);
			console.log("fSuccessGetParameters");
	}
	var fErrorGetParameters = function (errors, data) {
	
	}
	
	var allAjaxOK = function () {
	    console.log("all ajax ok " + new Date());
		$('#spinnerTable').WBSpinner().hide();
	}
	
	var arrayAjax = [ { url:"./wbpage/ext/{0}?include_links=1".format(encodeURIComponent(pageExternalKey)),
		 httpOperation:"GET", 
		 payloadData:"",
		 functionSuccess: fSuccessGetPage,
		 functionError: fErrorGetPage
		},
		{ url:"./wbparameter?ownerExternalKey=" + encodeURIComponent(pageExternalKey),
			 httpOperation:"GET", 
			 payloadData:"",
			 functionSuccess: fSuccessGetParameters,
			 functionError: fErrorGetParameters
			} ];
	$('#wbPageSummary').wbCommunicationManager().ajaxArray (arrayAjax, allAjaxOK, allAjaxOK);
												
});