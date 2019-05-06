package GrupoB.ApplicationServer.Models;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class NodeInfo implements Serializable {
    private Long id;
    private String address;
    private Integer port;

    public NodeInfo(Long id, String address, Integer port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "address='" + address + '\'' +
                ", port=" + port +
                '}';
    }
}
