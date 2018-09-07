//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2010 by:
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

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.log4j.LogManager.getLogger;
import static org.deegree.maven.utils.ClasspathHelper.addDependenciesToClasspath;

import java.io.*;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.AbstractScanner;
import org.reflections.scanners.Scanner;
import org.reflections.vfs.Vfs;

import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

/**
 *
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 *
 * @version $Revision$, $Date$
 */
@Execute(goal = "assemble-log4j", phase = LifecyclePhase.GENERATE_RESOURCES)
@Mojo(name = "assemble-log4j", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class Log4jMojo extends AbstractMojo {

    private static Logger LOG = getLogger( Log4jMojo.class );

    @Parameter(defaultValue = "120", required = true)
    private int width;

    @Parameter(defaultValue = "INFO", required = true)
    private String deegreeLoggingLevel;

    @Parameter(defaultValue = "ERROR", required = true)
    private String rootLoggingLevel;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${localRepository}")
    private ArtifactRepository localRepository;

    private void block( String text, PrintWriter out ) {
        out.print( "<!-- " );
        for ( int i = 0; i < width - 2; ++i ) {
            out.print( "=" );
        }
        out.println( " -->" );
        int odd = text.length() % 2;
        int len = ( width - text.length() - 4 ) / 2;
        out.print( "<!-- " );
        for ( int i = 0; i < len; ++i ) {
            out.print( "=" );
        }
        out.print( " " + text + " " );
        for ( int i = 0; i < len + odd; ++i ) {
            out.print( "=" );
        }
        out.println( " -->" );
        out.print( "<!-- " );
        for ( int i = 0; i < width - 2; ++i ) {
            out.print( "=" );
        }
        out.println( " -->" );
        out.println();
    }

    private void collect( final String type, String msg, final PrintWriter out, final String version ) {
        block( msg, out );

        // seems to scan automatically without requesting something
        new Reflections( "META-INF.deegree", new AbstractScanner() {

            @Override
            public void setConfiguration( Configuration configuration ) {
            }

            @Override
            public Multimap<String, String> getStore() {
                return null;
            }

            @Override
            public void setStore( Multimap<String, String> store ) {
            }

            @Override
            public Scanner filterResultsBy( Predicate<String> filter ) {
                return null;
            }

            @Override
            public boolean acceptsInput( String file ) {
                return file.equals( "META-INF.deegree.log4j-" + version + "." + type.toLowerCase() );
            }

            @Override
            public Object scan( Vfs.File file, Object classObject ) {
                try {
                    InputStream in = file.openInputStream();
                    BufferedReader reader = new BufferedReader( new InputStreamReader( in, "UTF-8" ) );
                    String line;
                    while ( ( line = reader.readLine() ) != null ) {
                        out.println( line );
                    }
                } catch ( IOException e ) {
                    LOG.error( "Trouble extracting the log4j snippet: " + e.getMessage() );
                    LOG.error( "This shouldn't happen, set log level to debug to see the stack trace." );
                    LOG.debug( e.getMessage(), e );
                }
                return classObject;
            }

            @Override
            public void scan( Object cls ) {
            }

            @Override
            public boolean acceptResult( String fqn ) {
                return fqn.equals( "META-INF.deegree.log4j." + type.toLowerCase() );
            }
        } );
    }

    @Override
    public void execute()
                            throws MojoExecutionException,
                            MojoFailureException {
        if ( new File( project.getBasedir(), "src/main/resources/log4j2.xml" ).exists() ) {
            getLog().info( "Skipping generation of log4j2.xml as it already exists in src/main/resources." );
            return;
        }
        String version = project.getVersion();

        addDependenciesToClasspath( project, artifactResolver, repositorySystem, localRepository );

        // to work around stupid initialization compiler error (hey, it's defined to be null if not 'initialized'!)
        PrintWriter o = null;
        final PrintWriter out;
        try {
            File outFile = new File( project.getBasedir(), "target/generated-resources/log4j2.xml" );
            if ( !outFile.getParentFile().exists() && !outFile.getParentFile().mkdirs() ) {
                throw new MojoFailureException( "Could not create parent directory: " + outFile.getParentFile() );
            }
            out = new PrintWriter( new OutputStreamWriter( new FileOutputStream( outFile ), "UTF-8" ) );

            out.println( "<!-- by default, only log to stdout -->" );
            out.println( "<Configuration>" );
            out.println( "  <Appenders>" );
            out.println( "    <Console name=\"stdout\" target=\"SYSTEM_OUT\">" );
            out.println( "      <PatternLayout pattern=\"[%d{HH:mm:ss}] %5p: [%c{1}] %m%n\">" );
            out.println( "    </Console>" );
            out.println( "    <!-- example log file appender -->" );
            out.println( "    <!-- RollingFile name=\"MyFile\" fileName=\"logs/app.log\">" );
            out.println( "      <PatternLayout pattern=\"[%d{HH:mm:ss}] %5p: [%c{1}] %m%n\">" );
            out.println( "        <Policies>" );
            out.println( "          <TimeBasedTriggeringPolicy />" );
            out.println( "          <SizeBasedTriggeringPolicy size=\"1000KB\" />" );
            out.println( "        </Policies>" );
            out.println( "        <DefaultRolloverStrategy />" );
            out.println( "    </RollingFile -->" );
            out.println( "  </Appenders>" );
            out.println( "  <Loggers>" );
            out.println();

            out.println( "    <!-- The log level for all classes that are not configured below. -->" );
            out.println( "    <AsyncLogger name=\"org.reflections\" level=\"FATAL\" />" );
            out.println();
            out.println( "    <!-- The log level for all classes that are not configured below. -->" );
            out.println( "    <AsyncLogger name=\"org.deegree\" level=\"" + deegreeLoggingLevel + "\" />" );
            out.println();
            out.println( "    <!-- automatically generated output follows -->" );
            out.println();

            o = out;

            collect( "ERROR", "Severe error messages", out, version );
            collect( "WARN", "Important warning messages", out, version );
            collect( "INFO", "Informational messages", out, version );
            collect( "DEBUG", "Debugging messages, useful for in-depth debugging of e.g. service setups", out, version );
            collect( "TRACE", "Tracing messages, for developers only", out, version );

            out.println( "    <AsyncRoot level=\"" + rootLoggingLevel + "\">" );
            out.println( "      <AppenderRef ref=\"stdout\" />" );
            out.println( "    </AsyncRoot>" );

            out.println( "  </Loggers>" );
            out.println( "</Configuration>" );
        } catch ( IOException e ) {
            getLog().error( e );
        } finally {
            closeQuietly( o );
        }
    }

}
