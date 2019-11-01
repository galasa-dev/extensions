/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.liveupdates.internal;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import dev.galasa.eclipse.Activator;
import dev.galasa.eclipse.liveupdates.ILiveUpdateServer;

public class LiveUpdateServer implements ILiveUpdateServer {

    private final Server                server;
    private final ServletContextHandler handler;
    private final URL                   url;

    public LiveUpdateServer() throws Exception {
        this.server = new Server(new InetSocketAddress("localhost", 0));
        String context = "/" + UUID.randomUUID().toString();
        this.handler = new ServletContextHandler();
        this.handler.setContextPath(context);
        this.server.setHandler(this.handler);
        this.server.start();

        this.url = new URL(this.server.getURI() + "/");
    }

    @Override
    public URL getLiveUpdateUrl() {
        return this.url;
    }

    @Override
    public synchronized void registerServlet(HttpServlet servlet, String pathSpec) {
        this.handler.addServlet(new ServletHolder(servlet), pathSpec);
    }

    @Override
    public synchronized void unregisterServlet(HttpServlet servlet) {
        // *** no official way to unregister servlets, as they are little beans, hope it
        // is not a problem
        // *** until we can find a way of doing it
//		ServletHolder[] servlets = this.handler.getServletHandler().getServlets();
//		
//		ArrayList<ServletHolder> newServlets = new ArrayList<>();
//		for(ServletHolder holder : servlets) {
//			try {
//				if (holder.getServlet() != servlet) {
//					newServlets.add(holder);
//				}
//			} catch (ServletException e) {
//				Activator.log(e);
//			}
//		}
//		
//		this.handler.getServletHandler().setServlets(newServlets.toArray(new ServletHolder[newServlets.size()]));
    }

    public synchronized void stop() throws Exception {
        this.server.stop();
    }

}
