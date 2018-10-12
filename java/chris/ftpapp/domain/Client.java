package chris.ftpapp.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.TableGenerator;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 
 * @author dadmin
 * This would map to mysql database and hosue the ftp credentials
 * 
 */

@Entity
@Table(name="client")
public class Client  implements java.io.Serializable {

    private static final long serialVersionUID = -6051244955221001275L;

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="client")
    @TableGenerator(name="client",
                    table="id_generation",
                    pkColumnName = "key_name",
                    valueColumnName = "next_val",
                    pkColumnValue="client",
                    initialValue = 300000000,
                    allocationSize=30)
    private Integer id;

    @Column(name="client_code")
    private String clientCode;

    @Column(name="client_name")
    private String clientName;

	@Column(name="ftp_host")
    private String ftpHost;

    @Column(name="ftp_port")
    private String ftpPort;

    @Column(name="ftp_username")
    private String ftpUsername;

    @Column(name="ftp_password")
    private String ftpPassword;
    
    @Column(name="export_folder")
    private String exportFolder;


    public String getExportFolder() {
		return exportFolder;
	}

	public void setExportFolder(String exportFolder) {
		this.exportFolder = exportFolder;
	}

	public String getFtpHost() {
      return ftpHost;
    }

    public void setFtpHost(String ftpHost) {
      this.ftpHost = ftpHost;
    }

    public String getFtpPort() {
      return ftpPort;
    }

    public void setFtpPort(String ftpPort) {
      this.ftpPort = ftpPort;
    }

    public String getFtpUsername() {
      return ftpUsername;
    }

    public void setFtpUsername(String ftpUsername) {
      this.ftpUsername = ftpUsername;
    }

    public String getFtpPassword() {
      return ftpPassword;
    }

    public void setFtpPassword(String ftpPassword) {
      this.ftpPassword = ftpPassword;
    }

    public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

    public Client() {
    }
}
