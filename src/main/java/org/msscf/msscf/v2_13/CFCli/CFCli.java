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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import org.msscf.msscf.v2_13.cflib.CFLib.*;
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

public class CFCli
{
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

	/**
	 *	The main just launches the toolbar window, which exists as long as
	 *	the application is running.
	 */
	public static void main( String args[] ) {
		final String S_ProcName = "MSSBamCFCli.main() ";
	
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
	
//			String logFileName = rootGenDir + LogFileName;
//			try {
//				log.openLogFile( logFileName );
//				log.closeLogFile();
//
//				File f = new File( logFileName );
//				if( f.exists() ) {
//					f.delete();
//				}
//			}
//			catch( FileNotFoundException e ) {
//			}
	
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

//			ICFBamClusterObj origBamCluster = (ICFBamClusterObj)bamBLSchema.getClusterTableObj().newInstance();
//			ICFBamClusterEditObj editBamCluster = (ICFBamClusterEditObj)origBamCluster.beginEdit();
//			editBamCluster.setRequiredFullDomName( "system" );
//			origBamCluster = (ICFBamClusterObj)editBamCluster.create();
//			editBamCluster.endEdit();

//			ICFBamTenantObj origBamTenant = (ICFBamTenantObj)bamBLSchema.getTenantTableObj().newInstance();
//			ICFBamTenantEditObj editBamTenant = (ICFBamTenantEditObj)origBamTenant.beginEdit();
//			editBamTenant.setRequiredContainerCluster( origBamCluster );
//			editBamTenant.setRequiredTenantName( "system" );
//			origBamTenant = (ICFBamTenantObj)editBamTenant.create();
//			editBamTenant.endEdit();
	
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
//		log.closeLogFile();
	}
}
