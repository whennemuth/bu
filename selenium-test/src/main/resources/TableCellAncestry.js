/**
 * Given a particular web element (node), return an object that represents the html table that
 * the web element is expected to be a descendent of. A list of table cells is built for this
 * object, one cell for the containing td/th element that contains the web element and all
 * additional 'outer' cells where there is a nested table hierarchy. NOTE: a "descendent" is not
 * necessarily a descendent of an ancestor, but will be a descendent of the row of that ancestor.
 * 
 * @param node The web element contained in a table cell.
 * @param cells The list of table cells that will be built.
 * @param depth How many jumps does it take up the document hierarchy to go from node to document root.
 * @returns
 */
function getAncestorTableCells(node, cells, depth) {
	if(node) {
		if(isCell(node)) {
			var table = getTable(node, depth);
			var tableTag = getFullTag(table);
			var id = getAttribute(node, "id");
			var tag = getFullTag(node);
			var first100Chars = getFirst100Chars(node);
			if(table) {
				cells[cells.length] = {
					id: id, 
					cell: node,
					depth: depth,
					x: node.cellIndex + 1,
					y: node.parentNode.rowIndex + 1,
					table: table,
					tableRows: table.rows.length,
					tableCols: table.rows[0].cells.length,
					tableTag: tableTag,
					tag: tag,
					first100Chars: first100Chars
				};
			}
		}
		return getAncestorTableCells(node.parentNode, cells, --depth);
	}			
	return cells;
}

/**
 * For each web element in the provided list, assume they each exist in the same table row and 
 * return that table row. This search accomodates iteration through a nested table hierarchy to
 * get the innermost table row that all the elements have in common.
 * 
 * @param nodes any set of web elements
 * @returns The table row or null if any of the nodes does not share the same row with any of the others
 */
function getCommonAncestorRow(nodes) {
	if(arguments.length == 1) {
		// First call to this function. Iterate over the whole array of nodes.
		if(nodes && nodes.length > 0) {
			if(nodes.length == 1) {
				return getRow(nodes[0]);
			}
			else if(nodes.length > 1) {
				var rows = [];
				var node1 = nodes[0];
				var remain = nodes.splice(1, nodes.length);
				for(var i=0; i<remain.length; i++) {
					var row = getCommonAncestorRow(node1, remain[i]);
					if(row == null) {
						return null;
					}
					rows[rows.length] = row;
				}	
				rows.sort(function(row1, row2) {
					// One row is a parent of another, so the parent will have larger innerHtml
					return row1.innerHTML.length < row2.innerHTML.length;
				});
				return rows[0];
			}
		}
		return null;		
	}
	else {
		// Recursive call to this function. It has narrowed work between only 2 nodes out of the initial array.
		var node1 = arguments[0];
		var node2 = arguments[1];
		
		if(node1 == null || node2 == null) {
			return null;
		}
		if(node1 === node2) {
			return node1;
		}
		
		var originalNode2 = arguments[2] ? arguments[2] : node2;
		
		if(isRow(node1)) {
			var row1 = node1;
			var row2 = getRow(node2.parentNode);
			if(row2 == null) {
				row1 = getRow(row1.parentNode);
				if(row1 != null) {
					row2 = getRow(originalNode2);
				}			
			}

			return getCommonAncestorRow(row1, row2, originalNode2); 
		}
		else {
			var row1 = getRow(node1.parentNode);
			var row2 = getRow(node2.parentNode);
			return getCommonAncestorRow(row1, row2, originalNode2);
		}
	}
}

/**
 * Object that represents a cell in a table column that is a parent, grandparent, etc. to a web element (originalField)
 */
function cellObject(originalField) {
	this.originalField = originalField;
	this.cell = getCell(originalField);
	this.sameColumn = function(othercell) {
		if(!this.cell || !othercell || !othercell.cell)
			return false;
		return this.cell.cellIndex == othercell.cell.cellIndex;
	};
	this.getDepth = function() {
		return getDepth(this.cell);
	};
	this.deeperThan = function(othercell) {
		if(!this.cell || !othercell)
			return false;
		return this.getDepth() > othercell.getDepth();
	};
	this.sameTable = function(othercell) {
		if(!this.cell || !othercell)
			return false;
		var table1 = getTable(this.cell);
		var table2 = getTable(othercell.cell);
		return table1 === table2;
	}
	this.nextCell = function() {
		if(this.cell) {
			this.cell = getParentCell(this.cell);
		}		
		return this.cell != null;
	}
	this.rowIndex = function() {
		return this.cell.parentNode.rowIndex;
	}
	this.clone = function() {
		// Most of the time you would want to produce a clone before the nextCell function
		// is called to get a clone of the instance as it was when it was first created.
		cellObject = new cellObject(originalField);
	}
}

function cloneCellObjectArray(array) {
	if(!array)
		return null;
	if(!Array.isArray(array)) 
		return cloneCellObjects([array]);
	
	var clones = [];
	for(var i=0; i<array.length; i++) {
		clones[clones.length] = new cellObject(array[i].originalField);
	}
	return clones;
}

/**
 * Given a particular web element that is in a table, select out of a list of other web elements those that are
 * in the same column of the table. If no matches are found for the immediate parent column, look for an outer table
 * and repeat the same check for the "grandparent" column. Repeat this until a match is found or the document root is reached.
 * 
 * @param node
 * @param nodes
 */
function getNodesInSameColumn(node, nodes) {
	
	// If this is the first call, node will be a web element. Wrap it in an object that will carry it through recursive calls.
	var cell = node;
	var cells = nodes;
	var exit = false;
	
	if(!node.originalField) {
		cell = new cellObject(node);
		for(var i=0; i<nodes.length; i++) {
			cells[i] = new cellObject(nodes[i]);
		}
	}
	else {
		var ascend = true;
		var fieldsAbove = false;
		for(var i=0; i<cells.length; i++) {
			var othercell = cells[i];
			if(othercell.cell && othercell.deeperThan(cell)) {
				if(othercell.nextCell()) {
					ascend = false;
				}
			}
			else if(!cell.sameTable(othercell)) {
				fieldsAbove = true;
			}
		}
		
		if(ascend) {
			if(fieldsAbove) {
				cell.nextCell();
			}
			else {
				exit = true;
			}
		}		
	}

	var filtered = [];
	if(cell.cell) {
		for(var i=0; i<cells.length; i++) {
			var othercell = cells[i];
			if(cell.sameColumn(othercell)) {
				if(cell.sameTable(othercell)) {
					filtered[filtered.length] = othercell;
				}
			}
		}
		if(filtered.length == 0 && !exit) {
			return getNodesInSameColumn(cell, cells);
		}
	}
	
	return filtered;
}

/**
 * 
 * @param label
 * @param nodes
 */
function getSameColumnNodeObjects(label, nodes) {
	var objects = [];
	var filtered1 = getNodesInSameColumn(label, nodes);
	var filtered2 = cloneCellObjectArray(filtered1);
	var labelLevelCells = getCellsInSharedColumn([label].concat(filtered1));
	var otherLevelCells = getCellsInSharedColumn(filtered2);
	var labelCell = new cellObject(label);
	
	for(var i=0; i<otherLevelCells.length; i++) {
		var cellinfo = otherLevelCells[i];
		objects[objects.length] = {
			originalField: otherLevelCells[i].originalField,
			commonColumnIndex: otherLevelCells[i].cell.cellIndex,
			rowIndex: otherLevelCells[i].cell.parentNode.rowIndex,
			labelColumnIndex: labelCell.cell.cellIndex,
			labelRowIndex: labelCell.cell.parentNode.rowIndex,
			labelLevelCell: labelLevelCells[0].cell,
			labelLevelColumnIndex: labelLevelCells[0].cell.cellIndex,
			labelLevelRowIndex: labelLevelCells[i+1].cell.parentNode.rowIndex,
			isLabel: false
		};
	}
	
	return objects;
}

/**
 * Given two or more web elements, recurse up the html hierarchy toward the table column 
 * they all share in common and return meta data objects for the cells they each occupy in that column.
 */
function getCellsInSharedColumn(nodes) {
	if(!Array.isArray(nodes)) {
		return getCellsInSharedColumn([nodes]);
	}
	if(nodes.length == 0) {
		return [];
	}
	nodes.forEach(function(node, index, array) {
		if(!node.originalField) {
			array[index] = new cellObject(node);
		}
	});
		
	if(nodes.length > 1) {
		for(;;) {
			nodes.forEach(function(node, index, array) {
				if(!node)
					return;
				var othernode = array[(index + 1) < array.length ? (index + 1) : (index - 1)];
				if(!othernode)
					return;
				if(node.sameTable(othernode))
					return;
				if(node.deeperThan(othernode))
					node.nextCell();
				else if(othernode.deeperThan(node))
					othernode.nextCell();
			});		
			
			if(nodes.every(function(node, index, array) {
				var othernode = array[(index + 1) < array.length ? (index + 1) : (index - 1)];
				if(!node || !othernode)
					return true;
				return node.sameTable(othernode);
			})) {
				break;
			}
		}
	}
	
	return nodes;
}

function getAttribute(node, attributeName) {
	if(!node)
		return null;
	//if(node.hasAttributes()) { // causes error
		var attrs = node.attributes;
		for(var i=0; i<attrs.length; i++) {
			if(attrs[i].name == attributeName) {
				return attrs[i].value;
			}
		}
	//}
	return null;
}

/**
 * @returns How many jumps does it take up the document hierarchy to go from node to document root.
 */
function getDepth(node, depth) {
	if(node) {
		if(depth == undefined) 
			return getDepth(node.parentNode, 0);
		else
			return getDepth(node.parentNode, ++depth);
	}
	return depth;
}

/**
 * @returns The first html table ancestor of a node
 */
function getTable(node, depth) {
	if(node) {
		if(node.nodeName.toLowerCase() == 'table') {
			node.setAttribute('depth', depth);
			return node;
		} 		
		return getTable(node.parentNode, --depth);
	}
	return null;
}

/**
 * @returns The first html row ancestor of node
 */
function getRow(node) {
	if(node) {
		if(node.nodeName.toLowerCase() == 'tr') {
			return node;
		} 		
		return getRow(node.parentNode);
	}
	return null;
}

function getCell(node) {
	if(node) {
		if(node.nodeName.toLowerCase() == 'td' || node.nodeName.toLowerCase() == 'th') {
			return node;
		} 		
		return getCell(node.parentNode);
	}
	return null;	
}

function getParentCell(node) {
	if(node) {
		return getCell(node.parentNode);
	}
	return null;
}

/**
 * @returns The first html cell ancestor of node that is at the specified depth beneath the document root.
 */
function getAncestorCellAtDepth(node, depth) {
	if(node) {
		if(node.nodeName.toLowerCase() == 'td' || node.nodeName.toLowerCase() == 'th') {
			var nodeDepth = getDepth(node) + 0;
			if(nodeDepth < depth) {
				// node is alread at a "shallower" depth than depth, and this is an invalid condition.
				return null;
			}
			if(nodeDepth == depth) {
				node.setAttribute("columnIndex", node.cellIndex + 1)
				return node;
			}
		} 		
		return getAncestorCellAtDepth(node.parentNode, depth);
	}
	return null;
}

/**
 * @returns True if a web element is either a table cell or table header cell.
 */
function isCell(node) {
	if(!node)
		return false;
	if(!node.nodeName)
		return false;
	if(node.nodeName.toLowerCase() == 'td')
		return true;
	if(node.nodeName.toLowerCase() == 'th')
		return true;
	return false;
}

/**
 * @returns True if a web element is a table row (tr).
 */
function isRow(node) {
	return node && node.nodeName && node.nodeName.toLowerCase() == 'tr';
}

/**
 * Get the full content within the opening tag of a node.
 * @param node
 * @returns
 */
function getFullTag(node) {
	if(!node.outerHTML)
		return null;
	if(!node.innerHTML)
		return node.outerHTML;
		
	return node.outerHTML.slice(0, node.outerHTML.indexOf(node.innerHTML));
}

function getFirst100Chars(node) {
	if(!node.outerHTML)
		return null;
	var html = removeEmptyLines(node.outerHTML);
	if(html.length <= 100)
		return html;
	if(html.length > 100) {
		return html.substr(0, 100) + ' [more...]';
	}
}

function removeEmptyLines(content) {
	if(content) {
		content = content.replace(/(((\r\n)|\n)+[\x20\t]*){2,}/, '\n');
	}
	return content;
}

/**
 * Get a copy of an array with the item at the specified index removed.
 */
Array.prototype.copyWithout = function(index) {
	return this.clone().splice(index, 1);
};

Array.prototype.clone = function() {
	return this.slice(0);
}

function doTest() {
	var doc = getDocumentObj(false);
	var node = doc.evaluate("//th[text()='Return Value']", doc, null, XPathResult.ANY_TYPE, null).iterateNext();
	var iter = doc.evaluate("//a[text()='return value']", doc, null, XPathResult.ANY_TYPE, null);
	var tag = iter.iterateNext();	
	var nodes = [];
	while(tag) {
		nodes[nodes.length] = tag;
		tag = iter.iterateNext();
	}
	
	var results = callOperator(['column', node, nodes]);
	//alert(results);
	return results;
}

function getDocumentObj(fromFrame) {
	if(fromFrame) {
		// This won't work in chrome because it doesn't allow frames from your hard disk to access each others' 
		// content. Which, technically we term as Cross-origin request. Use firefox instead or put the <script>
		// element inside the document the frame sources.
		var frame = window.frames[0];
		var doc = (frame.contentWindow || frame.contentDocument);
		if(doc.document) doc = doc.document;
		return doc;
	}
	return window.document;
}

/**
 * This function acts as a switchboard operator to connect the "caller" (selenium JavascriptExecutor.executeScript())
 * to the right functionality based on the arguments parameter array passed in. The JavascriptExecutor wraps the entire
 * contents of this file in a function that it calls, passing the parameters provided in executeScript().
 * @param args
 * @returns
 */
function callOperator(args) {
	if(!args || args.length == 0) {
		return;
	}
	var task = args[0];
	
	if(task == 'cell') {
		var node = args[1];
		var cellInfo = getAncestorTableCells(node, [], getDepth(node));
		cellInfo.node = node;
		return cellInfo;
	}
	else if(task == 'row') {
		var nodes = args[1];
		var compareInfo = getCommonAncestorRow(nodes);
		return compareInfo;
	}
	else if(task == 'ancestorcell') {
		var cell = args[1];
		var depth = args[2];
		var ancestor = getAncestorCellAtDepth(cell, depth);
		return ancestor;
	}
	else if(task == 'column') {
		var node = args[1];
		var nodes = args[2];
		var filtered = getSameColumnNodeObjects(node, nodes);
		return filtered;
	}
}

if(arguments) {
	return callOperator(arguments);
}


