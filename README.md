# githubldap
Basic process for adding and deleting users from the Github control DL, Development-Tools-Access-Group

Usage: sonarqubeldap [-add textfile] [-del textfile] [-dump textfile] 
                      [-developers | -admins | -test | -dev ] [-newusersonly ]
					  [-log textfile] [-synch] [-h |-?]
	-add option will add users to DL for pmfkeys in textfile param.
	    if the extension is not ".csv", then the file should consist of one user per line in the format:
			<text> (<pmfkey)
			
		if the extension is ".csv", the input should be like:
			id,type,recurse
			Toolsadmin,user,no
			berot01,user,yes
			sayay01,user,yes
			
			where id is the "pmfkey" or "sAMAccountName", type is "user" or "group", and recurse is "yes" or "no"
			"recurse=yes" mean that the process will include the user and all direct reports (recursively) of this user
			
	-developers option (default) indicates the GHE production server
	-test option indicates the GHE test server
	-dev option indicates the GHE development server
	-admin option is for managing the "GIS - Github - Admins" DL.
	-newusersonly option will exclude from the resultant user container, all suspended users for the GHE server
	-del option will remove users from DL for pmfkeys in textfile param.  
	-dump option will dump user in DL to textfile param.
	-synch option will synchronize attribute data with directory.
	-log option specifies location log file.

Prerequisites (Environment):
	DL_ADMINISTRATOR_PASSWORD	AES Encrypted value for the DL owner (i.e. Toolsadmin@ca.com) password.
	Note: the following are used in conjunction with the -newusersonly option, so the GHE server can be accessed.
	GITHUB_ACCESS_TOKEN			API token for the GHE production system (option -developerws)
	GITHUB_DEV_ACCESS_TOKEN		API token for the GHE development system (option -dev)
	GITHUB_TEST_ACCESS_TOKEN	API token for the GHE test system (option -test)
	
	
Files:

GithubLdap.java				Class file implementing the administrative DL processes for Github Enterprise.
