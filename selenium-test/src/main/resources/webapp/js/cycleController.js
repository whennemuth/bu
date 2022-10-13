var cycleCtrlFactory = function() {
	
	return {
		setScope: function(scope, cycleSvc) {
			
			// Load in Element Types
			cycleSvc.getElementTypes();
			
			// Load in ScreenScrape Types
			cycleSvc.getScreenScrapeTypes();
			
			// Load in DateFormat options
			cycleSvc.getDateFormats();
			
			// Load in DateParts options
			cycleSvc.getDateParts();
			
			// Add an event handler to query if the service has intialized yet.
			scope.isInitialized = function() {
				return cycleSvc.isInitialized();
			};
			
			scope.getElementTypes = function() {
				var types = cycleSvc.getElementTypes();
				var filtered = {};
				for(var et in types) {
					if(et != 'BUTTONIMAGE') {
						filtered[et] = types[et];
					}
				}
				return filtered;
			};
			
			scope.getScreenScrapeTypes = function() {
				return cycleSvc.getScreenScrapeTypes();
			};
			
			scope.getShortcuts = function() {
				return scope.config.configShortcuts;
			};
			
			scope.getDateFormats = function() {
				return cycleSvc.getDateFormats();
			};
			
			scope.getDateFormat = function(lv, callback) {
				return cycleSvc.getDateFormat(lv, callback);
			}
			
			scope.getDateParts = function() {
				var formats = cycleSvc.getDateParts();
				var filtered = {};
				for(var f in formats) {
					if(/(MONTH)|(DAY)|(YEAR)/.test(f.toUpperCase())) {
						filtered[f] = formats[f];
					}
				}
				return filtered;
			};
			
			scope.getShortcut = function(id) {
				for(var i=0; i<scope.config.configShortcuts.length; i++) {
					var shortcut = scope.config.configShortcuts[i];
					if(shortcut.id == id) {
						return shortcut;
					}
				}
				return {id:0};	// return a dummy shortcut that will be interpreted as such by id being 0
			};
			
			// Control which fields for label and value row get shown/hidden depending on element type selection.
			scope.lvShow = function(lv, inputType) {
				var checkable = false;
				var clickonly = false;
				var textvalue = false;
				var shortcut = false;
				var screenscrape = false;
				switch(lv.elementType) {
					case 'BUTTON': 	case 'HOTSPOT': case 'HYPERLINK': case 'SHORTCUT':
						clickonly = true;
						shortcut = lv.elementType == 'SHORTCUT';
						break;
					case 'CHECKBOX': case 'RADIO': 
						checkable = true;
						break;
					case 'SELECT': case 'TEXTAREA': case 'TEXTBOX': case 'PASSWORD': case 'OTHER':
						textvalue = true;
						break;
					case 'SCREENSCRAPE':
						screenscrape = true;
						break;
					default:
						return false;
				}
				
				switch(inputType) {
					case 'state':
						return checkable;
					case 'value':
						return textvalue;
					case 'navigates':
						return clickonly && !shortcut;
					case 'label': 
						return !shortcut
					case 'identifier':
						return !shortcut && !screenscrape;
					case 'shortcut':
						return shortcut;
					case 'screenscrape':
						return screenscrape;
					default:
						return lv.elementType;
				}
			};
			
			// Clear out or pre-populate field values of a label and value row depending on element type and checked state selections
			scope.lvChange = function(lv) {
				switch(lv.elementType) {
					case 'BUTTON': 	case 'HOTSPOT': case 'HYPERLINK':
						lv.value = null;
						lv.booleanValue = false;
						if(lv.configShortcut) {
							lv.configShortcut = null;
						}
						lv.screenScrapeType = null;
						scope.blankOutDateEntry(lv);
						break;
					case 'CHECKBOX': case 'RADIO': 
						lv.value = lv.booleanValue;
						lv.navigates = false;
						if(lv.configShortcut) {
							lv.configShortcut = null;
						}
						lv.screenScrapeType = null;
						scope.blankOutDateEntry(lv);
						break;
					case 'SELECT': case 'TEXTAREA': case 'TEXTBOX': case 'PASSWORD': case 'OTHER':
						lv.booleanValue = false;
						lv.value = /^(true)|(false)$/i.test(lv.value) ? null : lv.value;
						if(scope.isScreenScrapeType(lv.value)) {
							lv.value = null;
						}
						lv.navigates = false;
						if(lv.configShortcut) {
							lv.configShortcut = null;
						}
						lv.screenScrapeType = null;						
						scope.blankOutDateEntry(lv);
						break;
					case 'SHORTCUT':
						lv.identifier = null;
						lv.label = null;
						lv.value = null;
						lv.booleanValue = false;
						if(lv.configShortcut) {
							lv.navigates = (lv.navigates || scope.getShortcut(lv.configShortcut.id).navigates);
						}
						lv.screenScrapeType = null;
						scope.blankOutDateEntry(lv);
						break;
					case 'SCREENSCRAPE':
						lv.screenScrapeType = scope.getScreenScrapeTypes()[0].id; // default to the first one
						lv.identifier = null;
						lv.value = null;
						lv.booleanValue = false;
						lv.navigates = false;
						if(lv.configShortcut) {
							lv.configShortcut = null;
						}
						scope.blankOutDateEntry(lv);
						break;
					default:
						lv.identifier = null;
						lv.label = null;
						lv.value = null;
						lv.booleanValue = false;
						lv.navigates = false;
						if(lv.configShortcut) {
							lv.configShortcut = null;
						}
						scope.blankOutDateEntry(lv);
						break;
				}
			};
			
			scope.screenScrapeIdChange = function(lv) {
				lv.value = '';
				lv.dateUnits = null;
				if(lv.screenScrapeId == -1) {
					lv.dateUnits = 0;
					for(var part in scope.getDateParts()) {
						lv.datePart = part;
						break; // break after first iteration because we are defaulting to the first item in the list
					}
					for(var format in scope.getDateFormats()) {
						lv.dateFormatChoice = format;
						lv.dateFormat = format;
						break; // break after first iteration because we are defaulting to the first item in the list
					}
					scope.dateBlur(lv);
				}
				else {
					scope.blankOutDateEntry(lv, lv.screenScrapeId);
				}
			};
			
			scope.dateFormatChange = function(lv) {
				if(lv.dateFormatChoice == 'CUSTOM') { 
					lv.dateFormat = null; 
				} 
				else { 
					lv.dateFormat = lv.dateFormatChoice; 
				}
				scope.dateBlur(lv);
			};
			
			scope.dateBlur = function(lv) {
				var datespan = document.getElementById('computedDate' + lv.sequence);
				if(!lv.dateFormat)
					datespan.innerHTML = "<font color='red'>invalid/missing format!</font>";
				else if(!/^\-?\d+$/.test(lv.dateUnits))
					datespan.innerHTML = "<font color='red'>invalid/missing quantity!</font>";
				else if(!lv.datePart)
					datespan.innerHTML = "<font color='red'>invalid/missing units!</font>";
				else {
					scope.getDateFormat(lv, function(retval) {
						document.getElementById('computedDate' + lv.sequence).innerHTML = "<font color='green'>if run today: " + retval + "</font>";
					});
				}				
			};
			
			scope.blankOutDateEntry = function(lv, screenScrapeId) {
				if(screenScrapeId)
					lv.screenScrapeId = screenScrapeId;
				else
					lv.screenScrapeId = 0;
				lv.dateUnits = null;
				lv.datePart = null;
				lv.dateFormatChoice = null;
				lv.dateFormat = null;
				document.getElementById('computedDate' + lv.sequence).innerHTML = '';
			};
			
			scope.plusMinusIntegersOnly = function($event, val) {
				var char = String.fromCharCode($event.keyCode);
				var retval = /\d/.test(char) || (/\-/.test(char) && !val);
				if(retval && !/^\-/.test(val) && val && val.length == 3)
					retval = false;
				if(retval)
					return;
				
                $event.stopImmediatePropagation();
                $event.preventDefault();
                $event.stopPropagation();
                return false;
			};
			
			scope.dateFormatCharsOnly = function($event) {
				if(/[mdy\-\/,\.]/i.test(String.fromCharCode($event.keyCode)))
					return;
				
                $event.stopImmediatePropagation();
                $event.preventDefault();
                $event.stopPropagation();
                return false;
			};
			
			scope.isScreenScrapeType = function(id) {
				for(var i=0; i<scope.getScreenScrapeTypes().length; i++) {
					if(scope.getScreenScrapeTypes()[i].id == id) {
						return true;
					}
				}
				return false;
			};
			
			/** An array used to bind to screenscrape picklists */
			scope.screenscrapes = [];
			
			/**
			 * Refresh the array that is bound to all screenscrape picklists.
			 * This function should be called each time a screenscrape is added, removed or edited in any way.
			 */
			scope.refreshScreenScrapes = function() {
				scope.screenscrapes.length = 0; // clears the array without rereferencing it.
				// Add 2 default entries to the top of the array indicating non-screenscrape value entries.
				scope.screenscrapes[0] = {
						id:0, 
						label:'Manual Entry', 
						sequence:0,
						suiteId:0,
						suiteName:'',
						suiteSequence:0
					}
				scope.screenscrapes[1] = {
						id:-1, 
						label:'Date Entry', 
						sequence:0,
						suiteId:0,
						suiteName:'',
						suiteSequence:0
					}
				for(x=0; x<scope.cycle.suites.length; x++) {
					var suite = scope.cycle.suites[x];
					for(var i=0; i<suite.labelAndValues.length; i++) {
						var lv = suite.labelAndValues[i];
						if(lv.elementType == 'SCREENSCRAPE') {
							if(lv.id && lv.id > 0) {
								// Only saved sceenscrapes will be available in the array.
								// This is because the id of the entity will be needed as a reference.
								scope.screenscrapes[scope.screenscrapes.length] = {
									id: lv.id,
									label: lv.label,
									sequence: lv.sequence,
									suiteId: suite.id,
									suiteName: suite.name,
									suiteSequence: suite.sequence
								};							
							}
						}
					}
				}
			};
			
			/**
			 * Get a string that will be displayed as a single picklist entry for screen scrapes.
			 */
			scope.getScreenScrapeInfo = function(ss) {
				if(ss.id <= 0) {
					return ss.label;
				}
				else {
					return ss.suiteName + ': ' + ss.sequence + ') id:' + ss.id + ', "' + ss.label + '"';
				}
			};
			
			/**
			 * A screenscrape picklist should not contain screen scrapes that appear AFTER the LabelAndValue
			 * entry the picklist applies to. Otherwise, the user would have the option to attempt applying a 
			 * screen scrape value to an element BEFORE it was "scraped".
			 * ==============================================================================================
			 * PARAMETERS:
			 * ==============================================================================================
			 *    screenscrape: the screenscrape that the filterable picklist item represents
			 *    suite: The suite that the item displaying the picklist belongs to.
			 *    lv: The LabelAndValue that is displaying the picklist
			 */
			scope.filterScreenScrapes = function(suite, lv) {
				return function(screenscrape) {
					if(screenscrape.suiteSequence > suite.sequence) {
						return false;
					}
					if(screenscrape.sequence > lv.sequence) {
						return false;
					}
					return true;					
				}
			};
			
			scope.resequence = function(items) {
				for(var i=0; i<items.length; i++) {
					items[i].sequence = (i+1);
				}
				scope.refreshScreenScrapes();
			};
			
			// Define an event handler to load in an empty default cycle object bound to a ng-repeat block for new cycles
			scope.newCycle = function() {
				cycleSvc.getCycle(null, scope.config.user.id).then(
					function(data) {
						scope.cycle = data;
						scope.cycle.suites[0].labelAndValues[0].checked = true;
					},
					function(error) {
						alert("Cycle retrieval error!\n" + error);
					}
				);
			};
			
			scope.backupCycle = function() {
				scope.cycle.kerberosLoginParms.configEnvironmentId = scope.config.currentEnvironment.id;
				scope.cycleBackup = angular.copy(scope.cycle);
				scope.refreshScreenScrapes();
			};
				
			scope.saveCycle = function() {
				cycleSvc.saveCycle(scope.cycle).then(
					function(serviceResponse) {
						//if(serviceResponse.message) {							
						//	alert(serviceResponse.message);
						//}
						scope.cycles = serviceResponse.data;					
						scope.cycle = '';
						scope.$apply;
					},
					function(serviceResponse) {
						if(serviceResponse != undefined && serviceResponse.message) {
							alert(serviceResponse.message);
						}
						else {
							alert('Cycle saving error:\n' + serviceResponse);
						}
					}
				);
			};
			
			/**
			 * Define an event handler to cancel the addition of a new cycle. It must restore the cycle that was being 
			 * edited from its backup copy made before the edits started (backup made on change event for the cycles listbox).
			 */
			scope.cancelNewCycle = function() {
				scope.cycle = '';
				if(scope.cycles && scope.cycles.length && scope.cycles.length > 0) {
					for(var i=0; i<scope.cycles.length; i++) {
						if(scope.cycles[i].id == scope.cycleBackup.id) {
							scope.cycles[i] = scope.cycleBackup;
							scope.cycleBackup = '';
							break;
						}
					}
				}				
			};
			
			scope.cloneCycle = function() {
				
			};
			
			scope.removeCycle = function() {
				cycleSvc.removeCycle(scope.cycle).then(
						function(serviceResponse) {
							scope.cycles = serviceResponse.data;					
							scope.cycle = '';
							scope.$apply;
						},
						function(serviceResponse) {
							if(serviceResponse != undefined && serviceResponse.message) {
								alert(serviceResponse.message);
							}
							else {
								alert('Cycle deletion error:\n' + serviceResponse);
							}
						}
					);
			};
			
			scope.launch = function(itemId, itemType) {
				if(!scope.cycle.kerberosLoginParms.username) {
					alert('REQUIRED: Weblogin username');
					return;
				}
				if(!scope.cycle.kerberosLoginParms.password) {
					alert('REQUIRED: Weblogin password');
					return;
				}
				cycleSvc.launch(scope.config.id, itemId, itemType, scope.cycle.kerberosLoginParms).then(
						function(serviceResponse) {
							alert(serviceResponse.message);					
						},
						function(serviceResponse) {
							if(serviceResponse != undefined && serviceResponse.message) {
								alert(serviceResponse.message + '\n\n' + serviceResponse.data);
							}
							else {
								alert('Launch error:\n' + serviceResponse);
							}
						}
					);
			};
			
			scope.getBlankObject = function(objectType) {
				var cycleTemplate = cycleSvc.getEmptyCycle(scope.config.user.id);
				switch(objectType) {
				case 'suite': return cycleTemplate.suites[0];
				case 'lv': return cycleTemplate.suites[0].labelAndValues[0];
				}
			};
			
			scope.addSuite = function(cycle, suiteIdx) {
				cycle.suites.splice(suiteIdx+1, 0, scope.getBlankObject('suite'));
				scope.resequence(cycle.suites);
				cycle.suites[suiteIdx+1].labelAndValues[0].checked = true;
			};
			
			scope.removeSuite = function(cycle, suiteIdx) {
				cycle.suites.splice(suiteIdx, 1); 
				scope.resequence(cycle.suites);
			};
			
			scope.addLabelAndValue = function(suite, lvIdx) {
				suite.labelAndValues.splice(lvIdx+1, 0, scope.getBlankObject('lv')); 
				scope.resequence(suite.labelAndValues);
				suite.labelAndValues[lvIdx+1].checked = true;
			};
				
			scope.removeLabelAndValue = function(suite, lvIdx) {
				suite.labelAndValues.splice(lvIdx, 1); 
				scope.resequence(suite.labelAndValues);
			};
			
			/**
			 * Condense all field values of a LabelAndValue to one line of text.
			 */
			scope.getLvLabel = function(lv) {
				var s = lv.sequence + ') ';
				s += (lv.id && lv.id > 0) ? 'id:' + lv.id : 'new';
				s += ', type:';
				var shortcut = '';
				var screenscrape = '';
				if(lv.elementType) {
					s += lv.elementType;
					if(lv.elementType == 'SHORTCUT') {
						if(lv.configShortcut) {
							var id = lv.configShortcut.id;
							shortcut = '[id=' + (id > 0 ? id : '?') + ']';
						}
						else {
							shortcut = '[id=?]';
						}
					}
				}
				else {
					s += '?';
				}
				if(shortcut) {
					s += (', shortcut:' + shortcut);
				}
				else if(lv.elementType == 'SCREENSCRAPE') {
					s += ', screenscrape type:';
					s += lv.screenScrapeType ? lv.screenScrapeType : '?';
					s += ', label:';
					s += lv.label ? lv.label : '?';
				}
				else {
					var val = lv.value;
					if(!val && lv.screenScrapeId == -1 && lv.dateUnits && lv.datePart) {
						val = lv.dateUnits + ' ' + lv.datePart.toLowerCase() + (/^\-/.test(lv.dateUnits) ? 's ago' : 's from now');
					}
					s += ', label:';
					s += lv.label ? lv.label : '?';
					s += ', value:';
					s += lv.value ? lv.value : (val ? val : '?');
					s += ', other identifier:';
					s += lv.identifier ? lv.identifier : '?';
				}
				s += ', navigates:';
				s += lv.navigates ? 'true' : 'false';
				
				return s;
			};
			
			/**
			 * Flatten out an array like ['a', 'b', 'c'] to 'a   >   b   >   c'
			 */
			scope.getHierarchyLabel = function(shortcut) {
				var s = shortcut.sequence + ') ' + 
				shortcut.elementType + ': ';
				var parts = shortcut.labelHierarchyParts;
				for(var i=0; i<parts.length; i++) {
					s += parts[i];
					if((i+1) < parts.length) {
						s += '   >   ';
					}
					else if(shortcut.identifier != null && shortcut.identifier != '') {
						s += ' : ["' + shortcut.identifier + '"]';
					}
				}
				return s;
			};
			
			scope.getCycleJson = function() {
				return 'CYCLE:\n' + angular.toJson(scope.cycle, true) + '\n\nCYCLES:\n' + angular.toJson(scope.cycles, true) + '\n\nBACKUP:\n' + angular.toJson(scope.cycleBackup, true);
			};
		}
	};
};