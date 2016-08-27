package githubldap;


import com.ca.harvest.jhsdk.*;
import com.ca.harvest.jhsdk.hutils.*;
import com.ca.harvest.jhsdk.logger.*;

import java.sql.*;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.*;

import java.io.*;
import java.io.BufferedReader; 
import java.io.File; 
import java.io.FileReader; 
import java.io.FileNotFoundException; 
import java.io.IOException; 

import javax.naming.*;
import javax.naming.directory.*;

import com.ibm.team.repository.client.ITeamRepositoryService;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.client.internal.TeamRepository;

import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IDefaultLicenseProviderService;
import com.ibm.team.repository.common.ILicenseAdminService;


import com.ibm.team.repository.transport.client.*;
import com.ibm.team.repository.transport.client.AuthenticationException;

//import com.ibm.team.repository.service.internal.license.*;

import org.eclipse.core.runtime.*;

import junit.framework.*;


//Main Class
@SuppressWarnings("unused")
public class githubldap extends TestCase implements IJCaLogStreamListener 
{
	String LogName = "githubldap.log";
	static int iReturnCode = 0;
	
	private JCaHarvest harvest;
	private static PrintWriter Log = null;
	private boolean debug = false;
	static boolean bSynch = false;
	static int iJazzType = 0;
	
	githubldap()
	{
	       	try
	        {
	            FileOutputStream osLogStream = new FileOutputStream(LogName);

	            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
	        }
	        catch (FileNotFoundException ex)
	        {
	            if (debug) System.err.println("Error: creating log file");
	            if (debug) System.err.println("\t" + ex.getMessage());
	        }
	}

	private void setDebug()
	{
		debug = true;
	}

	// method to print log to logfile
	private static void printLog(String str)
	{
		System.out.println(str);
		Log.println(str);
	}

	public void handleMessage(String sMessage)
	{
		printLog(sMessage);
	}
	
	private static void ProcessInputList( JCaContainer cUserList,
            							  String InputFileName )
	{
        File file = new File(InputFileName);         
        BufferedReader reader = null;  
        int iIndex = 0;
        
        try {             
        	reader = new BufferedReader(new FileReader(file));             
        	String text = null; 
        	String name = null;
        	// repeat until all lines is read             
        	while ((text = reader.readLine()) != null) 
        	{     
        		name = text;
        		int cIndex = text.indexOf('(');
        		if ( cIndex >= 0)
        		{
        			int eIndex = text.indexOf(')');
        			if (eIndex < 0) eIndex = text.length()-1;
        			if (cIndex > 0) name = text.substring(0, cIndex);
        			text = text.substring(cIndex+1, eIndex);
        		}
        		cUserList.setString("pmfkey", text, iIndex);
        		cUserList.setString("name", name, iIndex++);
        	}         
        } catch (FileNotFoundException e) {             
        	//e.printStackTrace();         
        } catch (IOException e) {             
        	//e.printStackTrace();        
        } finally {             
        	try {                 
        		if (reader != null) 
        		{                     
        			reader.close();                 
        		}             
        	} catch (IOException e) {                 
        		//e.printStackTrace();             
        	}         
        } 	
	}
	
// LDAP-related routines
	
	private static boolean AddUserToLDAPGroup(DirContext ctx, 
			                                  String JazzLDAPUserGroup, 
			                                  String userDN)
	{		
		try {
			String sDN = userDN;
			
			//add to the required LDAP role   
			ModificationItem[] roleMods = new ModificationItem[]    
			{   
			    new ModificationItem( DirContext.ADD_ATTRIBUTE, new BasicAttribute( "member", sDN ) )   
			};  
			
			
			ctx.modifyAttributes( JazzLDAPUserGroup, roleMods );  

		} catch (javax.naming.AuthenticationException e) {
			iReturnCode = 1;
			System.err.println(e);
			System.exit(iReturnCode);
			
		// attempt to reacquire the authentication information
		} catch (NamingException e)	{
		    // Handle the error
			String sException = e.getMessage();
			if (sException.indexOf("ENTRY_EXISTS") < 0 ) 
			{
				iReturnCode = 2;
			    System.err.println(e);
			    System.exit(iReturnCode);
			}
			return false;
		}
		return true;
	}
	
	
	private static boolean RemoveUserFromLDAPGroup(DirContext ctx, 
                                                   String JazzLDAPUserGroup, 
                                                   String userDN)
	{		
		try {
			String sDN = userDN;
			
			//add to the required LDAP role   
			ModificationItem[] roleMods = new ModificationItem[]    
			{   
				new ModificationItem( DirContext.REMOVE_ATTRIBUTE, new BasicAttribute( "member", sDN ) )   
			};  
						
			ctx.modifyAttributes( JazzLDAPUserGroup, roleMods );  
		
		} catch (javax.naming.AuthenticationException e) {
			iReturnCode = 1;
			System.err.println(e);
			System.exit(iReturnCode);
		
		// attempt to reacquire the authentication information
		} catch (NamingException e)	{
			// Handle the error
			String sException = e.getMessage();
			if (sException.indexOf("ENTRY_NOT_FOUND") < 0 &&
				sException.indexOf("WILL_NOT_PERFORM") < 0) //forced deletion
			{
				iReturnCode = 2;
				System.err.println(e);
				System.exit(iReturnCode);
			}
			return false;
		}
		return true;
	}
	
	
	private static void ProcessLDAPAttrs(Attributes attributes, 
			                             JCaContainer cLDAP,
			                             boolean isNormalUser) 
	{
		int cIndex = 0;		
		if (cLDAP.getKeyCount() > 0)
		{
			cIndex = cLDAP.getKeyElementCount("sAMAccountName");
		}

		if (attributes.size() >= 3)
		{
		    boolean bMail = false;
		    boolean bPhone = false;
		    try {
				for (NamingEnumeration ae = attributes.getAll(); ae.hasMore();) {
				    Attribute attr = (Attribute)ae.next();
				    //printLog("attribute: " + attr.getID());
				    
				    /* Process each value */
				    //for (NamingEnumeration e = attr.getAll(); 
				    //     e.hasMore();
					//     System.out.println("attribute:" + attr.getID() + "  value: " + (String)e.next() )
					//     );
				    for (NamingEnumeration e = attr.getAll(); e.hasMore(); )
				    {
				    	String sAttr = attr.getID();
				    	if (sAttr.equalsIgnoreCase("mail")) bMail = true;
				    	if (sAttr.equalsIgnoreCase("ipPhone")) bPhone = true;
				    	cLDAP.setString(sAttr, (String)e.next(), cIndex);
				    	
				    }
				}

				String sID = cLDAP.getString("sAMAccountName", cIndex);
				
				if (!bMail) 
					cLDAP.setString("mail", "unknown", cIndex);
				if (!bPhone) 
					cLDAP.setString("ipPhone", "", cIndex);
									
				cLDAP.setString("haspmfkey", 
						        (isNormalUser &&
// TODO need a separate routine for validating the user id belongs to a user or not.						        		
					             sID.length() == 7 &&
					             !sID.equalsIgnoreCase("clate98") &&
					             !sID.equalsIgnoreCase("clate99") &&
					             !sID.equalsIgnoreCase("urctest") &&
					             !sID.equalsIgnoreCase("BEStest"))? "Y" : "N", 
					            cIndex);
			 
			} catch (NamingException e)
			{
			    // Handle the error
				iReturnCode = 2;
			    System.err.println(e);
			    System.exit(iReturnCode);
			}
		}
	} // end ProcessLDAPAttrs


	private static void ProcessLDAPRegion(DirContext ctx, 
			                              String region, 
			                              JCaContainer cLDAP,
			                              boolean isNormalUser) 
	{
		try {
			// Search directory for containers
			// Create the default search controls
			SearchControls ctls = new SearchControls();
	
			// Specify the search filter to match
			String filter = "(&(!(objectclass=computer))(&(objectclass=person)(sAMAccountName=*)))";
			
			// Specify the ids of the attributes to return
			String[] attrIDs = {"sAMAccountName", "displayName", "distinguishedName", "ipPhone", "mail"};
			ctls.setReturningAttributes(attrIDs);
	
			// Search for objects that have those matching attributes
			NamingEnumeration enumeration = ctx.search(region, filter, ctls);
			
			while (enumeration.hasMore()) {
			    SearchResult sr = (SearchResult)enumeration.next();
			    //System.out.println(">>>" + sr.getName());
			    ProcessLDAPAttrs(sr.getAttributes(), cLDAP, isNormalUser);
			}			
		} catch (javax.naming.AuthenticationException e) {
			iReturnCode = 1;
			System.err.println(e);
			System.exit(iReturnCode);			
		} catch (NamingException e)
		{
		    // attempt to reacquire the authentication information
		    // just skip region
			//iReturnCode = 2;
		    //System.err.println(e);
		    //System.exit(iReturnCode);
		}	
	} // end ProcessLDAPRegion
	
	private static void ProcessLDAPUserGroup(DirContext ctx, 
			                                 String JazzLDAPUserGroup, 
			                                 JCaContainer cJazzUsers)
	{
		try {
			// Retrieve attributes for a specific container
			int cIndex = 0;		
			if (cJazzUsers.getKeyCount() > 0)
			{
				cIndex = cJazzUsers.getKeyElementCount("member");
			}
			
			Attributes attributes = ctx.getAttributes(JazzLDAPUserGroup);
			for (NamingEnumeration ae = attributes.getAll(); ae.hasMore();) {
			    Attribute attr = (Attribute)ae.next();
			    //printLog("attribute: " + attr.getID());
			    
			    if (attr.getID().indexOf("member")==0)
			    {
				    /* Process each member attribute */
				    for (NamingEnumeration e = attr.getAll(); 
				         e.hasMore();
					     //printLog("value: " + e.next()) ) ;
				         )
				    {
				    	String dn = (String)e.next();
				    	//printLog("DN:" + dn);
				    	int iStart = dn.indexOf("CN=");
				    	int iEnd   = dn.indexOf(',', iStart);
				    	String pmfkey = dn.substring(iStart+3, iEnd);
				    	int iJazz[] = cJazzUsers.find("member", pmfkey);
				    	if (iJazz.length == 0)
				    		cJazzUsers.setString("member", pmfkey, cIndex++);
				    	    //printLog("member: "+pmfkey); //temp
				    }
				}
			}
		} catch (javax.naming.AuthenticationException e) {
			iReturnCode = 1;
		    System.err.println(e);
		    System.exit(iReturnCode);		    
	    // attempt to reacquire the authentication information
		} catch (NamingException e)
		{
		    // just skip region
			//iReturnCode = 2;
		    //System.err.println(e);
			//System.exit(iReturnCode);
		}	
	}

    
// main processing routine    
    
	private static void ProcessLDAPGroupUsers(JCaContainer cLDAP,
										      JCaContainer cJazzUsers,
			                                  DirContext ctx,
			                                  JCaContainer cAddUsers, 
			                                  JCaContainer cDelUsers,
			                                  String JazzLDAPUserGroup,
			                                  String sAuthName) 
	{
		
		printLog("Processing: " + sAuthName);
		
		try {
			boolean found=false;
			
			// 1. Active user accounts in CA.COM but with no JazzUser privilege
			printLog("1. Remove GitHub Users ");
			if (!cDelUsers.isEmpty())
			{
				for (int i=0; i<cDelUsers.getKeyElementCount("pmfkey"); i++ )
				{
					String sID = cDelUsers.getString("pmfkey", i);
					
					int iUser[] = cJazzUsers.find("member", sID);
					
					if (iUser.length > 0 || true) //forced deletion
					{
						int iLDAP[] = cLDAP.find("sAMAccountName", sID);
						if (iLDAP.length > 0)
						{
							String sUser  = cLDAP.getString("displayName", iLDAP[0]);
							String userDN = cLDAP.getString("distinguishedName", iLDAP[0]);									
							if (RemoveUserFromLDAPGroup(ctx, JazzLDAPUserGroup, userDN))
							{
								// Account not found in CA.COM
								printLog(">>>User (deactivate): "+sUser+ "("+ sID+")");									
							}
						}
					}
				}  //loop over user accounts						
			}	/* Delete List is not empty */
			
			// 2. LDAP users with no RTC user account
			printLog("2. Add GitHub Users");
			if (!cAddUsers.isEmpty())
			{
				for (int i=0; i<cAddUsers.getKeyElementCount("pmfkey"); i++ )
				{
					String sID = cAddUsers.getString("pmfkey", i);
					
					int iUser[] = cJazzUsers.find("member", sID);
					
					if (iUser.length == 0)
					{
						int iLDAP[] = cLDAP.find("sAMAccountName", sID);
						if (iLDAP.length > 0)
						{
							String sUser     = cLDAP.getString("displayName", iLDAP[0]);
							String userDN = cLDAP.getString("distinguishedName", iLDAP[0]);									
							if (AddUserToLDAPGroup(ctx, JazzLDAPUserGroup, userDN))
							{
								// Add user to LDAP JazzUser group
								printLog(">>>User (activate): "+sUser+ "("+ sID+")");											
							}
							
						} /* user exists in domain */
					} /*  user does not exist in GitHub DL */
				}  //loop over user accounts						
			} /* Add list is not empty */	
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
		String LogName = "C:";
		int dojts = -1;
		boolean bRepairLicenses = false;
		
		String[] regions = { "ou=users,ou=north america",
				             "ou=users,ou=itc hyderabad",
				             "ou=users,ou=europe middle east africa",
				             "ou=users,ou=asia pacific",
				             "ou=users,ou=south america",
				             "ou=joint venture consultants",
				             "ou=role-based,ou=north america",
				             "ou=role-based,ou=itc hyderabad",
				             "ou=role-based,ou=europe middle east africa",
				             "ou=role-based,ou=asia pacific",
				             "ou=role-based,ou=south america",
				             "cn=users"};

				
		String[] GitHubAdminLDAPGroupFormat = 
	  	{ 	
			"cn=Team - GitHub - %s,ou=groups,ou=north america"
	  	};
		
		String[] GitHubDevelopersLDAPGroupFormat = 
	  	{ 	
			"cn=Development-Tools-Access-Group,ou=groups,ou=north america"
	  	};

		String[][] JazzLDAPGroupFormat =
		{
			GitHubDevelopersLDAPGroupFormat,
			GitHubAdminLDAPGroupFormat
		};
				
		String[] GitHubUserAuthAdminSchemas =
		{
			"Admins" 
		};
		
		String[] GitHubUserAuthDeveloperSchemas =
		{
			"Developers" 
		};

		
		String[][] AuthSchemas =
		{
				GitHubUserAuthDeveloperSchemas,
				GitHubUserAuthAdminSchemas
		};
		
		JCaContainer cJazzUsers[] = { 
				  						new JCaContainer(),
				  						new JCaContainer()
									};
		
		JCaContainer cLDAP = new JCaContainer();
		JCaContainer cAddUsers = new JCaContainer();
		JCaContainer cDelUsers = new JCaContainer();
		

		
		// check parameters
		for (int i = 0; i < iParms; i++)
		{					
			if (args[i].compareToIgnoreCase("-add") == 0 )
			{
				ProcessInputList(cAddUsers, args[++i]);
			}			
			else if (args[i].compareToIgnoreCase("-del") == 0 )
			{
				ProcessInputList(cDelUsers, args[++i]);
			}			
			else if (args[i].compareToIgnoreCase("-synch") == 0 )
			{
				bSynch = true;
			}			
			else if (args[i].compareToIgnoreCase("-del") == 0 )
			{
				ProcessInputList(cDelUsers, args[++i]);
			}			
			else if (args[i].compareToIgnoreCase("-log") == 0 )
			{
				LogName = args[++i];
			}	
			else if (args[i].compareToIgnoreCase("-developers") == 0 )
			{
				iJazzType = 0;
			}	
			else if (args[i].compareToIgnoreCase("-admins") == 0 )
			{
				iJazzType = 1;
			}	
			
			else {
				System.out.println("Usage: githubldap [-add textfile] [-del textfile] [-log textfile] [-h |-?]");
				System.out.println(" -add option will add user accounts for pmfkeys in textfile param.");
				System.out.println(" -del option will archive user accounts for pmfkeys in textfile param.");
				System.out.println(" -log option specifies location log file.");
				System.exit(iReturnCode);
			}
		} // end for
		
		Hashtable env = new Hashtable();
		env.put(Context.PROVIDER_URL, "ldap://usildc04.ca.com:389/dc=ca,dc=com");
		//env.put(Context.SECURITY_PRINCIPAL, "harvestcscr");
		//env.put(Context.SECURITY_CREDENTIALS, "w3G0Th3Be3t");
		env.put(Context.SECURITY_PRINCIPAL, "toolsadmin");
		env.put(Context.SECURITY_CREDENTIALS, "R.oj;G>]<?.4UiQ");
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
						
		try {
			
	        //DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd@HH_mm_ss");
			Date date = new Date();
			LogName += "\\githubldap_" +dateFormat.format(date) +".log";
			
			FileOutputStream osLogStream = new FileOutputStream(LogName);
	        Log = new PrintWriter(osLogStream, true);

	        // Read user containers for CA.COM
			DirContext ctx = new InitialDirContext(env);
/* */		
			
			for (int i=0; i<regions.length; i++)
			{
				ProcessLDAPRegion(ctx, regions[i], cLDAP, (i<5));
			}

			// Show cLDAP statistics
			printLog("Number of CA.COM user containers read: " + cLDAP.getKeyElementCount("mail"));

			// Read Jazz LDAP group users
			for (int i=0; i<AuthSchemas[iJazzType].length; i++)
			{
				String[] JazzLDAPGroup = new String[JazzLDAPGroupFormat[iJazzType].length];
				
				for (int j=0; j<JazzLDAPGroupFormat[iJazzType].length; j++)
				{
					String JazzLDAPUserGroup = JazzLDAPGroup[j].format(JazzLDAPGroupFormat[iJazzType][j],
   		                                                               AuthSchemas[iJazzType][j]);
					ProcessLDAPUserGroup(ctx,
							             JazzLDAPUserGroup,
							             cJazzUsers[i]);
					ProcessLDAPGroupUsers(cLDAP,
						                  cJazzUsers[i],
                                          ctx,
                                          cAddUsers, 
                                          cDelUsers,
                                          JazzLDAPUserGroup,
                                          AuthSchemas[iJazzType][j]);
				}
			};
			
/* */						
		} catch (javax.naming.AuthenticationException e) {
			iReturnCode = 1;
		    System.err.println(e);
		    System.exit(iReturnCode);
		} catch (NamingException e) {
		    // attempt to reacquire the authentication information
		    // Handle the error
			iReturnCode = 2;
		    System.err.println(e);
		    System.exit(iReturnCode);			    
		} catch (FileNotFoundException e) {
			iReturnCode = 6;
		    System.err.println(e);			
			//e.printStackTrace();
		    System.exit(iReturnCode);		    
		}

		// exit application
		System.exit(iReturnCode);	

	} // end main
}
