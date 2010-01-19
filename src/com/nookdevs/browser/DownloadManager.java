/**
 *     This file is part of nookBrowser.

    nookBrowser is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    nookBrowser is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with nookBrowser.  If not, see <http://www.gnu.org/licenses/>.

 */
package com.nookdevs.browser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.net.WebAddress;
import android.app.AlertDialog;


public class DownloadManager implements DownloadListener, AlertDialog.OnClickListener {
	private nookBrowser m_Browser;
	private boolean m_StopDownload=false;
	Handler m_Handler = new Handler();
	
	public DownloadManager(nookBrowser browser) {
		this.m_Browser = browser;
	}

	//Copied from android Browser code 
	 /**
     * Notify the host application a download should be done, or that
     * the data should be streamed if a streaming viewer is available.
     * @param url The full url to the content that should be downloaded
     * @param contentDisposition Content-disposition http header, if
     *                           present.
     * @param mimetype The mimetype of the content reported by the server
     * @param contentLength The file size reported by the server
     */
    public void onDownloadStart( String url,  String userAgent,
    			 String contentDisposition,  String mimetype,  long contentLength) {
    		    
    	// if we're dealing wih A/V content that's not explicitly marked
        //     for download, check if it's streamable.
        if (contentDisposition == null
                || !contentDisposition.regionMatches(
                        true, 0, "attachment", 0, 10)) {
            // query the package manager to see if there's a registered handler
            //     that matches.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), mimetype);
            ResolveInfo info = m_Browser.getPackageManager().resolveActivity(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null) {
                ComponentName myName = m_Browser.getComponentName();
                // If we resolved to ourselves, we don't want to attempt to
                // load the url only to try and download it again.
                if (!myName.getPackageName().equals(
                        info.activityInfo.packageName)
                        || !myName.getClassName().equals(
                                info.activityInfo.name)) {
                    // someone (other than us) knows how to handle this mime
                    // type with this scheme, don't download.
                    try {
                        m_Browser.startActivity(intent);
                        return;

                    } catch (ActivityNotFoundException ex) {
                        Log.d("DownloadManager", "activity not found for " + mimetype
                                    + " over " + Uri.parse(url).getScheme(),
                                    ex);
                        // Best behavior is to fall back to a download in this
                        // case
                    }
                }
            }
        }
        final String furl=url;
        final String fuserAgent = userAgent;
        final String fcontentDisposition = contentDisposition;
        final String fmimetype = mimetype;
        final long flength = contentLength;
        Runnable thrd = new Runnable() {
        	public void run() {
         		onDownloadStartNoStream(furl, fuserAgent, fcontentDisposition, fmimetype, flength);
        	}
        };
        Thread t = new Thread(thrd);
        t.start();
    }

    /**
     * Notify the host application a download should be done, even if there
     * is a streaming viewer available for thise type.
     * @param url The full url to the content that should be downloaded
     * @param contentDisposition Content-disposition http header, if
     *                           present.
     * @param mimetype The mimetype of the content reported by the server
     * @param contentLength The file size reported by the server
     */
    /*package */ 
    void onDownloadStartNoStream(String url, String userAgent,
            String contentDisposition, String mimetype, long contentLength) {
    	if( url.startsWith("file://")) 
    		return;
        m_StopDownload=false;
        File sdcardFolder = Environment.getExternalStorageDirectory();
        //File internalFolder = Environment.getDataDirectory();
        String status = Environment.getExternalStorageState();
        File baseFolder=null;
        boolean useInternalFolder=false;
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
        	System.out.println("No 2nd SD card ...");
        	useInternalFolder=true;
        } else {
        	File tmp = new File(sdcardFolder.getPath() + "/nookBrowser_tmp.log");
        	try {
        		if( tmp.exists() || tmp.createNewFile()) {
        			baseFolder = sdcardFolder;
        			tmp.delete();
        		} else {
        			useInternalFolder=true;
        		}
        	} catch(Exception ex) {
        		useInternalFolder=true;
        	}
        }
        if( useInternalFolder) {
        	baseFolder = new File(m_Browser.getString(R.string.internal_download_path));
        	if( !baseFolder.exists()) {
        		baseFolder.mkdir();
        	}
        }
        final String fpath=baseFolder.toString();
        Runnable thrd = new Runnable() {
        	public void run() {
        		
    	m_Browser.displayAlert(m_Browser.getString(R.string.download), 
    			m_Browser.getString(R.string.downloadstart)+ " "  + 
        		fpath.toString(), 1, DownloadManager.this, R.drawable.wait);
        	}};
        m_Handler.postAtFrontOfQueue(thrd);
        URI uri = null;
        try {
            // Undo the percent-encoding that KURL may have done.
            String newUrl = new String(URLUtil.decode(url.getBytes()));
            // Parse the url into pieces
            WebAddress w = new WebAddress(newUrl);
            String frag = null;
            String query = null;
            String path = w.mPath;
            // Break the path into path, query, and fragment
            if (path.length() > 0) {
                // Strip the fragment
                int idx = path.lastIndexOf('#');
                if (idx != -1) {
                    frag = path.substring(idx + 1);
                    path = path.substring(0, idx);
                }
                idx = path.lastIndexOf('?');
                if (idx != -1) {
                    query = path.substring(idx + 1);
                    path = path.substring(0, idx);
                }
            }
            uri = new URI(w.mScheme, w.mAuthInfo, w.mHost, w.mPort, path,
                    query, frag);
        } catch (Exception e) {
            Log.e("DownloadManager", "Could not parse url for download: " + url, e);
            Runnable thrd1 = new Runnable() {
            	public void run() {
            		m_Browser.closeAlert();
            	}
            };
            m_Handler.post(thrd1);
            return;
        }

        String cookies = CookieManager.getInstance().getCookie(url);
        String fileName =URLUtil.guessFileName(url, contentDisposition, mimetype);
        if( fileName == null || fileName.equals("")) 
        	fileName="index.html";
        final File outputFile = new File(baseFolder.getAbsolutePath() + "/" + fileName);
        try {
        	InputStream in = OpenHttpConnection(uri.toURL(), cookies);
        	if( outputFile.exists()) {
        		//warn - alert
        	} else {
        		
        	}
        	FileOutputStream fout = new FileOutputStream(outputFile);
        	//FileOutputStream fout = m_Browser.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
        	byte[] buffer = new byte[1024];
        	int len=0;
        	int totallen=0;
        	while( (len=in.read(buffer)) != -1) {
        		if( m_StopDownload) {
        			break;
        		}
        		fout.write(buffer,0,len);
        		totallen += len;
        	}
        	fout.flush(); 
        	fout.close();
        	in.close();
    		if( m_StopDownload) {
    			outputFile.delete();
    		}
        	
        } catch(Exception ex) {
        	ex.printStackTrace();
        	Runnable thrd2 = new Runnable() {
        		public void run() {
            		m_Browser.closeAlert();
        			m_Browser.displayAlert(m_Browser.getString(R.string.download), 
        					m_Browser.getString(R.string.downloadfailed),3,null, R.drawable.fail);
        		}
        	};
        	m_Handler.post(thrd2);
            return;
        }
        Runnable thrd3 = new Runnable() {
        	public void run() {
        		m_Browser.closeAlert();
            	if( !m_StopDownload) 
            		m_Browser.displayAlert(m_Browser.getString(R.string.download), outputFile.getAbsolutePath() + " " + 
            						m_Browser.getString(R.string.downloadsuccess), 2,null, R.drawable.success);
        	}
        };
        m_Handler.post(thrd3);
    }
    private InputStream OpenHttpConnection(URL url, String cookies) 
    throws IOException
    {
        InputStream in = null;
        URLConnection conn = url.openConnection();
        if( cookies != null && !cookies.trim().equals(""))
        	conn.setRequestProperty("Cookie", cookies);
        conn.setAllowUserInteraction(false);
        conn.connect(); 
        in = conn.getInputStream();
        return in;     
    }

	public void onClick(DialogInterface dialog, int buttonPosition) {
		m_StopDownload=true;
	}
    
}



