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

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import java.util.Locale;

/**
 *
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 *
 * @version $Revision$, $Date: 2011-09-13 15:43:38 +0200 (Di, 13. Sep 2011) $
 */
@Execute(goal = "generate-modules-site")
@Mojo(defaultPhase = LifecyclePhase.SITE, name = "generate-modules-site", aggregator = true)
public class ModuleListSiteMojo extends AbstractMavenReport {

    /**
     * Directory where reports will go.
     */
    @Parameter(property = "project.reporting.outputDirectory", readonly = true, required = true)
    private String outputDirectory;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Component
    private Renderer siteRenderer;

    @Override
    public String getOutputName() {
        return "module-stability-status";
    }

    @Override
    public String getName( Locale locale ) {
        return "module stability status report";
    }

    @Override
    public String getDescription( Locale locale ) {
        return "module stability status report";
    }

    @Override
    protected Renderer getSiteRenderer() {
        return siteRenderer;
    }

    @Override
    protected String getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected void executeReport( Locale locale )
                            throws MavenReportException {
        ModuleListRenderer renderer = new ModuleListRenderer( project, getSink(), outputDirectory );
        renderer.renderBody();
    }

}
