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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;

/**
 * @author Benjamin Hanzelmann
 */
public class BehaviourServlet extends HttpServlet {
  private String spec;

  private final Behaviour[] behaviour;

  public BehaviourServlet(String pathspec, Behaviour[] behaviour) {
    this.spec = pathspec;
    this.behaviour = behaviour;
  }

  public String getPath() {
    if (!spec.startsWith("/")) {
      spec = "/" + spec;
    }
    if (!spec.endsWith("/*")) {
      spec += "/*";
    }
    return spec;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    behave(req, resp);
  }

  private void behave(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
    Log.getLog().debug("behaving: " + req.getPathInfo() + ", " + Arrays.toString(behaviour));
    try {
      Map<Object, Object> ctx = new HashMap<Object, Object>();
      for (Behaviour b : behaviour) {
        if (!b.execute(req, resp, ctx)) {
          break;
        }
      }
    } catch (Exception e) {
      throw new ServletException(e.getMessage(), e);
    }
  }

  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    behave(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    behave(req, resp);
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    behave(req, resp);
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    behave(req, resp);
  }

  @Override
  protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    behave(req, resp);
  }

  @Override
  protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    behave(req, resp);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " " + Arrays.toString(behaviour);
  }
}
