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

import static io.takari.aether.connector.test.suite.server.BehaviourHelper.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Benjamin Hanzelmann
 */
public class Redirect
    implements Behaviour
{

    private int count = -1;

    private final AtomicInteger redirectCount = new AtomicInteger( 0 );

    private String content;

    private String target;

    private String pattern;

    private String replace;

    public Redirect( String pattern, String replace )
    {
        this.pattern = pattern;
        this.replace = replace;
    }

    public Redirect()
    {
        super();
    }

    public Redirect( int count )
    {
        this.count = count;
    }

    /**
     * @param url
     */
    public Redirect( String url )
    {
        this.target = url;
    }

    public boolean execute( HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx )
        throws Exception
    {
        if ( target != null )
        {
            response.setContentLength( 0 );
            response.sendRedirect( target );
            return false;
        }

        if ( pattern != null )
        {
            String path = request.getPathInfo();
            path = path.replaceAll( pattern, replace );

            response.sendRedirect( path );

            return false;
        }

        if ( count == -1 )
        {
            count = Integer.valueOf( firstPart( request.getPathInfo() ) );
            // save original content for last redirect
            content = content( request.getPathInfo() );
        }
        else if ( content == null )
        {
            content = request.getPathInfo();
        }

        if ( redirectCount.get() < count )
        {
            System.err.println( "Redirecting... " + redirectCount );
            response.setContentLength( 0 );
            response.sendRedirect( String.valueOf( redirectCount.incrementAndGet() ) );
            return false;
        }

        setContent( content, ctx );

        count = -1;
        redirectCount.set( 0 );
        content = null;

        return true;
    }

}
