import os, glob, sys, shutil

#changes the current working directory to the given path in command line
os.chdir(sys.argv[1])

count = 0
totalfiles = 0
increment = 48
folders = 0

while increment < 129:
	if increment != 63: # this is an odd character
		first_char = (chr(increment))
		inner_increment = 48
		while inner_increment < 129:
			if inner_increment != 63: # this is an odd character
				second_char = (chr(inner_increment))
				for file in glob.glob(first_char + second_char + "*.*"):
					count += 1
					# print file
				if count != 0:
					folders += 1
					#because Mac file system does not distinguash upper and lower case
					path_upper = os.path.normpath(os.path.join(sys.argv[1], ".."))
					folder = path_upper + '/processed_khan22/' + str(ord(first_char)) + str(ord(second_char))
					os.makedirs(folder)
					print "total for " + first_char + second_char + ": ", count
					#put the files in its folder
					for file in glob.glob(first_char + second_char + "*.*"):
						shutil.copy(file, folder)

				totalfiles += count
				count = 0
			inner_increment += 1
	increment += 1

print "\ntotal files: ", totalfiles
print "\ntotal folders: ", folders