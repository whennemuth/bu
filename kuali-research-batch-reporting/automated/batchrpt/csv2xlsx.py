import ast
import os
import glob
import csv
from xlsxwriter.workbook import Workbook
from xlsxwriter.utility  import xl_col_to_name

# Check if sting value is a number
def containsNumber(s):
	try:
		float(s)
		return True
	except ValueError:
		return False

# Convert CSV file number of rows/cols into an Excel range (eg. A1:H42)
def cellRange(lastRow, lastCol):
	ExcelCol = xl_col_to_name(lastCol)  # translate col nbr to letter value for Excel Column
	ExcelRow = str(lastRow)             # translate row nbr to char value of nbr for Excel Row
	return 'A1:' + ExcelCol + ExcelRow

# Convert header names to xlsxWriter options for adding an Excel table
def headerOptions(colHead):
	strHead = "'}, {'header': '".join(str(e) for e in colHead)
	return (ast.literal_eval("{'columns': [{'header': '" + strHead + "'},]}"))

# Convert one or more CSVs to an Excel file with one worksheet per CSV file.
def convertCsvToExcel(csvFiles, tabNames, excelFile):
	print("======= Converting {ef} to csv...".format(ef=excelFile))

	if os.path.exists(excelFile):
		os.remove(excelFile)

	workbook = Workbook(excelFile)

	rightJustify = workbook.add_format()
	rightJustify.set_align('right')

	for cv, tb in zip(csvFiles, tabNames):
		print("       Adding worksheet {tb}".format(tb=tb))
		worksheet = workbook.add_worksheet(tb)
		with open(cv, 'rU') as f:
			reader = csv.reader(f)
			headerRow = next(reader)  # get column headers in first row of CSV
			f.seek(0)  # reset file pointer to top
			for n, colName in enumerate(headerRow):
				worksheet.set_column(n,n,len(colName) + 4)  # set width of each col to nbr of chars of header plus a margin
			for r, row in enumerate(reader):
				for c, col in enumerate(row):
#					cell = col.decode('latin-1')
					cell = col
					if containsNumber(cell):
						worksheet.write(r, c, cell, rightJustify)
					else:
						worksheet.write(r, c, cell)
		if r == 0: # If there is a header without any detail, generate an empty detail row to satisfy the xlsxwriter utility
			r = r + 1
			for c, col in enumerate(headerRow):
				worksheet.write(r, c, None)
		worksheet.add_table(cellRange(r + 1, c), headerOptions(headerRow)) # Generate Excel table and add 1 to rows to account for header

	workbook.close() 