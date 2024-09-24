import org.rmj.lib.net.SFTP_DU;


public class testSFTPUpload {
    public static void main(String [] args){
        SFTP_DU sftp = new SFTP_DU();

        sftp.setHost("112.199.91.14");
        //sftp.setHostKey("ssh-ed25519 255 e6vFs7ULtSiKo3GfwWlkuz792QGW2YeyWn/83Vsq38A=");
        sftp.setUser("LPPangasinan");
        sftp.setPassword("RLC@20211102Pag");
        sftp.setPort(22);
        
        try {
            if (sftp.xConnect("112.199.91.14")){
                System.out.println("Connected.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
