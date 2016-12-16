package githubldap;

import commonldap.CommonLdap;
import commonldap.JCaContainer;


//Main Class
//@SuppressWarnings("unused")
public class GithubLdap 
{
	private static int iReturnCode = 0;
	private static CommonLdap frame;
	
	GithubLdap()
	{
		// leave blank for now
	}

    
// main processing routine    
	public static void main(String[] args)
	{
		int iParms = args.length;
		String sBCC = "";
		String sLogPath = "scmldap.log";
		String sAddFile = "";
		String sDelFile = "";
		boolean bSynch = false;
		int iUserType = 0;
		String sDumpFile = "";
		
		
		String[] aAdminLDAPGroupFormat = 
	  	{ 	
			"cn=Team - GitHub - %s,ou=groups,ou=north america"
	  	};
		
		String[] aDevelopersLDAPGroupFormat = 
	  	{ 	
			"cn=Development-Tools-Access-Group,ou=groups,ou=north america"
	  	};

		String[][] aDLLDAPGroupFormat =
		{
			aDevelopersLDAPGroupFormat,
			aAdminLDAPGroupFormat
		};
				
		String[] aUserAuthAdminSchemas =
		{
			"Admins" 
		};
		
		String[] aUserAuthDeveloperSchemas =
		{
			"Developers" 
		};

		
		String[][] aAuthSchemas =
		{
				aUserAuthDeveloperSchemas,
				aUserAuthAdminSchemas
		};
		
		
		// check parameters
		for (int i = 0; i < iParms; i++)
		{					
			if (args[i].compareToIgnoreCase("-add") == 0 )
			{
				sAddFile = args[++i];
			}			
			else if (args[i].compareToIgnoreCase("-del") == 0 )
			{
				sDelFile = args[++i];
			}			
			else if (args[i].compareToIgnoreCase("-synch") == 0 )
			{
				bSynch = true;
			}			
			else if (args[i].compareToIgnoreCase("-dump") == 0 )
			{
				sDumpFile = args[++i];
			}			
			else if (args[i].compareToIgnoreCase("-log") == 0 )
			{
				sLogPath = args[++i];
			}	
			else if (args[i].compareToIgnoreCase("-developers") == 0 )
			{
				iUserType = 0;
			}	
			else if (args[i].compareToIgnoreCase("-admins") == 0 )
			{
				iUserType = 1;
			}	
			
			else {
				frame.printLog("Usage: githubldap [-add textfile]"
			                                       + " [-del textfile] "
			                                       + " [-dump textfile] "
			                                       + " [ -developers | -admins ] "
						                           + " [-log textfile] [-h |-?]");
				frame.printLog(" -add option will add users to DL for pmfkeys in textfile param.");
				frame.printLog(" -del option will remove users from DL for pmfkeys in textfile param.");
				frame.printLog(" -dump option will dump user in DL to textfile param.");
				frame.printLog(" -admins option will select Team-GitHub-Admins DL.");
				frame.printLog(" -developers option will select Developer-Tools-Access-Group DL (default).");
				frame.printLog(" -log option specifies location log file.");
				System.exit(iReturnCode);
			}
		} // end for

		JCaContainer cDLUsers[] = { 
		  	new JCaContainer(),
			new JCaContainer()
		};

		JCaContainer cAddUsers = new JCaContainer();
		JCaContainer cDelUsers = new JCaContainer();
		
		JCaContainer cLDAP = new JCaContainer();
		frame = new CommonLdap("githubldap",
		           sLogPath,
		           sBCC,
		           cLDAP);

		if (!sAddFile.isEmpty())
			frame.readUserListToContainer(cAddUsers, sAddFile);
		if (!sDelFile.isEmpty())
			frame.readUserListToContainer(cDelUsers, sDelFile);

		try {			
			frame.processStandardDL(aAuthSchemas,
					                aDLLDAPGroupFormat, 
					                cLDAP, 
					                cDLUsers, 
					                cAddUsers, 
					                cDelUsers, 
					                iUserType,
					                sDumpFile,
					                bSynch);
								
		} catch (Exception e) {
			iReturnCode = 1;
		    frame.printErr(e.getLocalizedMessage());			
			//e.printStackTrace();
		    System.exit(iReturnCode);		    
		}

		// exit application
		System.exit(iReturnCode);	

	} // end main
}
