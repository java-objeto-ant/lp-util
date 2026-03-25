import org.rmj.lib.net.SFTP_DU;

public class testSFTPUpload {
    public static void main(String [] args){
        SFTP_DU sftp = new SFTP_DU();
        sftp.setHost("rlccloud.robinsonsland.com");        
        sftp.setUser("LPPangasinan");
        sftp.setPassword("RLC@20211102Pag");
        sftp.setHostKey("ssh-ed25519 255 RuzY6SKnHx+uPYlz3iVdhVgDlTHO5aOJfKhUroepL3o");
        sftp.setPort(22);
        
        try {
            if (sftp.xConnect("rlccloud.robinsonsland.com")){
                System.out.println("Connected.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

//sftp.setHostKey("ssh-ed25519 255 e6vFs7ULtSiKo3GfwWlkuz792QGW2YeyWn/83Vsq38A=");
        