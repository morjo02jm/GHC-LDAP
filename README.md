# githubldap
Basic process for adding and deleting users from the Github control DL, Development-Tools-Access-Group

Usage: sonarqubeldap [-add textfile] [-del textfile]  [-dump textfile] [-log textfile] [-synch] [-h |-?]
	-add option will add users to DL for pmfkeys in textfile param.
	-del option will remove users from DL for pmfkeys in textfile param.
	-dump option will dump user in DL to textfile param.
	-synch option will synchronize attribute data with directory.
	-log option specifies location log file.

Prerequisites (Environment):
	DL_ADMINISTRATOR_PASSWORD	AES Encrypted value for the DL owner (i.e. Toolsadmin@ca.com) password.
	
Files:

GithubLdap.java				Class file implementing the administrative DL processes for Github Enterprise.
