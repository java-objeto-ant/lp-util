import org.rmj.lib.net.SFTP_DU;


public class testSFTP {
    public static void main(String [] args){
        SFTP_DU sftp = new SFTP_DU();
        sftp.setHost("112.199.91.14");
        sftp.setHostKey("ssh-ed25519 255 e6vFs7ULtSiKo3GfwWlkuz792QGW2YeyWn/83Vsq38A=");
        sftp.setUser("LPPangasinan");
        sftp.setPassword("RLC@20211102Pag");
        sftp.setPort(22);
        
//        try {
//            if (!sftp.Download("/50080729/", "d:/", "07290503.011xx")){
//                System.err.println("Unable to download file.");
//            }
//            
//            System.out.println("File downloaded successfully.");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }
}
