package githubldap;


import commonldap.CommonLdap;
import commonldap.JCaContainer;

import java.io.*;
import java.io.File; 


//Main Class
//@SuppressWarnings("unused")
public class githubldap 
{
	private static int iReturnCode = 0;
	private static CommonLdap frame;
	
	private static boolean bDump = false;
	private static String sDumpFile = "";
	
	private void githubldap()
	{
		// leave blank for now
	}

    
// main processing routine    
    
	private static void processLDAPGroupUsers(JCaContainer cLDAP,
										      JCaContainer cDLUsers,
			                                  JCaContainer cAddUsers, 
			                                  JCaContainer cDelUsers,
			                                  String sDLLDAPUserGroup,
			                                  String sAuthName) 
	{
		
		frame.printLog("Processing: " + sAuthName);
		
		try {
			boolean found=false;
			
			// 1. Remove DL users
			frame.printLog("1. Remove GitHub Users from DL");
			if (!cDelUsers.isEmpty())
			{
				for (int i=0; i<cDelUsers.getKeyElementCount("pmfkey"); i++ )
				{
					String sID = cDelUsers.getString("pmfkey", i);
					
					int iLDAP[] = cLDAP.find("sAMAccountName", sID);
					if (iLDAP.length > 0)
					{
						String sUser  = cLDAP.getString("displayName", iLDAP[0]);
						String userDN = cLDAP.getString("distinguishedName", iLDAP[0]);									
						
						// Force removal if a valid user in directory
						if (frame.removeUserFromLDAPGroup(sDLLDAPUserGroup, userDN))
						{
							frame.printLog(">>>User (deactivate): "+sUser+ "("+ sID+")");									
						}
					} // valid directory user

				}  //loop over user accounts						
			}	/* Delete List is not empty */
			
			// 2. Add users to DL
			frame.printLog("2. Add GitHub Users to DL");
			if (!cAddUsers.isEmpty())
			{
				for (int i=0; i<cAddUsers.getKeyElementCount("pmfkey"); i++ )
				{					
					String sID = cAddUsers.getString("pmfkey", i);
					
					int iLDAP[] = cLDAP.find("sAMAccountName", sID);
					if (iLDAP.length > 0)
					{
						String sUser  = cLDAP.getString("displayName", iLDAP[0]);
						String userDN = cLDAP.getString("distinguishedName", iLDAP[0]);									
						
						int iUser[] = cDLUsers.find("dn", userDN);
						
						if (iUser.length == 0) {
							if (frame.addUserToLDAPGroup(sDLLDAPUserGroup, userDN))
							{
								// Add user to LDAP DLUser group
								frame.printLog(">>>User (activate): "+sUser+ "("+ sID+")");											
							}							
						} // user not found in DL 
					} //  user in directory 
				}  // loop over user accounts						
            } // Add list is not empty 	

			// 3. Dump Request
			frame.printLog("3. Dump User DL");
			if (bDump)
			{

				File file = new File(sDumpFile);

				// if file doesnt exists, then create it
				if (!file.exists()) {
					file.createNewFile();
				}

				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				
				int nSize = cDLUsers.getKeyElementCount("dn");
				if (nSize < 1500) {					
					for (int i=0; i<nSize; i++ )
					{
						String sDN = cDLUsers.getString("dn", i);
						int iLDAP[] = cLDAP.find("distinguishedName", sDN);
						if (iLDAP.length > 0)
						{
							String sUser = cLDAP.getString("displayName", iLDAP[0]);
							String sID   = cLDAP.getString("sAMAccountName", iLDAP[0]);									
						    //printLog(sUser+ " ("+ sID+")");	
							bw.write(sUser+ " ("+ sID+")\n");
						} // user exists in domain
					}  // loop over DL members						
				} // DL size does not exceed 1499
				else {
					for (int i=0; i<cLDAP.getKeyElementCount("sAMAccountName"); i++) {
						String sID    = cLDAP.getString("sAMAccountName", i);	
						String sUser  = cLDAP.getString("displayName", i);
						String userDN = cLDAP.getString("distinguishedName", i);
						
						int iUser[]=cDLUsers.find("dn", userDN);
						if (iUser.length > 0)
						{
							bw.write(sUser + " ("+ sID + ")\n");
						}
						else {
							if (frame.addUserToLDAPGroup(sDLLDAPUserGroup, userDN)) {
								frame.removeUserFromLDAPGroup(sDLLDAPUserGroup, userDN);
							}
							else {
								bw.write(sUser + " ("+ sID + ")\n");								
							}
						}
					}
				} // DL size exceeds 1499
				bw.close();
			} /* Dump Users */	
		} /* try block */
		catch (Throwable e) {
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}
		finally { }
	} // end ProcessDB2Database

	
	public static void main(String[] args)
	{
		int iParms = args.length;
		String sBCC = "";
		String sLogPath = "scmldap.log";
		String sAddFile = "";
		String sDelFile = "";
		int iDLType = 0;

		int dojts = -1;
		boolean bRepairLicenses = false;
		
		String[] aGitHubAdminLDAPGroupFormat = 
	  	{ 	
			"cn=Team - GitHub - %s,ou=groups,ou=north america"
	  	};
		
		String[] aGitHubDevelopersLDAPGroupFormat = 
	  	{ 	
			"cn=Development-Tools-Access-Group,ou=groups,ou=north america"
	  	};

		String[][] aDLLDAPGroupFormat =
		{
			aGitHubDevelopersLDAPGroupFormat,
			aGitHubAdminLDAPGroupFormat
		};
				
		String[] aGitHubUserAuthAdminSchemas =
		{
			"Admins" 
		};
		
		String[] aGitHubUserAuthDeveloperSchemas =
		{
			"Developers" 
		};

		
		String[][] aAuthSchemas =
		{
				aGitHubUserAuthDeveloperSchemas,
				aGitHubUserAuthAdminSchemas
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
				sAddFile = args[++i];
			}			
			else if (args[i].compareToIgnoreCase("-dump") == 0 )
			{
				sDumpFile = args[++i];
				bDump = true;
			}			
			else if (args[i].compareToIgnoreCase("-log") == 0 )
			{
				sLogPath = args[++i];
			}	
			else if (args[i].compareToIgnoreCase("-developers") == 0 )
			{
				iDLType = 0;
			}	
			else if (args[i].compareToIgnoreCase("-admins") == 0 )
			{
				iDLType = 1;
			}	
			
			else {
				System.out.println("Usage: githubldap [-add textfile]"
			                                       + " [-del textfile] "
			                                       + " [-dump textfile] "
			                                       + " [ -developers | -admins ] "
						                           + " [-log textfile] [-h |-?]");
				System.out.println(" -add option will add users to DL for pmfkeys in textfile param.");
				System.out.println(" -del option will remove users from DL for pmfkeys in textfile param.");
				System.out.println(" -dump option will dump user in DL to textfile param.");
				System.out.println(" -admins option will select Team-GitHub-Admins DL.");
				System.out.println(" -developers option will select Developer-Tools-Access-Group DL (default).");
				System.out.println(" -log option specifies location log file.");
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
			

			// Read DL LDAP group users
			for (int i=0; i<aAuthSchemas[iDLType].length; i++)
			{
				String[] aDLLDAPGroup = new String[aDLLDAPGroupFormat[iDLType].length];
				
				for (int j=0; j<aDLLDAPGroupFormat[iDLType].length; j++)
				{
					String sDLLDAPUserGroup = aDLLDAPGroupFormat[iDLType][j].replaceAll("%s", aAuthSchemas[iDLType][j]);
					//String sDLLDAPUserGroup = aDLLDAPGroup[j].format(aDLLDAPGroupFormat[iDLType][j],aAuthSchemas[iDLType][j]);
					
					frame.readLDAPUserGroupToContainer(sDLLDAPUserGroup, cDLUsers[i]);
					
					processLDAPGroupUsers(cLDAP,
						                  cDLUsers[i],
                                          cAddUsers, 
                                          cDelUsers,
                                          sDLLDAPUserGroup,
                                          aAuthSchemas[iDLType][j]);
				}
			}
								
		} catch (Exception e) {
			iReturnCode = 1;
		    System.err.println(e);			
			//e.printStackTrace();
		    System.exit(iReturnCode);		    
		}

		// exit application
		System.exit(iReturnCode);	

	} // end main
}
