// Description: Main for CFCli

/*
 *  MSS Code Factory MssCF 2.13 CLI
 *
 *	Copyright 2020-2021 Mark Stephen Sobkow
 *
 *	This file is part of MSS Code Factory.
 *
 *	MSS Code Factory is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	MSS Code Factory is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with MSS Code Factory.  If not, see https://www.gnu.org/licenses/.
 *
 *	Donations to support MSS Code Factory can be made at
 *	https://www.paypal.com/paypalme2/MarkSobkow
 *
 *	Please contact Mark Stephen Sobkow at mark.sobkow@gmail.com for commercial licensing.
 */

package org.msscf.msscf.v2_13.CFCli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

import org.msscf.msscf.v2_13.cflib.CFLib.*;
import org.msscf.msscf.v2_13.cflib.CFLib.xml.*;

import org.msscf.msscf.v2_13.cfcore.CFGenKb.*;
import org.msscf.msscf.v2_13.cfcore.CFGenKbObj.*;
import org.msscf.msscf.v2_13.cfcore.CFGenKbRam.*;
import org.msscf.msscf.v2_13.cfcore.MssCF.*;
import org.msscf.msscf.v2_13.cfsec.CFSec.*;
import org.msscf.msscf.v2_13.cfsec.CFSecObj.*;
import org.msscf.msscf.v2_13.cfbam.CFBam.*;
import org.msscf.msscf.v2_13.cfbam.CFBamObj.*;
import org.msscf.msscf.v2_13.cfbam.CFBamRam.*;
import org.msscf.msscf.v2_13.cfbam.CFBamMssCF.*;
import org.msscf.msscf.v2_13.cfbamcust.CFBamXmlLoader.CFBamXmlLoader;
import org.msscf.msscf.v2_13.cfbamcust.MSSBamCF.*;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class
})
public class CFCli {
    private static final AtomicReference<Properties> systemProperties = new AtomicReference<>(null);
    private static final AtomicReference<Properties> applicationProperties = new AtomicReference<>(null);
    private static final AtomicReference<Properties> userDefaultProperties = new AtomicReference<>(null);
    private static final AtomicReference<Properties> userProperties = new AtomicReference<>(null);
    private static final AtomicReference<Properties> mergedProperties = new AtomicReference<>(null);

    /**
     * Loads the application properties file from the application resources.
     */
    public static Properties getApplicationProperties() {
        if (applicationProperties.get() == null) {
            Properties props = new Properties();
            try (var in = CFCli.class.getClassLoader().getResourceAsStream("application.properties")) {
                if (in != null) {
                    props.load(in);
                } else {
                    throw new RuntimeException("application.properties not found in classpath resources");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load application properties from application.properties", e);
            }
            applicationProperties.compareAndSet(null, props);
        }
        return applicationProperties.get();
    }

    /**
     * Loads the system properties, which hopefully haven't had the merge applied yet.
     */
    public static Properties getSystemProperties() {
        if (systemProperties.get() == null) {
            Properties props = new Properties();
            props.putAll(System.getProperties());
            systemProperties.compareAndSet(null, props);
        }
        return systemProperties.get();
    }
  
    /**
     * Loads the user default properties file from the application resources.
     */
    public static Properties getUserDefaultProperties() {
        if (userDefaultProperties.get() == null) {
            Properties props = new Properties();
            try (var in = CFCli.class.getClassLoader().getResourceAsStream("user-default.properties")) {
                if (in != null) {
                    props.load(in);
                } else {
                    throw new RuntimeException("user-default.properties not found in classpath resources");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load user default properties from user-default.properties", e);
            }
            userDefaultProperties.compareAndSet(null, props);
        }
        return userDefaultProperties.get();
    }

    /**
     * Loads the user properties file from their home directory.
     */
    public static Properties getUserProperties() {
        if (userProperties.get() == null) {
            Properties props = new Properties();
            File userFile = new File(System.getProperty("user.home"), ".cfcli.properties");
            if (userFile.exists()) {
                try (FileInputStream fis = new FileInputStream(userFile)) {
                    props.load(fis);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load user properties from .cfcli.properties", e);
                }
            } else {
                try (var in = CFCli.class.getClassLoader().getResourceAsStream("user-default.properties")) {
                    if (in != null) {
                        Files.copy(in, userFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println(String.format("A new user properties file has been created at: %s", userFile.getAbsolutePath()));
                        System.out.println("Please customize this file before running the application again.");
                        System.exit(0);
                    }
                    else {
                        var subin = CFCli.class.getClassLoader().getResourceAsStream("application.properties");
                        if (subin != null) {
                            Files.copy(subin, userFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        	System.out.println(String.format("A new user properties file has been created at: %s", userFile.getAbsolutePath()));
	                        System.out.println("Please customize this file before running the application again.");
                            System.exit(0);
                        } else {
                            throw new RuntimeException("user-default.properties and application.properties not found in classpath resources");
                        }
                    }
                } catch (IOException e) {
                    System.err.println(String.format("Failed to create user properties file \"%s\": %s", userFile.getAbsolutePath(), e.getMessage()));
                    System.exit(1);
                }
            }
            userProperties.compareAndSet(null, props);
        }
        return userProperties.get();
    }

    /**
     * Merges the System and User properties, giving preference to the User properties.
     */
    public static Properties getMergedProperties() {
        if (mergedProperties.get() == null) {
            Properties merged = new Properties();
            merged.putAll(getApplicationProperties());
            merged.putAll(getUserDefaultProperties());
            merged.putAll(getSystemProperties());
            merged.putAll(getUserProperties());
            mergedProperties.compareAndSet(null, merged);
        }
        return mergedProperties.get();
    }

    public static void main(String[] args) {
        // This weird looking cadence ensures that all the sub-property lists are prepared before getMergedProperties() is invoked, ensuring that any errors and exceptions along the way are thrown first and in predictable order
        Properties mergedProperties = getApplicationProperties();
        mergedProperties = getUserDefaultProperties();
        mergedProperties = getSystemProperties();
        mergedProperties = getUserProperties();
        mergedProperties = getMergedProperties();
        System.getProperties().putAll(mergedProperties);

        SpringApplication app = new SpringApplication(CFCli.class);
        app.addInitializers((applicationContext) -> {
            ConfigurableEnvironment env = applicationContext.getEnvironment();
            env.getPropertySources().addLast(new org.springframework.core.env.PropertiesPropertySource("userProperties", userProperties.get()));
        });
        app.run(args);
    }

	public final static String LogFileName = "ManufactureProject.log";
	public final static String ProductName = "MSS Code Factory 2.13";
	public final static String ProductWithVersion = "MSS Code Factory 2.13.11195";
	private static MSSBamCFPrefs _UserPrefs = null;
	private static String parsedToolSetNames[] = null;
	private static ICFLibMessageLog	log = new CFLibConsoleMessageLog();
	private static MSSBamCFEngine cfEngine = null;
	
	public static MSSBamCFEngine getEngine() {
		return( cfEngine );
	}
	
	public static void setEngine( MSSBamCFEngine engine ) {
		cfEngine = engine;
	}

	/**
	 *	Release static resources
	 */
	public static void saveUserPreferences() {
		if( _UserPrefs != null ) {
			if( ! _UserPrefs.savePreferences( log ) ) {
				return;
			}
	    }
	}
	
	/**
	 *	Initialize the console log
	 */
	protected static void initConsoleLog() {
	//	Layout layout = new PatternLayout(
	//			"%d{ISO8601}"		// Start with a timestamp
	//		+	" %-5p"				// Include the severity
	////		+	" %C.%M"			// pkg.class.method()
	////		+	" %F[%L]"			// File[lineNumber]
	//		+	": %m" );			// Message text
		//BasicConfigurator.configure( new ConsoleAppender( layout, "System.out" ) );
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {

			final String S_ProcName = "CFCli.main() ";
		
			initConsoleLog();
	
			MSSBamCFGelCompiler.setCodeFactoryVersion( ProductName );
	
		//	System.setProperty( "org.apache.xerces.xni.parser.XMLParserConfiguration",
		//		"org.apache.xerces.parsers.XMLGrammarCachingConfiguration" );
	
		//	Get the name of the cartridge to be resolved.
		
			String cartridgeName = "";
		
		//	Finish application library loading and initialization
	
			String generatingBuild = "1";	
			Timestamp				genstarttime = null;
			Timestamp				genendtime = null;
			ICFLibAnyObj			version = null;
			ArrayList<String>		listOfToolsetName = new ArrayList<String>();
			String					toolsetName = null;
			
			genstarttime = new Timestamp( System.currentTimeMillis() );
		
			try {
		
				log.message(
					"Starting " + ProductWithVersion + "..." );
		
		//		Get the name of the model to be resolved.
		
				boolean testOnly = false;
				String modelName;
				switch( args.length ) {
					case 0:
					case 1:
					case 2:
						testOnly = true;
						toolsetName = null;
						String msg = "org.msscf.msscf.CFCli.CFCli build_string model_name_or_uri cartridge_name [toolsetName{ toolsetName}]\n"
							+ "\tWHERE\n"
							+ "\t\tbuild_string is the name of the build to be manufactured,\n"
							+ "\t\tmodel_name_or_uri is the name of the application model to generate,\n"
							+ "\t\t\tor the URI of a model definition file to load.\n"
							+ "\t\tcartridge_name is the name of the rule cartridge to use\n"
							+ "\t\t\tfor generating code.\n"
							+ "\t\ttoolsetName[s] are the names of the toolsets to produce code for";
						log.message( msg );
						throw new IllegalArgumentException( msg );
					case 3:
						generatingBuild = args[0];
						modelName = args[1];
						cartridgeName = args[2];
						toolsetName = null;
						break; 
					default:
						generatingBuild = args[0];
						modelName = args[1];
						cartridgeName = args[2];
						for( int idxArg = 3; idxArg < args.length; idxArg ++ ) {
							toolsetName = args[idxArg];
							if( ( toolsetName != null ) && ( toolsetName.length() > 0 ) ) {
								listOfToolsetName.add( toolsetName );
							}
						}
						break; 
				}
		
		//		Load the user preferences
		
		//	    log.message( "Loading user preferences..." );
				_UserPrefs = new MSSBamCFPrefs();
				if( ! _UserPrefs.loadPreferences( log, false ) ) {
					log.message( "Cannot continue." );
					return;
				}
				
				String prefsGenDir = _UserPrefs.getRootGenDir();
				if( ( prefsGenDir == null ) || ( prefsGenDir.length() <= 0 ) ) {
					throw new RuntimeException( S_ProcName + "Preferences RootGenDir is null or empty" );
				}
		
				int idxLast = prefsGenDir.length() - 1;
				String rootGenDir =
					(	( prefsGenDir.lastIndexOf( '/' ) == idxLast )
						|| ( prefsGenDir.lastIndexOf( '\\' ) == idxLast )
						|| ( prefsGenDir.lastIndexOf( File.separatorChar ) == idxLast ) )
					? prefsGenDir
					: prefsGenDir + File.separator;
		
		//		Create a log file for the run
		
//				String logFileName = rootGenDir + LogFileName;
//				try {
//					log.openLogFile( logFileName );
//					log.closeLogFile();
//	
//					File f = new File( logFileName );
//					if( f.exists() ) {
//						f.delete();
//					}
//				}
//				catch( FileNotFoundException e ) {
//				}
		
		//		Log generator versioning
		
				log.message( ProductWithVersion + " started" );
		
		//		Initialize the generation engine
		
				ICFGenKbSchema genKbSchema = new CFGenKbRamSchema();
				ICFGenKbSchemaObj genKbSchemaObj = new CFGenKbSchemaObj();
				genKbSchemaObj.setBackingStore( genKbSchema );
				genKbSchema.connect( "system", "system", "system", "system" );
				genKbSchema.rollback();
				genKbSchema.beginTransaction();
				genKbSchemaObj.setSecCluster( genKbSchemaObj.getClusterTableObj().getSystemCluster() );
				genKbSchemaObj.setSecTenant( genKbSchemaObj.getTenantTableObj().getSystemTenant() );
				genKbSchemaObj.setSecSession( genKbSchemaObj.getSecSessionTableObj().getSystemSession() );
				CFGenKbAuthorization genKbAuth = new CFGenKbAuthorization();
				genKbAuth.setSecCluster( genKbSchemaObj.getSecCluster() );
				genKbAuth.setSecTenant( genKbSchemaObj.getSecTenant() );
				genKbAuth.setSecSession( genKbSchemaObj.getSecSession() );
				genKbSchemaObj.setAuthorization( genKbAuth );
	
				ICFBamSchema cfBamSchema = new CFBamRamSchema();
				ICFBamSchemaObj cfBamSchemaObj = new CFBamSchemaObj();
				cfBamSchemaObj.setBackingStore( cfBamSchema );
				cfBamSchema.connect( "system", "system", "system", "system" );
				cfBamSchema.rollback();
				cfBamSchema.beginTransaction();
				cfBamSchemaObj.setSecCluster( cfBamSchemaObj.getClusterTableObj().getSystemCluster() );
				cfBamSchemaObj.setSecTenant( cfBamSchemaObj.getTenantTableObj().getSystemTenant() );
				cfBamSchemaObj.setSecSession( cfBamSchemaObj.getSecSessionTableObj().getSystemSession() );
				CFSecAuthorization cfBamAuth = new CFSecAuthorization();
				cfBamAuth.setSecCluster( cfBamSchemaObj.getSecCluster() );
				cfBamAuth.setSecTenant( cfBamSchemaObj.getSecTenant() );
				cfBamAuth.setSecSession( cfBamSchemaObj.getSecSession() );
				cfBamSchemaObj.setAuthorization( cfBamAuth );
	
				cfEngine = new MSSBamCFEngine();
				cfEngine.setLog( log );
				
				((MSSBamCFEngine)cfEngine).init( generatingBuild, genKbSchemaObj, genKbSchemaObj.getSecTenant(), cfBamSchemaObj, rootGenDir );
				
				log.message( "Linked with "
						+ CFLib.LinkName
						+ " version "
						+ CFLib.LinkVersion );
				log.message( "Linked with "
						+ MssCFEngine.GeneratorName
						+ " version "
						+ MssCFEngine.GeneratorVersion );
				log.message( "Linked with "
						+ MSSBamCFEngine.GeneratorName
						+ " version "
						+ MSSBamCFEngine.GeneratorVersion );
		
		//		Configure parser
		
				log.message( "Initializing rule cartridge parser..." );
		
				Iterator<String> cartridgePath = _UserPrefs.getCartridgePathIterator();
				while( cartridgePath.hasNext() ) {
					String cartridgeDir = (String)cartridgePath.next();
					if( ( cartridgeDir != null ) && ( cartridgeDir.length() > 0 ) ) {
						MssCFRuleCartridgeParser.addCartridgePath( cartridgeDir );
					}
				}
		
				URL url = cfEngine.getClass().getResource( "/cartridge-2.13/org-msscf-msscf-2-13-toolset-java/rulecartridge.xml" );
				if( url == null ) {
					url = cfEngine.getClass().getResource( "cartridge-2.13/org-msscf-msscf-2-13-toolset-java/rulecartridge.xml" );
				}
				if( url != null ) {
					String str = url.toString();
					int lastSlash = str.lastIndexOf( '/' );
					if( lastSlash > 0 ) {
						// strip off /rulecartridge.xml
						str = str.substring( 0, lastSlash );
						lastSlash = str.lastIndexOf( '/' );
						if( lastSlash > 0 ) {
							// strip off /net...java, keeping trailing slash
							str = str.substring( 0, lastSlash + 1 );
							MssCFRuleCartridgeParser.addCartridgePath( str );
						}
					}
				}
		
				Iterator<String> modelPath = _UserPrefs.getModelPathIterator();
				while( modelPath.hasNext() ) {
					String modelDir = (String)modelPath.next();
					CFBamXmlLoader.addModelPath( modelDir );
				}
		
		//		Load the cartridge
		
				MssCFRuleCartridgeParser cartridgeParser = new MssCFRuleCartridgeParser( cfEngine, log );
				
				parsedToolSetNames = null;
				try {
					cartridgeParser.loadRuleCartridge( cartridgeName );
				}
				catch( Exception e ) {
					log.message( "Could not load rule cartridge: " + e.getMessage() );
		            throw( e );
				}
				catch( Error e ) {
					log.message( "Could not load rule cartridge: " + e.getMessage() );
		            throw( e );
				}
				parsedToolSetNames = MssCFRuleCartridgeParser.getToolSetNames();
		
				if( ( parsedToolSetNames != null )
				 && ( parsedToolSetNames.length > 0 ) )
				{
					StringBuffer msg = new StringBuffer();
					msg.append( "Rule cartridge specified tool set names " );
					for( int idxName = 0; idxName < parsedToolSetNames.length; idxName ++ )
					{
						if( idxName > 0 )
						{
							msg.append( ", " );
						}
						msg.append( parsedToolSetNames[idxName] );
					}
					log.message( msg.toString() );
				}
				else {
					log.message( "Rule cartridge did not define tool set names to process." );
				}
	
		//		Initialize the cluster and tenant for the dictionary
	
//				ICFBamClusterObj origBamCluster = (ICFBamClusterObj)bamBLSchema.getClusterTableObj().newInstance();
//				ICFBamClusterEditObj editBamCluster = (ICFBamClusterEditObj)origBamCluster.beginEdit();
//				editBamCluster.setRequiredFullDomName( "system" );
//				origBamCluster = (ICFBamClusterObj)editBamCluster.create();
//				editBamCluster.endEdit();
	
//				ICFBamTenantObj origBamTenant = (ICFBamTenantObj)bamBLSchema.getTenantTableObj().newInstance();
//				ICFBamTenantEditObj editBamTenant = (ICFBamTenantEditObj)origBamTenant.beginEdit();
//				editBamTenant.setRequiredContainerCluster( origBamCluster );
//				editBamTenant.setRequiredTenantName( "system" );
//				origBamTenant = (ICFBamTenantObj)editBamTenant.create();
//				editBamTenant.endEdit();
		
		//		Instantiate Bam Model parser
	
				CFBamXmlLoader bamParser = new CFBamXmlLoader( (MSSBamCFEngine)cfEngine, log );
				bamParser.setSchemaObj( cfBamSchemaObj );
				bamParser.setTenant( (ICFBamTenantObj)( cfBamSchemaObj.getSecTenant() ) );
				
		//		Parse the model
		
				try {
					bamParser.loadTenant(modelName);
				}
				catch( Exception e ) {
					log.message( "Could not load Tenant: " + e.getMessage() );
		            throw( e );
				}
				catch( Error e ) {
					log.message( "Could not load Tenant: " + e.getMessage() );
		            throw( e );
				}
		
				ICFBamTenantObj Tenant = bamParser.getTenant();
				version = bamParser.getDefinedProjectVersion();
	
				try {
					if( listOfToolsetName.size() <= 0 ) {
						for( int idx = 0; idx < parsedToolSetNames.length; idx++ ) {
							listOfToolsetName.add( parsedToolSetNames[idx] );
						}
					}
					if( ( listOfToolsetName.size() > 0 ) && ( version != null ) ) {
						cfEngine.generate(generatingBuild, rootGenDir, version, listOfToolsetName, MSSBamCFEngine.ITEMNAME_TOP);
					}
				}
				catch( Exception e )
				{
					log.message( S_ProcName + "Manufacturing code threw " + e.getClass().getSimpleName() + " " + e.getMessage() + ", stack trace follows:" );
			    	e.printStackTrace( /*log.getPrintStream()*/ );
				}
			}
			catch( Exception e )
			{
		    	log.message( S_ProcName
		    			+ "Caught " + e.getClass().getSimpleName() + " " + e.getMessage() + ", stack trace follows:" );
		    	e.printStackTrace( /*log.getPrintStream()*/ );
			}
		
			genendtime = new Timestamp( System.currentTimeMillis() );
		
			long msec = genendtime.getTime() - genstarttime.getTime();
		
			String elapsed = String.format( "%1$d:%2$02d:%3$02d.%4$03d",
					msec / ( 1000 * 60 * 60 ),
					( msec / ( 1000 * 60 )) % 60,
					( msec / 1000 ) % 60,
					( msec % 1000 ) );
			log.message( 
					( ( version != null )
						? MSSBamCFAnyObj.getFullName( version )
						: "Code" )
				+ " manufacturing took " + elapsed );
		
			log.message( "Releasing MSS Code Factory " + MSSBamCFEngine.GeneratorVersion + " engine..." );
			cfEngine = null;
		
			log.message( ProductWithVersion + " finished." );
//			log.closeLogFile();
		};
	}
}
