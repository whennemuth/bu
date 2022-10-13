from datetime import datetime
import os

# Append current date to base file name (ex. my_file.xlsx becomes my_file_2021_04_30.xlsx).  
def appendDateToFile(filename):
	base_ext = os.path.splitext(filename)  # create list of 2 elements; file base & extension
	now = datetime.now()

	filename_with_date = base_ext[0] + now.strftime('_%Y_%m_%d') + base_ext[1]
	
	return filename_with_date
