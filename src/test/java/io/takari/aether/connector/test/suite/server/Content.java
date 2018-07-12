/**
 * Copyright (c) 2012 to original author or authors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.aether.connector.test.suite.server;

/*
 * Copyright (c) 2010-2011 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0, 
 * and you may not use this file except in compliance with the Apache License Version 2.0. 
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the Apache License Version 2.0 is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Benjamin Hanzelmann
 *
 */
public class Content
    implements Behaviour
{

    private String content;

    public Content()
    {
    }

    public Content( String content )
    {
        this.content = content;
    }

    public boolean execute( HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx )
        throws Exception
    {
        String path = request.getPathInfo().substring( 1 );
        String content = path;
        if ( "GET".equals( request.getMethod() ) )
        {
            response.setContentType( "text/plain" );
            if ( this.content != null )
            {
                content = this.content;
            }
            else if ( ctx.containsKey( Behaviour.Keys.CONTENT ) )
            {
                content = ctx.get( Behaviour.Keys.CONTENT ).toString();
            }

            response.setContentLength( content.getBytes( "UTF-8" ).length );
            try
            {
                response.getOutputStream().write( content.getBytes( "UTF-8" ) );
            }
            catch ( IllegalStateException e )
            {
                response.getWriter().write( content );
            }
            return false;
        }
        else
        {
            return true;
        }
    }

}
