package chris.ftpapp.dialect;

import java.io.BufferedOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import chris.ftpapp.domain.Client;
import chris.ftpapp.dialect.BaseDialect;

/**
 * This is the dialect for all clients assume they all use same logic. 
 * If they do not lets try and create abstracted methods and think about re-usability
 *
 * @author dadmin / Chris W.
 */
public class BaseDialect implements Dialect
{

	protected static Log log = LogFactory.getLog(BaseDialect.class);
  
  protected String getFtpUrl(Client client, String filename) { 
    try{
      StringBuffer sb = new StringBuffer();
      sb.append("ftp://");
      if(client.getFtpUsername()!=null) {
        sb.append(URLEncoder.encode(client.getFtpUsername(),"UTF-8"));
        if(client.getFtpPassword()!=null) {
          sb.append(":" + URLEncoder.encode(client.getFtpPassword(),"UTF-8"));
        }
        sb.append("@");
      }
      sb.append(client.getFtpHost());
      //null check
      if(client.getFtpPort()!=null) {
        sb.append(":" + client.getFtpPort());
      }
      sb.append("/");
    //null check
      if(client.getExportFolder()!=null) {
        sb.append(client.getExportFolder());
        sb.append("/");
      }
      sb.append(filename);
  
      return sb.toString(); 
    } catch(UnsupportedEncodingException x) {
      return null;
    }
  }

  protected void ftpExportFile(Object[] expData, Client client) throws Exception {
    String urlValue = getFtpUrl(client, (String)expData[0]);
    if (log.isInfoEnabled()) {
      log.info("Writing to file:" + urlValue);
    }
    URL url = new URL(urlValue);
    URLConnection urlConn = url.openConnection();
    urlConn.setDoOutput(true);
    BufferedOutputStream bufOS = new BufferedOutputStream(urlConn.getOutputStream());
    bufOS.write((byte[]) expData[1]);
    bufOS.flush();
    bufOS.close();
  }
}
