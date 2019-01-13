
import java.io.Serializable;

public class Packet implements Serializable {
    private String person;
    private String msg;

    public Packet(String person, String msg) {
        this.person = person;
        this.msg = msg;
    }

    public String getPerson() {
        return this.person;
    }

    public String getMsg() {
        return this.msg;
    }

    @Override
    public String toString() {
        return "Packet [person=" + this.person + ", msg=" + this.msg + "]";
    }
}