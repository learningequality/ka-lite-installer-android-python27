import sys;

with open(sys.argv[1]) as data_file:    
    assessmentsjson = data_file.read()

def create_subfolder_name(s):
	first = str(ord(s[0]))
	second = str(ord(s[1]))
	return first + second + '/'

data = assessmentsjson.split("/content/khan/")

f = open('processed-assessmentitems.json', 'w')

for i, val in enumerate(data):
	if i > 0:
		val = create_subfolder_name(val) + val
	if len(data) > i + 1:
		val = val + 'file:///android_asset/khan/'
	# print '@@@@@@    ', val
	f.write(val)
	
f.close()