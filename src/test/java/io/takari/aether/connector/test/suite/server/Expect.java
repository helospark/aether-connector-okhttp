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

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Benjamin Hanzelmann
 */
public class Expect
    implements Behaviour
{

    private final Map<String, byte[]> expectations = new ConcurrentHashMap<String, byte[]>();

    private final Map<String, byte[]> seen = new ConcurrentHashMap<String, byte[]>();

    public void addExpectation( String path, byte[] content )
    {
        expectations.put( path, content );
    }

    public boolean execute( HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx )
        throws Exception
    {
        if ( "PUT".equals( request.getMethod() ) )
        {
            String path = request.getPathInfo().substring( 1 );
            ServletInputStream in = request.getInputStream();

            int count;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[16000];
            while ( ( count = in.read( b ) ) != -1 )
            {
                out.write( b, 0, count );
            }

            out.close();
            byte[] ba = out.toByteArray();

            seen.put( path, ba );

            return false;
        }
        return true;
    }

    public byte[] seenBytes( String path )
    {
        return seen.get( path );
    }

    public void assertExpectations()
    {
        for ( Entry<String, byte[]> entry : expectations.entrySet() )
        {
            String path = entry.getKey();
            if ( !Arrays.equals( entry.getValue(), seen.get( path ) ) )
            {
                throw new AssertionError( "assertion failed for " + path );
            }
        }
    }

}
