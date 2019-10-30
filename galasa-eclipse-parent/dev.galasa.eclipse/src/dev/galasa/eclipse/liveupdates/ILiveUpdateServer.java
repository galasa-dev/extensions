package dev.galasa.eclipse.liveupdates;

import java.net.URL;

import javax.servlet.http.HttpServlet;

public interface ILiveUpdateServer {
	
	URL getLiveUpdateUrl();
	void registerServlet(HttpServlet servlet, String pathSpec);
	void unregisterServlet(HttpServlet servlet);

}
