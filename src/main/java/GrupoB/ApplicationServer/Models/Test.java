package GrupoB.ApplicationServer.Models;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class Test implements Serializable {
    private String message;

    public Test() {
    }

    public Test(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Test{" +
                "message='" + message + '\'' +
                '}';
    }
}
