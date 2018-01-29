package commonldap.githubldap;

import java.util.*;

import commonldap.commonldap.CommonLdap;
import commonldap.commonldap.JCaContainer;


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
		int iUserType = 0;

		String sBCC = "";
		String sLogPath = "scmldap.log";
		String sAddFile = "";
		String sDelFile = "";
		String sDumpFile = "";

		boolean bSynch = false;
		boolean bNewUsersOnly = false;
		
		
		String[] aDevelopersProdLDAPGroupFormat = 
	  	{ 	
			"cn=Development-Tools-Access-Group,ou=groups,ou=north america"
	  	};
		
		String[] aAdminLDAPGroupFormat = 
	  	{ 	
			"cn=Team - GitHub - %s,ou=groups,ou=north america"
	  	};

		String[] aDevelopersTestLDAPGroupFormat = 
	  	{ 	
			"cn=Team - GIS - Development-Tools-Access-Group-Test,ou=self service groups,ou=groups"
	  	};

		String[] aDevelopersDevLDAPGroupFormat = 
	  	{ 	
			"cn=Team - GIS - Development-Tools-Access-Group-Dev,ou=self service groups,ou=groups"
	  	};

		String[][] aDLLDAPGroupFormat =
		{
			aDevelopersProdLDAPGroupFormat,
			aAdminLDAPGroupFormat,
			aDevelopersTestLDAPGroupFormat,
			aDevelopersDevLDAPGroupFormat
		};
				
		String[] aUserAuthAdminSchemas =
		{
			"GHE Administrators" 
		};
		
		String[] aUserAuthDeveloperSchemas =
		{
			"GHE Production Developers" 
		};

		String[] aUserAuthTestSchemas =
		{
			"GHE Test Developers" 
		};

		String[] aUserAuthDevSchemas =
		{
			"GHE Development Developers" 
		};

		
		String[][] aAuthSchemas =
		{
				aUserAuthDeveloperSchemas,
				aUserAuthAdminSchemas,
				aUserAuthTestSchemas,
				aUserAuthDevSchemas
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
			else if (args[i].compareToIgnoreCase("-newusersonly") == 0 )
			{
				bNewUsersOnly = true;
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
			else if (args[i].compareToIgnoreCase("-test") == 0 )
			{
				iUserType = 2;
			}	
			else if (args[i].compareToIgnoreCase("-dev") == 0 )
			{
				iUserType = 3;
			}	
			
			else {
				frame.printLog("Usage: githubldap [-add textfile]"
				                               + " [-del textfile] "
				                               + " [-dump textfile] "
				                               + " [-developers | -admins | -test | -dev ] "
				                               + " [-newusersonly] "
					                           + " [-log textfile ] [ -h | -?]");
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
		JCaContainer cGHEUsers = new JCaContainer();
		
		JCaContainer cLDAP = new JCaContainer();
		frame = new CommonLdap("githubldap",
		           sLogPath,
		           sBCC,
		           cLDAP);
		
		
		try {
	        if (bNewUsersOnly) {
				String sAccessToken="";
				String sAPIToken = "";
				String sType = "ghe";
				
				
				switch (iUserType) {
				case 0:
				case 1:
					sAPIToken="GITHUB_ACCESS_TOKEN";
					sType = "ghe";
				default:
					break;
				case 2:
					sAPIToken="GITHUB_TEST_ACCESS_TOKEN";
					sType = "ghe-test";
					break;
				case 3:
					sAPIToken="GITHUB_DEV_ACCESS_TOKEN";
					sType = "ghe-dev";
					break;
				}
	        	
				Map<String, String> environ = System.getenv();
		        for (String envName : environ.keySet()) {
		        	if (envName.equalsIgnoreCase(sAPIToken))     
		        		sAccessToken = environ.get(envName);
		        }
		        
		        //frame.readGitHubInstanceUsers(cGHEUsers, sAccessToken, sType);
		        frame.readGitHubSuspendedUsers(cGHEUsers, sAccessToken, sType);
	        }
	        
	        
			if (!sAddFile.isEmpty()) {
				if (sAddFile.contains(".csv")) {
					JCaContainer cAddUsers2 = new JCaContainer();
					frame.readInputListGeneric(cAddUsers2, sAddFile,',');
					for (int iIndex=0; iIndex<cAddUsers2.getKeyElementCount("id"); iIndex++) {
						frame.readLDAPEntry(cAddUsers, 
										    cLDAP,
											cAddUsers2.getString("id", iIndex),
											cAddUsers2.getString("type",iIndex).equalsIgnoreCase("group"),
											cAddUsers2.getString("recurse", iIndex).equalsIgnoreCase("yes"),
											false,
											cGHEUsers);					
					}
				}
				else
					frame.readUserListToContainer(cAddUsers, sAddFile);	
			}
			
	
			if (!sDelFile.isEmpty()) {
				if (sDelFile.contains(".csv")) {
					JCaContainer cDelUsers2 = new JCaContainer();
					frame.readInputListGeneric(cDelUsers2, sDelFile,',');
					for (int iIndex=0; iIndex<cDelUsers2.getKeyElementCount("id"); iIndex++) {
						frame.readLDAPEntry(cDelUsers, 
										    cLDAP,
											cDelUsers2.getString("id", iIndex),
											cDelUsers2.getString("type",iIndex).equalsIgnoreCase("group"),
											cDelUsers2.getString("recurse", iIndex).equalsIgnoreCase("yes"),
											false,
											cGHEUsers);					
					}
				}
				frame.readUserListToContainer(cDelUsers, sDelFile);
			}

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
