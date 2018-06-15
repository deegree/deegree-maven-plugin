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

import java.io.*;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date: 2011-09-13 15:43:38 +0200 (Di, 13. Sep 2011) $
 */
@Execute(goal = "list-modules-wiki")
@Mojo(name = "list-modules-wiki")
public class ModuleListMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute()
                            throws MojoExecutionException,
                            MojoFailureException {

        FileOutputStream fos = null;
        try {
            if ( !project.getPackaging().equalsIgnoreCase( "pom" ) ) {

                String status = project.getProperties().getProperty( "deegree.module.status" );
                if ( status == null ) {
                    status = "unknown";
                }

                File f = new File( "/tmp/" + status + ".txt" );
                fos = new FileOutputStream( f, true );
                PrintWriter writer = new PrintWriter( new OutputStreamWriter( fos, "UTF-8" ) );

                writer.print( "||" );
                writer.print( project.getArtifactId() );
                writer.print( "||" );
                writer.print( project.getDescription() );
                writer.print( "||" );
                writer.print( "\n" );
                writer.close();
            }
        } catch ( FileNotFoundException e ) {
            throw new MojoExecutionException( e.getMessage(), e );
        } catch ( UnsupportedEncodingException e ) {
            throw new MojoExecutionException( e.getMessage(), e );
        } finally {
            IOUtils.closeQuietly( fos );
        }
    }

}
