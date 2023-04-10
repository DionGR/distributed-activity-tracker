public class Address {
    String ip;
    int port;

    public Address(String ip, int port){
        this.port = port;
        this.ip= ip;
    }

    public String getIP() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
