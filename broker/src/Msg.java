import java.io.Serializable;

public class Msg implements Serializable {
    private String type;
    private String value;
    private String content;
	
	public Msg(String i, String j, String k) {
            this.type = i;
            this.value = j;
            this.content = k;
	}

    public String getType() {
        return this.type;
    }

    public String getValue() {
        return this.value;
    }

    public String getContent() {
        return this.content;
    }

    @Override
    public String toString() {
        return "Msg [type=" + this.type + ", value=" + this.value + ", content=" + this.content +  "]";
    }
}