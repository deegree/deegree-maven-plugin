//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2012 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.maven;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal generate-modules-site
 * @aggregator
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date: 2011-09-13 15:43:38 +0200 (Di, 13. Sep 2011) $
 */
public class ModuleListSiteMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    @Override
    public void execute()
                            throws MojoExecutionException, MojoFailureException {
        File dir = new File( project.getBasedir(), "target" );
        List<MavenProject> modules = project.getCollectedProjects();

        SortedMap<String, String> byModuleName = new TreeMap<String, String>();
        HashSet<String> status = new HashSet<String>();

        for ( MavenProject p : modules ) {
            if ( p.getPackaging().equals( "pom" ) ) {
                continue;
            }
            String st = getModuleStatus( p );
            status.add( st );
            byModuleName.put( p.getArtifactId(), st );
        }

        String html = generateHtml( byModuleName, status );
        try {
            FileUtils.write( new File( dir, "modulestatus.html" ), html );
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String generateHtml( SortedMap<String, String> byModuleName, HashSet<String> status ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "<html><body>" );

        sb.append( generateAlphabeticalTable( byModuleName ) );
        sb.append( generateStatusTables( byModuleName, status ) );

        sb.append( "</body></html>\n" );
        return sb.toString();
    }

    private String generateStatusTables( SortedMap<String, String> byModuleName, HashSet<String> status ) {
        StringBuilder sb = new StringBuilder();

        sb.append( generateStatusTable( "ok", byModuleName ) );
        sb.append( generateStatusTable( "check", byModuleName ) );
        sb.append( generateStatusTable( "rework", byModuleName ) );
        for ( String s : status ) {
            if ( !( s.equals( "ok" ) || s.equals( "check" ) || s.equals( "rework" ) ) ) {
                sb.append( generateStatusTable( s, byModuleName ) );
            }
        }

        return sb.toString();
    }

    private String generateStatusTable( String status, SortedMap<String, String> byModuleName ) {
        String color = colorForStatus( status );
        StringBuilder sb = new StringBuilder();
        sb.append( "<fieldset style='display: inline-block;'><legend>Modules with status " ).append( status ).append( "</legend>" );
        sb.append( "<table bgcolor='" ).append( color ).append( "'>" );

        for ( Entry<String, String> e : byModuleName.entrySet() ) {
            if ( e.getValue().equals( status ) ) {
                sb.append( "<tr><td>" ).append( e.getKey() ).append( "</td></tr>" );
            }
        }

        sb.append( "</table></fieldset>" );
        return sb.toString();
    }

    private String generateAlphabeticalTable( SortedMap<String, String> byModuleName ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "<fieldset style='display: inline-block;'>" );
        sb.append( "<legend>Status by module name</legend>" );
        sb.append( "<table>" );

        for ( Entry<String, String> e : byModuleName.entrySet() ) {
            sb.append( "<tr><td>" );
            sb.append( e.getKey() );
            sb.append( "</td><td bgcolor='" ).append( colorForStatus( e.getValue() ) ).append( "'>" );
            sb.append( e.getValue() );
            sb.append( "</td></tr>" );
        }

        sb.append( "</table></fieldset>" );
        return sb.toString();
    }

    private String getModuleStatus( MavenProject p ) {
        Properties props = p.getModel().getProperties();
        String st = props.getProperty( "deegree.module.status" );
        if ( st == null ) {
            return "not specified in pom";
        }
        return st;
    }

    private String colorForStatus( String status ) {
        if ( status.equals( "ok" ) ) {
            return "lightgreen";
        }
        if ( status.equals( "check" ) ) {
            return "yellow";
        }
        if ( status.equals( "rework" ) ) {
            return "red";
        }
        return "lightgray";
    }

}
