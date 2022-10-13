package edu.bu.ist.apps.kualiautomation.services.config;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 * This is a factory class for producing web browser class instances.
 * The implementation of the web browser will depend on the type of operating system the factory determines is present.
 * 
 * @author wrh
 *
 */
public class WebBrowserFactory {

	public WebBrowser getBrowser(OperatingSystem os) {
		WebBrowser wb = null;
        
		switch(os.getType()) {
		case WINDOWS:
			wb = new WebBrowser() {
				@Override public void open(String url) throws Exception {
					if(!launchDesktopBrowser(url)) {
						Runtime rt = Runtime.getRuntime();
						rt.exec( "rundll32 url.dll,FileProtocolHandler " + url);
					}
				}};
			break;
		case MAC:
			wb = new WebBrowser() {
				@Override public void open(String url) throws Exception {
					if(!launchDesktopBrowser(url)) {
						Runtime rt = Runtime.getRuntime();
						rt.exec( "open" + url);				
					}
				}
			};
			break;
		case UNIX:
			wb = new WebBrowser() {
				@Override public void open(String url) throws Exception {
					if(!launchDesktopBrowser(url)) {
						Runtime rt = Runtime.getRuntime();
						String[] browsers = {"epiphany", "firefox", "mozilla", "konqueror", "netscape", "opera", "links", "lynx"};
						StringBuffer cmd = new StringBuffer();
						for (int i=0; i<browsers.length; i++)
						     cmd.append( (i==0  ? "" : " || " ) + browsers[i] +" \"" + url + "\" ");
	
						rt.exec(new String[] { "sh", "-c", cmd.toString() });				
					}
				}
			};
			break;
		}
		return wb;
	}
	
	/**
	 * Try to open a web browser using the awt Desktop library first.
	 * @param url
	 * @return
	 */
	private boolean launchDesktopBrowser(String url) {
        if(Desktop.isDesktopSupported()){
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } 
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } 
        else {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec("xdg-open " + url);
            } 
            catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
	}
}
