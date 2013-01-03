//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2011 by:
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

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReportRenderer;

/**
 * Responsible for actually rendering the site.
 * 
 * @author <a href="mailto:schmitz@occamlabs.de">Andreas Schmitz</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: $, $Date: $
 */
public class ModuleListRenderer extends AbstractMavenReportRenderer {

    private MavenProject project;

    public ModuleListRenderer( MavenProject project, Sink sink, String outputDirectory ) {
        super( sink );
        this.project = project;
    }

    @Override
    public String getTitle() {
        return "Module stability status report";
    }

    @Override
    protected void renderBody() {
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

        generateReport( byModuleName, status );
    }

    private void generateReport( SortedMap<String, String> byModuleName, HashSet<String> status ) {
        sink.head();
        sink.title();
        sink.text( "Module stability status report" );
        sink.title_();
        sink.head_();
        sink.body();

        generateAlphabeticalTable( byModuleName );
        generateStatusTables( byModuleName, status );

        sink.body_();
        sink.flush();
        sink.close();
    }

    private void generateStatusTables( SortedMap<String, String> byModuleName, HashSet<String> status ) {
        generateStatusTable( "ok", byModuleName );
        generateStatusTable( "check", byModuleName );
        generateStatusTable( "rework", byModuleName );
        for ( String s : status ) {
            if ( !( s.equals( "ok" ) || s.equals( "check" ) || s.equals( "rework" ) ) ) {
                generateStatusTable( s, byModuleName );
            }
        }
    }

    private void generateStatusTable( String status, SortedMap<String, String> byModuleName ) {
        sink.table();
        sink.tableCaption();
        sink.text( "Modules with status " + status );
        sink.tableCaption_();

        for ( Entry<String, String> e : byModuleName.entrySet() ) {
            if ( e.getValue().equals( status ) ) {
                sink.tableRow();
                sink.tableCell();
                sink.text( e.getKey() );
                sink.tableCell_();
                sink.tableRow_();
            }
        }

        sink.table_();
    }

    private void generateAlphabeticalTable( SortedMap<String, String> byModuleName ) {
        sink.table();
        sink.tableCaption();
        sink.text( "Status by module name" );
        sink.tableCaption_();

        for ( Entry<String, String> e : byModuleName.entrySet() ) {
            sink.tableRow();
            sink.tableCell();
            sink.text( e.getKey() );
            sink.tableCell_();
            sink.tableCell();
            sink.text( e.getValue() );
            sink.tableCell_();
            sink.tableRow_();
        }

        sink.table_();
    }

    private String getModuleStatus( MavenProject p ) {
        Properties props = p.getModel().getProperties();
        String st = props.getProperty( "deegree.module.status" );
        if ( st == null ) {
            return "not specified in pom";
        }
        return st;
    }

}
